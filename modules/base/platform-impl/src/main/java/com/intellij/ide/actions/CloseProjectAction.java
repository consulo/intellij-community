/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package com.intellij.ide.actions;

import com.intellij.ide.RecentProjectsManagerBase;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.project.ex.ProjectManagerEx;
import com.intellij.openapi.wm.impl.welcomeScreen.WelcomeFrame;
import consulo.annotations.RequiredDispatchThread;
import consulo.application.AccessRule;
import consulo.ui.UIAccess;

import javax.annotation.Nonnull;
import javax.inject.Inject;

public class CloseProjectAction extends AnAction implements DumbAware {
  private final ProjectManagerEx myProjectManager;

  @Inject
  public CloseProjectAction(ProjectManager projectManager) {
    myProjectManager = (ProjectManagerEx)projectManager;
  }

  @RequiredDispatchThread
  @Override
  public void actionPerformed(@Nonnull AnActionEvent e) {
    Project project = e.getRequiredData(CommonDataKeys.PROJECT);

    UIAccess uiAccess = UIAccess.current();
    AccessRule.writeAsync(() -> {
      myProjectManager.closeAndDisposeAsync(project, uiAccess).doWhenProcessed(() -> {
        RecentProjectsManagerBase.getInstance().updateLastProjectPath();
        WelcomeFrame.showIfNoProjectOpened();
      });
    });
  }

  @RequiredDispatchThread
  @Override
  public void update(@Nonnull AnActionEvent event) {
    Presentation presentation = event.getPresentation();
    Project project = event.getData(CommonDataKeys.PROJECT);
    presentation.setEnabled(project != null);
  }
}
