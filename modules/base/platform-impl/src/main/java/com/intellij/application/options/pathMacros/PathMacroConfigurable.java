/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package com.intellij.application.options.pathMacros;

import com.intellij.openapi.application.ApplicationBundle;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.project.ex.ProjectEx;
import consulo.components.impl.stores.ProjectStorageUtil;
import consulo.ui.annotation.RequiredUIAccess;
import org.jetbrains.annotations.NonNls;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;

/**
 * @author dsl
 */
public class PathMacroConfigurable implements SearchableConfigurable, Configurable.NoScroll {
  @NonNls
  public static final String HELP_ID = "preferences.pathVariables";
  private PathMacroListEditor myEditor;

  @Override
  public JComponent createComponent() {
    myEditor = new PathMacroListEditor();
    return myEditor.getPanel();
  }

  @RequiredUIAccess
  @Override
  public void apply() throws ConfigurationException {
    myEditor.commit();

    final Project[] projects = ProjectManager.getInstance().getOpenProjects();
    for (Project project : projects) {
      ProjectStorageUtil.checkUnknownMacros((ProjectEx)project, false);
    }
  }

  @Override
  public void reset() {
    myEditor.reset();
  }

  @Override
  public void disposeUIResources() {
    myEditor = null;
  }

  @Override
  public String getDisplayName() {
    return ApplicationBundle.message("title.path.variables");
  }

  @Override
  public String getHelpTopic() {
    return HELP_ID;
  }

  @Override
  public boolean isModified() {
    return myEditor != null && myEditor.isModified();
  }

  @Override
  @Nonnull
  public String getId() {
    return getHelpTopic();
  }

  @Override
  @Nullable
  public Runnable enableSearch(String option) {
    return null;
  }
}
