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
package com.intellij.openapi.externalSystem.test

import com.intellij.testFramework.SkipInHeadlessEnvironment
import com.intellij.testFramework.UsefulTestCase
import com.intellij.testFramework.fixtures.IdeaProjectTestFixture
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory

/**
 * @author Denis Zhdanov
 * @since 8/7/13 2:04 PM
 */
@SkipInHeadlessEnvironment
abstract class AbstractExternalSystemTest extends UsefulTestCase {

  static File tmpDir
  
  IdeaProjectTestFixture testFixture
  Project project
  File projectDir

  TestExternalSystemManager externalSystemManager
  ExtensionPoint externalSystemManagerEP
  
  @Override
  protected void setUp() throws Exception {
    super.setUp()
    
    ensureTempDirCreated()

    testFixture = IdeaTestFixtureFactory.fixtureFactory.createFixtureBuilder(name).fixture
    testFixture.setUp()
    project = testFixture.project

    projectDir = new File(tmpDir, getTestName(false));
    projectDir.mkdirs();
    
    externalSystemManager = new TestExternalSystemManager(project)
    externalSystemManagerEP = Application.get().getExtensionPoint(ExternalSystemManager.EP_NAME)
    //externalSystemManagerEP.registerExtension(externalSystemManager)
  }

  private static void ensureTempDirCreated() {
    if (tmpDir != null) {
      return
    }

    tmpDir = new File(FileUtil.tempDirectory, "externalSystemTests")
    FileUtil.delete(tmpDir)
    tmpDir.mkdirs()
  }

  @Override
  protected void tearDown() throws Exception {
    project = null
    UIUtil.invokeAndWaitIfNeeded {
      try {
        //externalSystemManagerEP.unregisterExtension(externalSystemManager)
        testFixture.tearDown();
        testFixture = null;
      }
      catch (Exception e) {
        throw new RuntimeException(e);
      }
    }

    if (!FileUtil.delete(projectDir) && projectDir.exists()) {
      System.err.println("Cannot delete " + projectDir);
      //printDirectoryContent(myDir);
      projectDir.deleteOnExit();
    }

    super.tearDown();
    resetClassFields(getClass());
  }

  private void resetClassFields(@Nullable Class<?> aClass) {
    if (aClass == null) {
      return
    }

    for (Field field : aClass.declaredFields) {
      final int modifiers = field.modifiers;
      if ((modifiers & Modifier.FINAL) == 0 && (modifiers & Modifier.STATIC) == 0 && !field.getType().isPrimitive()) {
        field.setAccessible(true);
        try {
          field.set(this, null);
        }
        catch (IllegalAccessException e) {
          e.printStackTrace();
        }
      }
    }

    if (aClass != AbstractExternalSystemTest.class) {
      resetClassFields(aClass.getSuperclass());
    }
  }

  public void setupExternalProject(@NotNull Closure c) {
    DataNode<ProjectData> node = buildExternalProjectInfo(c)
    applyProjectState([node])
  }
  
  @NotNull
  public <T> DataNode<T> buildExternalProjectInfo(@NotNull Closure c) {
    ExternalProjectBuilder builder = new ExternalProjectBuilder(projectDir: projectDir)
    c.delegate = builder
    c.call()
  }

  protected void applyProjectState(@NotNull List<DataNode<ProjectData>> states) {
    def dataManager = ServiceManager.getService(ProjectDataManager.class)
    def settingsInitialized = false
    for (DataNode<ProjectData> node : states) {
      if (!settingsInitialized) {
        settingsInitialized = true
        def settings = ExternalSystemApiUtil.getSettings(project, ExternalSystemTestUtil.TEST_EXTERNAL_SYSTEM_ID)
        settings.linkedProjectsSettings = [new TestExternalProjectSettings(externalProjectPath: node.data.linkedExternalProjectPath)]
      }

      final Project myProject = project
      ExternalSystemApiUtil.executeProjectChangeAction(true, new DisposeAwareProjectChange(myProject) {
        @RequiredDispatchThread
        @Override
        void execute() {
          ProjectRootManagerEx.getInstanceEx(myProject).mergeRootsChangesDuring {
            dataManager.importData(node.key, [node], myProject, true)
          }
        }})
    }
  }
}
