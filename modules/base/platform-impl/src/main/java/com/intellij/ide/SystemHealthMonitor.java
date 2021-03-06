/*
 * Copyright 2000-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.ide;

import com.intellij.concurrency.JobScheduler;
import com.intellij.diagnostic.VMOptions;
import com.intellij.jna.JnaLoader;
import com.intellij.notification.*;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ApplicationNamesInfo;
import com.intellij.openapi.application.PreloadingActivity;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.ThrowableComputable;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.HyperlinkAdapter;
import com.intellij.util.SystemProperties;
import com.intellij.util.TimeoutUtil;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import com.sun.jna.Library;
import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import consulo.application.AccessRule;
import consulo.container.boot.ContainerPathManager;
import consulo.ide.updateSettings.impl.PlatformOrPluginUpdateChecker;
import consulo.logging.Logger;
import consulo.util.ApplicationPropertiesComponent;
import jakarta.inject.Inject;
import org.jetbrains.annotations.PropertyKey;

import javax.annotation.Nonnull;
import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import java.io.File;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class SystemHealthMonitor extends PreloadingActivity {
  private static final Logger LOG = Logger.getInstance(SystemHealthMonitor.class);

  private static final NotificationGroup GROUP = new NotificationGroup("System Health", NotificationDisplayType.STICKY_BALLOON, false);

  private final ApplicationPropertiesComponent myProperties;

  private static String checkJavaLower11RuntimeIgnoreKey = "java.lower.11.runtime";

  @Inject
  public SystemHealthMonitor(@Nonnull ApplicationPropertiesComponent properties) {
    myProperties = properties;
  }

  @Override
  public void preload(@Nonnull ProgressIndicator indicator) {
    checkEARuntime();
    checkJavaLower11Runtime();
    checkReservedCodeCacheSize();
    checkSignalBlocking();
    checkHiDPIMode();
    startDiskSpaceMonitoring();
  }

  private void checkJavaLower11Runtime() {
    // if jre is bundled - skip, we will update it with platform
    if (PlatformOrPluginUpdateChecker.isJreBuild()) {
      return;
    }

    // already at 11
    if (SystemInfo.isJavaVersionAtLeast(11)) {
      return;
    }

    if (myProperties.getBoolean(checkJavaLower11RuntimeIgnoreKey)) {
      return;
    }

    Application app = Application.get();
    app.invokeLater(() -> {
      Notification notification = GROUP.createNotification("", IdeBundle.message("lower.jre.versions.11.message"), NotificationType.WARNING, new NotificationListener.Adapter() {
        @Override
        protected void hyperlinkActivated(@Nonnull Notification notification, @Nonnull HyperlinkEvent e) {
          notification.expire();

          if ("ignore".equals(e.getDescription())) {
            myProperties.setValue(checkJavaLower11RuntimeIgnoreKey, true);
          }
          else if ("jre".equals(e.getDescription())) {
            PlatformOrPluginUpdateChecker.setForceBundledJreAtUpdate();
          }
        }
      });
      notification.setImportant(true);
      Notifications.Bus.notify(notification);
    });
  }

  private void checkEARuntime() {
    if (StringUtil.endsWithIgnoreCase(System.getProperty("java.version", ""), "-ea")) {
      showNotification(new KeyHyperlinkAdapter("unsupported.jvm.ea.message"));
    }
  }

  private void checkReservedCodeCacheSize() {
    int minReservedCodeCacheSize = 240;
    int reservedCodeCacheSize = VMOptions.readOption(VMOptions.MemoryKind.CODE_CACHE, true);
    if (reservedCodeCacheSize > 0 && reservedCodeCacheSize < minReservedCodeCacheSize) {
      showNotification(new KeyHyperlinkAdapter("vmoptions.warn.message"), reservedCodeCacheSize, minReservedCodeCacheSize);
    }
  }

  private void checkSignalBlocking() {
    if (SystemInfo.isUnix && JnaLoader.isLoaded()) {
      try {
        LibC lib = Native.loadLibrary("c", LibC.class);
        Memory buf = new Memory(1024);
        if (lib.sigaction(LibC.SIGINT, null, buf) == 0) {
          long handler = Native.POINTER_SIZE == 8 ? buf.getLong(0) : buf.getInt(0);
          if (handler == LibC.SIG_IGN) {
            showNotification(new KeyHyperlinkAdapter("ide.sigint.ignored.message"));
          }
        }
      }
      catch (Throwable t) {
        LOG.warn(t);
      }
    }
  }

  private void checkHiDPIMode() {
    // if switched from JRE-HiDPI to IDE-HiDPI
    boolean switchedHiDPIMode = SystemInfo.isJetBrainsJvm && "true".equalsIgnoreCase(System.getProperty("sun.java2d.uiScale.enabled")) && !UIUtil.isJreHiDPIEnabled();
    if (SystemInfo.isWindows && ((switchedHiDPIMode && JBUI.isHiDPI(JBUI.sysScale())) || RemoteDesktopService.isRemoteSession())) {
      showNotification(new KeyHyperlinkAdapter("ide.set.hidpi.mode"));
    }
  }

  private void showNotification(KeyHyperlinkAdapter adapter, Object... params) {
    @PropertyKey(resourceBundle = "messages.IdeBundle") String key = adapter.key;
    boolean ignored = adapter.isIgnored();
    LOG.info("issue detected: " + key + (ignored ? " (ignored)" : ""));
    if (ignored) return;

    String message = IdeBundle.message(key, params) + IdeBundle.message("sys.health.acknowledge.link");

    Application app = Application.get();
    app.invokeLater(() -> {
      Notification notification = GROUP.createNotification("", message, NotificationType.WARNING, new NotificationListener.Adapter() {
        @Override
        protected void hyperlinkActivated(@Nonnull Notification notification, @Nonnull HyperlinkEvent e) {
          adapter.hyperlinkActivated(e);
        }
      });
      notification.setImportant(true);
      Notifications.Bus.notify(notification);
    });
  }

  private static void startDiskSpaceMonitoring() {
    if (SystemProperties.getBooleanProperty("idea.no.system.path.space.monitoring", false)) {
      return;
    }

    final File file = new File(ContainerPathManager.get().getSystemPath());
    final AtomicBoolean reported = new AtomicBoolean();
    final ThreadLocal<Future<Long>> ourFreeSpaceCalculation = new ThreadLocal<>();

    JobScheduler.getScheduler().schedule(new Runnable() {
      private static final long LOW_DISK_SPACE_THRESHOLD = 50 * 1024 * 1024;
      private static final long MAX_WRITE_SPEED_IN_BPS = 500 * 1024 * 1024;  // 500 MB/sec is near max SSD sequential write speed

      @Override
      public void run() {
        if (!reported.get()) {
          Future<Long> future = ourFreeSpaceCalculation.get();
          if (future == null) {
            ourFreeSpaceCalculation.set(future = ApplicationManager.getApplication().executeOnPooledThread(() -> {
              // file.getUsableSpace() can fail and return 0 e.g. after MacOSX restart or awakening from sleep
              // so several times try to recalculate usable space on receiving 0 to be sure
              long fileUsableSpace = file.getUsableSpace();
              while (fileUsableSpace == 0) {
                TimeoutUtil.sleep(5000);  // hopefully we will not hummer disk too much
                fileUsableSpace = file.getUsableSpace();
              }

              return fileUsableSpace;
            }));
          }
          if (!future.isDone() || future.isCancelled()) {
            restart(1);
            return;
          }

          try {
            final long fileUsableSpace = future.get();
            final long timeout = Math.min(3600, Math.max(5, (fileUsableSpace - LOW_DISK_SPACE_THRESHOLD) / MAX_WRITE_SPEED_IN_BPS));
            ourFreeSpaceCalculation.set(null);

            if (fileUsableSpace < LOW_DISK_SPACE_THRESHOLD) {
              ThrowableComputable<NotificationsConfiguration, RuntimeException> action = () -> NotificationsConfiguration.getNotificationsConfiguration();
              if (AccessRule.read(action) == null) {
                ourFreeSpaceCalculation.set(future);
                restart(1);
                return;
              }
              reported.compareAndSet(false, true);

              //noinspection SSBasedInspection
              SwingUtilities.invokeLater(() -> {
                String productName = ApplicationNamesInfo.getInstance().getFullProductName();
                String message = IdeBundle.message("low.disk.space.message", productName);
                if (fileUsableSpace < 100 * 1024) {
                  LOG.warn(message + " (" + fileUsableSpace + ")");
                  Messages.showErrorDialog(message, "Fatal Configuration Problem");
                  reported.compareAndSet(true, false);
                  restart(timeout);
                }
                else {
                  GROUP.createNotification(message, file.getPath(), NotificationType.ERROR, null).whenExpired(() -> {
                    reported.compareAndSet(true, false);
                    restart(timeout);
                  }).notify(null);
                }
              });
            }
            else {
              restart(timeout);
            }
          }
          catch (Exception ex) {
            LOG.error(ex);
          }
        }
      }

      private void restart(long timeout) {
        JobScheduler.getScheduler().schedule(this, timeout, TimeUnit.SECONDS);
      }
    }, 1, TimeUnit.SECONDS);
  }

  @SuppressWarnings({"SpellCheckingInspection", "SameParameterValue"})
  private interface LibC extends Library {
    int SIGINT = 2;
    long SIG_IGN = 1L;

    int sigaction(int signum, Pointer act, Pointer oldact);
  }

  private class KeyHyperlinkAdapter extends HyperlinkAdapter {
    private final String key;

    private KeyHyperlinkAdapter(@PropertyKey(resourceBundle = "messages.IdeBundle") String key) {
      this.key = key;
    }

    private boolean isIgnored() {
      return myProperties.isValueSet("ignore." + key);
    }

    @Override
    protected void hyperlinkActivated(HyperlinkEvent e) {
      String url = e.getDescription();
      if ("ack".equals(url)) {
        myProperties.setValue("ignore." + key, "true");
      }
      else {
        BrowserUtil.browse(url);
      }
    }
  }
}