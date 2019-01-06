/*
 * Copyright 2000-2013 JetBrains s.r.o.
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

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurableEP;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.options.ex.ConfigurableExtensionPointUtil;
import com.intellij.openapi.options.ex.ConfigurableWrapper;
import com.intellij.openapi.options.ex.SingleConfigurableEditor;
import com.intellij.openapi.options.newEditor.OptionsEditor;
import com.intellij.openapi.options.newEditor.OptionsEditorDialog;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.util.AsyncResult;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.ui.update.Activatable;
import com.intellij.util.ui.update.UiNotifyConnector;
import consulo.ui.RequiredUIAccess;
import consulo.ui.UIAccess;
import consulo.ui.impl.ModalityPerProjectEAPDescriptor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Singleton;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author max
 */
@Singleton
public class ShowSettingsUtilImpl extends ShowSettingsUtil {
  private static final Logger LOG = Logger.getInstance(ShowSettingsUtilImpl.class);
  private final AtomicBoolean myShown = new AtomicBoolean(false);

  @RequiredUIAccess
  @Override
  public void showSettingsDialog(@Nullable Project project) {
    UIAccess uiAccess = UIAccess.current();

    Task.Backgroundable.queue(project, "Open Settings", progressIndicator -> {
      Project actualProject = project != null ? project : ProjectManager.getInstance().getDefaultProject();

      Configurable[] configurables = buildConfigurables(project);

      uiAccess.give(() -> {
        try {
          myShown.set(true);

          _showSettingsDialog(actualProject, configurables, null).doWhenDone(() -> myShown.set(false));
        }
        catch (Exception e) {
          LOG.error(e);
        }
      });
    });
  }

  @RequiredUIAccess
  private static AsyncResult<Void> _showSettingsDialog(@Nonnull final Project project, @Nonnull Configurable[] configurables, @Nullable Configurable toSelect) {
    if (ModalityPerProjectEAPDescriptor.is()) {
      return new OptionsEditorDialog(project, configurables, toSelect, true).showAsync();
    }
    else {
      return new OptionsEditorDialog(project, configurables, toSelect).showAsync();
    }
  }

  @RequiredUIAccess
  @Override
  public void showSettingsDialog(@Nullable final Project project, final Class configurableClass) {
    assert Configurable.class.isAssignableFrom(configurableClass) : "Not a configurable: " + configurableClass.getName();

    Configurable[] configurables = buildConfigurables(project);

    Configurable config = findByClass(configurables, configurableClass);

    assert config != null : "Cannot find configurable: " + configurableClass.getName();

    @Nonnull Project nnProject = project != null ? project : ProjectManager.getInstance().getDefaultProject();
    _showSettingsDialog(nnProject, configurables, config);
  }

  @Nullable
  private static Configurable findByClass(Configurable[] configurables, Class configurableClass) {
    for (Configurable configurable : configurables) {
      if (configurableClass.isInstance(configurable)) {
        return configurable;
      }
    }
    return null;
  }

  @RequiredUIAccess
  @Override
  public void showSettingsDialog(@Nullable final Project project, @Nonnull final String nameToSelect) {
    Configurable[] configurables = buildConfigurables(project);

    Project actualProject = project != null ? project : ProjectManager.getInstance().getDefaultProject();

    OptionsEditorDialog dialog;
    if (ModalityPerProjectEAPDescriptor.is()) {
      dialog = new OptionsEditorDialog(actualProject, configurables, nameToSelect, true);
    }
    else {
      dialog = new OptionsEditorDialog(actualProject, configurables, nameToSelect);
    }
    dialog.show();
  }

  @RequiredUIAccess
  public static void showSettingsDialog(@Nullable Project project, final String id2Select, final String filter) {
    Configurable[] configurables = buildConfigurables(project);

    Project actualProject = project != null ? project : ProjectManager.getInstance().getDefaultProject();

    final Configurable configurable2Select = findConfigurable2Select(id2Select, configurables);

    final OptionsEditorDialog dialog;
    if (ModalityPerProjectEAPDescriptor.is()) {
      dialog = new OptionsEditorDialog(actualProject, configurables, configurable2Select, true);
    }
    else {
      dialog = new OptionsEditorDialog(actualProject, configurables, configurable2Select);
    }

    new UiNotifyConnector.Once(dialog.getContentPane(), new Activatable() {
      @Override
      public void showNotify() {
        final OptionsEditor editor = dialog.getDataUnchecked(OptionsEditor.KEY);
        LOG.assertTrue(editor != null);
        editor.select(configurable2Select, filter);
      }
    });
    dialog.showAsync();
  }

  @Nonnull
  public static Configurable[] buildConfigurables(@Nullable Project project) {
    if (project == null) {
      project = ProjectManager.getInstance().getDefaultProject();
    }

    final Project tempProject = project;

    List<ConfigurableEP<Configurable>> configurableEPs = new ArrayList<>();
    Collections.addAll(configurableEPs, Configurable.APPLICATION_CONFIGURABLE.getExtensions());
    Collections.addAll(configurableEPs, Configurable.PROJECT_CONFIGURABLE.getExtensions(project));

    List<Configurable> result =
            ConfigurableExtensionPointUtil.buildConfigurablesList(configurableEPs, configurable -> !tempProject.isDefault() || !ConfigurableWrapper.isNonDefaultProject(configurable));

    return ContainerUtil.toArray(result, Configurable.ARRAY_FACTORY);
  }

  @Nullable
  private static Configurable findConfigurable2Select(String id2Select, Configurable[] configurables) {
    for (Configurable configurable : configurables) {
      final Configurable conf = containsId(id2Select, configurable);
      if (conf != null) return conf;
    }
    return null;
  }

  @Nullable
  private static Configurable containsId(String id2Select, Configurable configurable) {
    if (configurable instanceof SearchableConfigurable && id2Select.equals(((SearchableConfigurable)configurable).getId())) {
      return configurable;
    }
    if (configurable instanceof SearchableConfigurable.Parent) {
      for (Configurable subConfigurable : ((SearchableConfigurable.Parent)configurable).getConfigurables()) {
        final Configurable config = containsId(id2Select, subConfigurable);
        if (config != null) return config;
      }
    }
    return null;
  }

  @RequiredUIAccess
  @Override
  public void showSettingsDialog(@Nonnull final Project project, final Configurable toSelect) {
    _showSettingsDialog(project, buildConfigurables(project), toSelect);
  }

  @RequiredUIAccess
  @Override
  public AsyncResult<Void> editConfigurable(@Nullable String title, Project project, Configurable configurable) {
    return editConfigurable(title, project, createDimensionKey(configurable), configurable);
  }

  @Override
  public <T extends Configurable> T findApplicationConfigurable(final Class<T> confClass) {
    return ConfigurableExtensionPointUtil.findApplicationConfigurable(confClass);
  }

  @Override
  public <T extends Configurable> T findProjectConfigurable(final Project project, final Class<T> confClass) {
    return ConfigurableExtensionPointUtil.findProjectConfigurable(project, confClass);
  }

  @RequiredUIAccess
  @Override
  public AsyncResult<Void> editConfigurable(@Nullable String title, Project project, String dimensionServiceKey, @Nonnull Configurable configurable) {
    return editConfigurable(null, project, configurable, title, dimensionServiceKey, null);
  }

  @Override
  @RequiredUIAccess
  public AsyncResult<Void> editConfigurable(String title, Project project, Configurable configurable, Runnable advancedInitialization) {
    return editConfigurable(null, project, configurable, title, createDimensionKey(configurable), advancedInitialization);
  }

  @RequiredUIAccess
  @Override
  public AsyncResult<Void> editConfigurable(Component parent, Configurable configurable) {
    return editConfigurable(parent, configurable, null);
  }

  @RequiredUIAccess
  @Override
  public AsyncResult<Void> editConfigurable(final Component parent, final Configurable configurable, @Nullable final Runnable advancedInitialization) {
    return editConfigurable(parent, null, configurable, null, createDimensionKey(configurable), advancedInitialization);
  }

  @RequiredUIAccess
  private static AsyncResult<Void> editConfigurable(@Nullable Component parent,
                                                    @Nullable Project project,
                                                    Configurable configurable,
                                                    String title,
                                                    String dimensionKey,
                                                    @Nullable final Runnable advancedInitialization) {
    SingleConfigurableEditor editor;
    if (parent != null) {
      editor = new SingleConfigurableEditor(parent, configurable, title, dimensionKey, true, DialogWrapper.IdeModalityType.IDE);
    }
    else {
      editor = new SingleConfigurableEditor(project, configurable, title, dimensionKey, true, DialogWrapper.IdeModalityType.IDE);
    }
    if (advancedInitialization != null) {
      new UiNotifyConnector.Once(editor.getContentPane(), new Activatable() {
        @Override
        public void showNotify() {
          advancedInitialization.run();
        }
      });
    }
    return editor.showAsync();
  }

  public static String createDimensionKey(@Nonnull Configurable configurable) {
    String displayName = configurable.getDisplayName();
    if (displayName == null) {
      displayName = configurable.getClass().getName();
    }
    return '#' + StringUtil.replaceChar(StringUtil.replaceChar(displayName, '\n', '_'), ' ', '_');
  }

  @RequiredUIAccess
  @Override
  public AsyncResult<Void> editConfigurable(Component parent, String dimensionServiceKey, Configurable configurable) {
    return editConfigurable(parent, null, configurable, null, dimensionServiceKey, null);
  }

  public boolean isAlreadyShown() {
    return myShown.get();
  }
}
