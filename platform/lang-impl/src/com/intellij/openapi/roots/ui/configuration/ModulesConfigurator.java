/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package com.intellij.openapi.roots.ui.configuration;

import com.intellij.compiler.ModuleCompilerUtil;
import com.intellij.ide.actions.ImportModuleAction;
import com.intellij.ide.util.newProjectWizard.AddModuleWizard;
import com.intellij.ide.util.projectWizard.ModuleBuilder;
import com.intellij.ide.util.projectWizard.ProjectBuilder;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.ModifiableModuleModel;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectBundle;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.*;
import com.intellij.openapi.roots.impl.ModifiableModelCommitter;
import com.intellij.openapi.roots.ui.configuration.actions.ModuleDeleteProvider;
import com.intellij.openapi.roots.ui.configuration.projectRoot.ProjectSdksModel;
import com.intellij.openapi.roots.ui.configuration.projectRoot.StructureConfigurableContext;
import com.intellij.openapi.roots.ui.configuration.projectRoot.daemon.ModuleProjectStructureElement;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.packaging.artifacts.ModifiableArtifactModel;
import com.intellij.projectImport.ProjectImportBuilder;
import com.intellij.util.ArrayUtil;
import com.intellij.util.containers.HashMap;
import com.intellij.util.graph.GraphGenerator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * @author Eugene Zhuravlev
 *         Date: Dec 15, 2003
 */
public class ModulesConfigurator implements ModulesProvider, ModuleEditor.ChangeListener {
  private static final Logger LOG = Logger.getInstance("#" + ModulesConfigurator.class.getName());

  private final Project myProject;
  private final List<ModuleEditor> myModuleEditors = new ArrayList<ModuleEditor>();
  private final Comparator<ModuleEditor> myModuleEditorComparator = new Comparator<ModuleEditor>() {
    @Override
    public int compare(ModuleEditor editor1, ModuleEditor editor2) {
      return ModulesAlphaComparator.INSTANCE.compare(editor1.getModule(), editor2.getModule());
    }

    @SuppressWarnings({"EqualsWhichDoesntCheckParameterClass"})
    public boolean equals(Object o) {
      return false;
    }
  };
  private boolean myModified = false;
  private ModifiableModuleModel myModuleModel;
  private boolean myModuleModelCommitted = false;


  private StructureConfigurableContext myContext;
  private final List<ModuleEditor.ChangeListener> myAllModulesChangeListeners = new ArrayList<ModuleEditor.ChangeListener>();

  public ModulesConfigurator(Project project) {
    myProject = project;
    myModuleModel = ModuleManager.getInstance(myProject).getModifiableModel();
  }

  public void setContext(final StructureConfigurableContext context) {
    myContext = context;
  }

  public void disposeUIResources() {
    ApplicationManager.getApplication().runWriteAction(new Runnable() {
      @Override
      public void run() {
        for (final ModuleEditor moduleEditor : myModuleEditors) {
          Disposer.dispose(moduleEditor);
        }
        myModuleEditors.clear();

        myModuleModel.dispose();
      }
    });

  }

  @Override
  @NotNull
  public Module[] getModules() {
    return myModuleModel.getModules();
  }

  @Override
  @Nullable
  public Module getModule(String name) {
    final Module moduleByName = myModuleModel.findModuleByName(name);
    if (moduleByName != null) {
      return moduleByName;
    }
    return myModuleModel.getModuleToBeRenamed(name); //if module was renamed
  }

  @Nullable
  public ModuleEditor getModuleEditor(Module module) {
    for (final ModuleEditor moduleEditor : myModuleEditors) {
      if (module.equals(moduleEditor.getModule())) {
        return moduleEditor;
      }
    }
    return null;
  }

  @Override
  public ModuleRootModel getRootModel(@NotNull Module module) {
    return getOrCreateModuleEditor(module).getRootModel();
  }

  public ModuleEditor getOrCreateModuleEditor(Module module) {
    LOG.assertTrue(getModule(module.getName()) != null, "Module has been deleted");
    ModuleEditor editor = getModuleEditor(module);
    if (editor == null) {
      editor = doCreateModuleEditor(module);
    }
    return editor;
  }

  private ModuleEditor doCreateModuleEditor(final Module module) {
    final ModuleEditor moduleEditor = new HeaderHidingTabbedModuleEditor(myProject, this, module);

    myModuleEditors.add(moduleEditor);

    moduleEditor.addChangeListener(this);
    Disposer.register(moduleEditor, new Disposable() {
      @Override
      public void dispose() {
        moduleEditor.removeChangeListener(ModulesConfigurator.this);
      }
    });
    return moduleEditor;
  }


  public void resetModuleEditors() {
    myModuleModel = ModuleManager.getInstance(myProject).getModifiableModel();

    ApplicationManager.getApplication().runWriteAction(new Runnable() {
      @Override
      public void run() {
        if (!myModuleEditors.isEmpty()) {
          LOG.error("module editors was not disposed");
          myModuleEditors.clear();
        }
        final Module[] modules = myModuleModel.getModules();
        if (modules.length > 0) {
          for (Module module : modules) {
            getOrCreateModuleEditor(module);
          }
          Collections.sort(myModuleEditors, myModuleEditorComparator);
        }
      }
    });
    myModified = false;
  }

  @Override
  public void moduleStateChanged(final ModifiableRootModel moduleRootModel) {
    for (ModuleEditor.ChangeListener listener : myAllModulesChangeListeners) {
      listener.moduleStateChanged(moduleRootModel);
    }
    myContext.getDaemonAnalyzer().queueUpdate(new ModuleProjectStructureElement(myContext, moduleRootModel.getModule()));
  }

  public void addAllModuleChangeListener(ModuleEditor.ChangeListener listener) {
    myAllModulesChangeListeners.add(listener);
  }

  public GraphGenerator<ModuleRootModel> createGraphGenerator() {
    final Map<Module, ModuleRootModel> models = new HashMap<Module, ModuleRootModel>();
    for (ModuleEditor moduleEditor : myModuleEditors) {
      models.put(moduleEditor.getModule(), moduleEditor.getRootModel());
    }
    return ModuleCompilerUtil.createGraphGenerator(models);
  }

  public void apply() throws ConfigurationException {
    // validate content and source roots 
    final Map<VirtualFile, String> contentRootToModuleNameMap = new HashMap<VirtualFile, String>();
    final Map<VirtualFile, VirtualFile> srcRootsToContentRootMap = new HashMap<VirtualFile, VirtualFile>();
    for (final ModuleEditor moduleEditor : myModuleEditors) {
      final ModifiableRootModel rootModel = moduleEditor.getModifiableRootModel();
      final ContentEntry[] contents = rootModel.getContentEntries();
      for (ContentEntry contentEntry : contents) {
        final VirtualFile contentRoot = contentEntry.getFile();
        if (contentRoot == null) {
          continue;
        }
        final String moduleName = moduleEditor.getName();
        final String previousName = contentRootToModuleNameMap.put(contentRoot, moduleName);
        if (previousName != null && !previousName.equals(moduleName)) {
          throw new ConfigurationException(
            ProjectBundle.message("module.paths.validation.duplicate.content.error", contentRoot.getPresentableUrl(), previousName, moduleName)
          );
        }

        final VirtualFile[] sourceAndTestFiles = ArrayUtil.mergeArrays(contentEntry.getFolderFiles(ContentFolderType.SOURCE),
                                                                 contentEntry.getFolderFiles(ContentFolderType.TEST));
        for (VirtualFile srcRoot : sourceAndTestFiles) {
          final VirtualFile anotherContentRoot = srcRootsToContentRootMap.put(srcRoot, contentRoot);
          if (anotherContentRoot != null) {
            final String problematicModule;
            final String correctModule;
            if (VfsUtilCore.isAncestor(anotherContentRoot, contentRoot, true)) {
              problematicModule = contentRootToModuleNameMap.get(anotherContentRoot);
              correctModule = contentRootToModuleNameMap.get(contentRoot);
            }
            else {
              problematicModule = contentRootToModuleNameMap.get(contentRoot);
              correctModule = contentRootToModuleNameMap.get(anotherContentRoot);
            }
            throw new ConfigurationException(
              ProjectBundle.message("module.paths.validation.duplicate.source.root.error", problematicModule, srcRoot.getPresentableUrl(), correctModule)
            );
          }
        }
      }
    }
    // additional validation: directories marked as src roots must belong to the same module as their corresponding content root
    for (Map.Entry<VirtualFile, VirtualFile> entry : srcRootsToContentRootMap.entrySet()) {
      final VirtualFile srcRoot = entry.getKey();
      final VirtualFile correspondingContent = entry.getValue();
      final String expectedModuleName = contentRootToModuleNameMap.get(correspondingContent);

      for (VirtualFile candidateContent = srcRoot; candidateContent != null && !candidateContent.equals(correspondingContent); candidateContent = candidateContent.getParent()) {
        final String moduleName = contentRootToModuleNameMap.get(candidateContent);
        if (moduleName != null && !moduleName.equals(expectedModuleName)) {
          throw new ConfigurationException(
            ProjectBundle.message("module.paths.validation.source.root.belongs.to.another.module.error", srcRoot.getPresentableUrl(), expectedModuleName, moduleName)
          );
        }
      }
    }

    final List<ModifiableRootModel> models = new ArrayList<ModifiableRootModel>(myModuleEditors.size());
    for (ModuleEditor moduleEditor : myModuleEditors) {
      moduleEditor.canApply();
    }
    
    final Map<Sdk, Sdk> modifiedToOriginalMap = new HashMap<Sdk, Sdk>();
    final ProjectSdksModel projectJdksModel = ProjectStructureConfigurable.getInstance(myProject).getProjectJdksModel();
    for (Map.Entry<Sdk, Sdk> entry : projectJdksModel.getProjectSdks().entrySet()) {
      modifiedToOriginalMap.put(entry.getValue(), entry.getKey());
    }
    
    for (final ModuleEditor moduleEditor : myModuleEditors) {
      final ModifiableRootModel model = moduleEditor.apply();
      if (model != null) {
        /*if (!model.isSdkInherited()) {
          // make sure the sdk is set to original SDK stored in the JDK Table
          final Sdk modelSdk = model.getSdk();
          if (modelSdk != null) {
            final Sdk original = modifiedToOriginalMap.get(modelSdk);
            if (original != null) {
              model.setSdk(original);
            }
          }
        } */
        models.add(model);
      }
    }


    ApplicationManager.getApplication().runWriteAction(new Runnable() {
      @Override
      public void run() {
        try {
          final ModifiableRootModel[] rootModels = models.toArray(new ModifiableRootModel[models.size()]);
          ModifiableModelCommitter.multiCommit(rootModels, myModuleModel);
          myModuleModelCommitted = true;
        }
        finally {

          myModuleModel = ModuleManager.getInstance(myProject).getModifiableModel();
          myModuleModelCommitted = false;
        }
      }
    });

    myModified = false;
  }

  public void setModified(final boolean modified) {
    myModified = modified;
  }

  public ModifiableModuleModel getModuleModel() {
    return myModuleModel;
  }

  public boolean isModuleModelCommitted() {
    return myModuleModelCommitted;
  }

  public boolean deleteModule(final Module module) {
    ModuleEditor moduleEditor = getModuleEditor(module);
    if (moduleEditor == null) return true;
    return doRemoveModule(moduleEditor);
  }


  @Nullable
  public List<Module> addModule(Component parent, boolean anImport) {
    if (myProject.isDefault()) return null;
    final ProjectBuilder builder = runModuleWizard(parent, anImport);
    if (builder != null ) {
      final List<Module> modules = new ArrayList<Module>();
      final List<Module> commitedModules;
      if (builder instanceof ProjectImportBuilder<?>) {
        final ModifiableArtifactModel artifactModel =
            ProjectStructureConfigurable.getInstance(myProject).getArtifactsStructureConfigurable().getModifiableArtifactModel();
        commitedModules = ((ProjectImportBuilder<?>)builder).commit(myProject, myModuleModel, this, artifactModel);
      }
      else {
        commitedModules = builder.commit(myProject, myModuleModel, this);
      }
      if (commitedModules != null) {
        modules.addAll(commitedModules);
      }
      ApplicationManager.getApplication().runWriteAction(new Runnable() {
         @Override
         public void run() {
           for (Module module : modules) {
             getOrCreateModuleEditor(module);
           }
         }
       });
      return modules;
    }
    return null;
  }

  private Module createModule(final ModuleBuilder builder) {
    final Exception[] ex = new Exception[]{null};
    final Module module = ApplicationManager.getApplication().runWriteAction(new Computable<Module>() {
      @Override
      @SuppressWarnings({"ConstantConditions"})
      public Module compute() {
        try {
          return builder.createModule(myModuleModel);
        }
        catch (Exception e) {
          ex[0] = e;
          return null;
        }
      }
    });
    if (ex[0] != null) {
      Messages.showErrorDialog(ProjectBundle.message("module.add.error.message", ex[0].getMessage()),
                               ProjectBundle.message("module.add.error.title"));
    }
    return module;
  }

  @Nullable
  public Module addModule(final ModuleBuilder moduleBuilder) {
    final Module module = createModule(moduleBuilder);
    if (module != null) {
      ApplicationManager.getApplication().runWriteAction(new Runnable() {
        @Override
        public void run() {
          getOrCreateModuleEditor(module);
          Collections.sort(myModuleEditors, myModuleEditorComparator);
        }
      });
      processModuleCountChanged();
    }
    return module;
  }

  @Nullable
  ProjectBuilder runModuleWizard(Component dialogParent, boolean anImport) {
    AddModuleWizard wizard;
    if (anImport) {
      wizard = ImportModuleAction.selectFileAndCreateWizard(myProject, dialogParent);
      if (wizard == null) return null;
      if (wizard.getStepCount() == 0) return wizard.getProjectBuilder();
    }
    else {
      wizard = new AddModuleWizard(dialogParent, myProject, this);
    }
    wizard.show();
    if (wizard.isOK()) {
      final ProjectBuilder builder = wizard.getProjectBuilder();
      if (builder instanceof ModuleBuilder) {
        final ModuleBuilder moduleBuilder = (ModuleBuilder)builder;
        if (moduleBuilder.getName() == null) {
          moduleBuilder.setName(wizard.getProjectName());
        }
        if (moduleBuilder.getModuleFilePath() == null) {
          moduleBuilder.setModuleFilePath(wizard.getModuleFilePath());
        }
      }
      if (!builder.validate(myProject, myProject)) {
        return null;
      }
      return wizard.getProjectBuilder();
    }

    return null;
  }


  private boolean doRemoveModule(@NotNull ModuleEditor selectedEditor) {

    String question;
    if (myModuleEditors.size() == 1) {
      question = ProjectBundle.message("module.remove.last.confirmation");
    }
    else {
      question = ProjectBundle.message("module.remove.confirmation", selectedEditor.getModule().getName());
    }
    int result =
      Messages.showYesNoDialog(myProject, question, ProjectBundle.message("module.remove.confirmation.title"), Messages.getQuestionIcon());
    if (result != 0) {
      return false;
    }
    // do remove
    myModuleEditors.remove(selectedEditor);

    // destroyProcess removed module
    final Module moduleToRemove = selectedEditor.getModule();
    // remove all dependencies on the module that is about to be removed
    List<ModifiableRootModel> modifiableRootModels = new ArrayList<ModifiableRootModel>();
    for (final ModuleEditor moduleEditor : myModuleEditors) {
      final ModifiableRootModel modifiableRootModel = moduleEditor.getModifiableRootModelProxy();
      modifiableRootModels.add(modifiableRootModel);
    }

    // destroyProcess editor
    ModuleDeleteProvider.removeModule(moduleToRemove, null, modifiableRootModels, myModuleModel);
    processModuleCountChanged();
    Disposer.dispose(selectedEditor);
    
    return true;
  }


  private void processModuleCountChanged() {
    for (ModuleEditor moduleEditor : myModuleEditors) {
      moduleEditor.moduleCountChanged();
    }
  }

  public void processModuleCompilerOutputChanged(String baseUrl) {
    for (ModuleEditor moduleEditor : myModuleEditors) {
      moduleEditor.updateCompilerOutputPathChanged(baseUrl, moduleEditor.getName());
    }
  }

  public boolean isModified() {
    if (myModuleModel.isChanged()) {
      return true;
    }
    for (ModuleEditor moduleEditor : myModuleEditors) {
      if (moduleEditor.isModified()) {
        return true;
      }
    }
    return myModified;
  }

  public static boolean showArtifactSettings(@NotNull Project project, @Nullable final Artifact artifact) {
    final ProjectStructureConfigurable configurable = ProjectStructureConfigurable.getInstance(project);
    return ShowSettingsUtil.getInstance().editConfigurable(project, configurable, new Runnable() {
      @Override
      public void run() {
        configurable.select(artifact, true);
      }
    });
  }

  public static boolean showDialog(Project project, @Nullable final String moduleToSelect, @Nullable final String editorNameToSelect) {
    final ProjectStructureConfigurable config = ProjectStructureConfigurable.getInstance(project);
    return ShowSettingsUtil.getInstance().editConfigurable(project, config, new Runnable() {
      @Override
      public void run() {
        config.select(moduleToSelect, editorNameToSelect, true);
      }
    });
  }

  public void moduleRenamed(Module module, final String oldName, final String name) {
    for (ModuleEditor moduleEditor : myModuleEditors) {
      if (module == moduleEditor.getModule() && Comparing.strEqual(moduleEditor.getName(), oldName)) {
        moduleEditor.setModuleName(name);
        moduleEditor.updateCompilerOutputPathChanged(ProjectStructureConfigurable.getInstance(myProject).getProjectConfig().getCompilerOutputUrl(), name);
        myContext.getDaemonAnalyzer().queueUpdate(new ModuleProjectStructureElement(myContext, module));
        return;
      }
    }
  }

  public StructureConfigurableContext getContext() {
    return myContext;
  }
}