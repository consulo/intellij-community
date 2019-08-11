/*
 * Copyright 2013-2017 consulo.io
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
package consulo.platform;

import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.util.text.StringUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

/**
 * @author VISTALL
 * @since 15-Sep-17
 *
 * static fields from SystemInfo from IDEA
 */
public abstract class PlatformBase implements Platform {
  private static final String OS_NAME = System.getProperty("os.name");
  private static final String OS_VERSION = System.getProperty("os.version").toLowerCase();

  private static final String _OS_NAME = OS_NAME.toLowerCase();
  private static final boolean isWindows = _OS_NAME.startsWith("windows");
  private static final boolean isOS2 = _OS_NAME.startsWith("os/2") || _OS_NAME.startsWith("os2");
  private static final boolean isMac = _OS_NAME.startsWith("mac");
  private static final boolean isLinux = _OS_NAME.startsWith("linux");
  private static final boolean isUnix = !isWindows && !isOS2;

  private static final boolean isFileSystemCaseSensitive = isUnix && !isMac || "true".equalsIgnoreCase(System.getProperty("idea.case.sensitive.fs"));
  private static final boolean isWinVistaOrNewer = isWindows && isOsVersionAtLeast("6.0");
  private static final boolean areSymLinksSupported = isUnix || isWinVistaOrNewer;

  public static boolean isOsVersionAtLeast(@Nonnull String version) {
    return StringUtil.compareVersionNumbers(OS_VERSION, version) >= 0;
  }

  protected static class FileSystemImpl implements FileSystem {

    @Override
    public boolean isCaseSensitive() {
      return isFileSystemCaseSensitive;
    }

    @Override
    public boolean areSymLinksSupported() {
      return areSymLinksSupported;
    }
  }

  protected static class OperatingSystemImpl implements OperatingSystem {
    @Override
    public boolean isWindows() {
      return isWindows;
    }

    @Override
    public boolean isMac() {
      return isMac;
    }

    @Nonnull
    @Override
    public String name() {
      return OS_NAME;
    }

    @Nonnull
    @Override
    public String version() {
      return OS_VERSION;
    }

    @Nonnull
    @Override
    public String arch() {
      return StringUtil.notNullize(System.getProperty("os.arch"));
    }
  }

  protected static class JvmImpl implements Jvm {

    @Nonnull
    @Override
    public String version() {
      return System.getProperty("java.version");
    }

    @Nonnull
    @Override
    public String runtimeVersion() {
      return StringUtil.notNullize(System.getProperty("java.runtime.version"), "n/a");
    }

    @Nonnull
    @Override
    public String vendor() {
      return StringUtil.notNullize(System.getProperty("java.vendor"), "n/a");
    }
  }

  private final PluginId myPluginId;

  private final FileSystem myFileSystem;
  private final OperatingSystem myOperatingSystem;
  private final Jvm myJvm;

  protected PlatformBase(@Nonnull String pluginId) {
    myPluginId = PluginId.getId(pluginId);

    myFileSystem = createFS();
    myOperatingSystem = createOS();
    myJvm = createJVM();
  }

  @Nonnull
  protected FileSystem createFS() {
    return new FileSystemImpl();
  }

  @Nonnull
  protected OperatingSystem createOS() {
    return new OperatingSystemImpl();
  }

  @Nonnull
  protected Jvm createJVM() {
    return new JvmImpl();
  }

  @Nonnull
  @Override
  public Jvm jvm() {
    return myJvm;
  }

  @Nonnull
  @Override
  public FileSystem fs() {
    return myFileSystem;
  }

  @Nonnull
  @Override
  public OperatingSystem os() {
    return myOperatingSystem;
  }

  @Nullable
  @Override
  public String getRuntimeProperty(@Nonnull String key) {
    return System.getProperty(key);
  }

  @Nullable
  @Override
  public String getEnvironmentVariable(@Nonnull String key) {
    return System.getenv(key);
  }

  @Nonnull
  @Override
  public Map<String, String> getEnvironmentVariables() {
    return Collections.unmodifiableMap(System.getenv());
  }

  @Nonnull
  @Override
  public Map<String, String> getRuntimeProperties() {
    Properties properties = System.getProperties();
    Map<String, String> map = new LinkedHashMap<>();
    for (Map.Entry<Object, Object> entry : properties.entrySet()) {
      map.put(String.valueOf(entry.getKey()), String.valueOf(entry.getValue()));
    }
    return map;
  }

  @Nonnull
  @Override
  public PluginId getPluginId() {
    return myPluginId;
  }

  @Override
  public boolean isUnderRoot() {
    return false;
  }

  @Override
  public boolean isDesktop() {
    return false;
  }

  @Override
  public boolean isWebService() {
    return false;
  }
}
