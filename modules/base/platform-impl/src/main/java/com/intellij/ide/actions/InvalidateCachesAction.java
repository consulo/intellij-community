// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.actions;

import com.intellij.ide.caches.CachesInvalidator;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ex.ApplicationEx;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.newvfs.persistent.FSRecords;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.gist.GistManager;
import com.intellij.util.indexing.FileBasedIndex;
import javax.annotation.Nonnull;

import java.util.Collections;
import java.util.List;

public class InvalidateCachesAction extends AnAction implements DumbAware {
  public InvalidateCachesAction() {
    getTemplatePresentation().setText(ApplicationManager.getApplication().isRestartCapable() ? "Invalidate Caches / Restart..." : "Invalidate Caches...");
  }

  @Override
  public void actionPerformed(@Nonnull AnActionEvent e) {
    final ApplicationEx app = (ApplicationEx)ApplicationManager.getApplication();
    final boolean mac = Messages.canShowMacSheetPanel();
    boolean canRestart = app.isRestartCapable();

    String[] options = new String[canRestart ? 4 : 3];
    options[0] = canRestart ? "Invalidate and &Restart" : "Invalidate and &Exit";
    options[1] = mac ? "Cancel" : "&Invalidate";
    options[2] = mac ? "&Invalidate" : "Cancel";
    if (canRestart) {
      options[3] = "&Just Restart";
    }

    List<String> descriptions = new SmartList<>();
    boolean invalidateCachesInvalidatesVfs = Registry.is("idea.invalidate.caches.invalidates.vfs");

    if (invalidateCachesInvalidatesVfs) descriptions.add("Local History");

    for (CachesInvalidator invalidater : CachesInvalidator.EP_NAME.getExtensionList()) {
      ContainerUtil.addIfNotNull(descriptions, invalidater.getDescription());
    }
    Collections.sort(descriptions);

    String warnings = "WARNING: ";
    if (descriptions.size() == 1) {
      warnings += descriptions.get(0) + " will be also cleared.";
    }
    else if (!descriptions.isEmpty()) {
      warnings += "The following items will also be cleared:\n" + StringUtil.join(descriptions, s -> "  " + s, "\n");
    }

    String message = "The caches will be invalidated and rebuilt on the next startup.\n\n" + (descriptions.isEmpty() ? "" : warnings + "\n\n") + "Would you like to continue?\n";
    int result = Messages.showDialog(e.getData(CommonDataKeys.PROJECT), message, "Invalidate Caches", options, 0, Messages.getWarningIcon());

    if (result == -1 || result == (mac ? 1 : 2)) {
      return;
    }

    if (result == 3) {
      app.restart(true);
      return;
    }

    if (invalidateCachesInvalidatesVfs) {
      FSRecords.invalidateCaches();
    }
    else {
      FileBasedIndex.getInstance().invalidateCaches();
      GistManager.getInstance().invalidateData();
    }

    for (CachesInvalidator invalidater : CachesInvalidator.EP_NAME.getExtensions()) {
      invalidater.invalidateCaches();
    }

    if (result == 0) app.restart(true);
  }
}
