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
package com.intellij.testFramework;

import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.projectRoots.JavaSdk;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.projectRoots.SdkModificator;
import com.intellij.openapi.roots.LanguageLevelModuleExtension;
import com.intellij.openapi.roots.LanguageLevelProjectExtension;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.util.ArchiveVfsUtil;
import com.intellij.pom.java.LanguageLevel;
import org.jetbrains.annotations.NotNull;

import java.io.File;

public class IdeaTestUtil extends PlatformTestUtil {
  public static void main(String[] args) {
    printDetectedPerformanceTimings();
  }

  @SuppressWarnings({"UseOfSystemOutOrSystemErr"})
  public static void printDetectedPerformanceTimings() {
    System.out.println(Timings.getStatistics());
  }

  public static void withLevel(final Module module, final LanguageLevel level, final Runnable r) {
    final LanguageLevelProjectExtension projectExt = LanguageLevelProjectExtension.getInstance(module.getProject());

    final LanguageLevel projectLevel = projectExt.getLanguageLevel();
    final LanguageLevel moduleLevel = LanguageLevelModuleExtension.getInstance(module).getLanguageLevel();
    try {
      projectExt.setLanguageLevel(level);
      setModuleLanguageLevel(module, level);
      r.run();
    }
    finally {
      setModuleLanguageLevel(module, moduleLevel);
      projectExt.setLanguageLevel(projectLevel);
    }
  }

  public static void setModuleLanguageLevel(Module module, final LanguageLevel level) {
    final LanguageLevelModuleExtension modifiable = (LanguageLevelModuleExtension)LanguageLevelModuleExtension.getInstance(module).getModifiableModel(true);
    modifiable.setLanguageLevel(level);
    modifiable.commit();
  }

  public static Sdk getMockJdk17() {
    return getMockJdk17("java 1.7");
  }

  public static Sdk getMockJdk17(@NotNull String name) {
    return JavaSdk.getInstance().createJdk(name, getMockJdk17Path().getPath(), false);
  }

  public static Sdk getMockJdk14() {
    return JavaSdk.getInstance().createJdk("java 1.4", getMockJdk14Path().getPath(), false);
  }

  public static File getMockJdk14Path() {
    return getPathForJdkNamed("mockJDK-1.4");
  }

  public static File getMockJdk17Path() {
    return getPathForJdkNamed("mockJDK-1.7");
  }

  private static File getPathForJdkNamed(String name) {
    File mockJdkCEPath = new File(PathManager.getHomePath(), "java/" + name);
    return mockJdkCEPath.exists() ? mockJdkCEPath : new File(PathManager.getHomePath(), "community/java/" + name);
  }

  public static Sdk getWebMockJdk17() {
    Sdk jdk = getMockJdk17();
    addWebJarsTo(jdk);
    return jdk;
  }

  public static void addWebJarsTo(@NotNull Sdk jdk) {
    SdkModificator sdkModificator = jdk.getSdkModificator();
    sdkModificator.addRoot(findJar("lib/jsp-api.jar"), OrderRootType.CLASSES);
    sdkModificator.addRoot(findJar("lib/servlet-api.jar"), OrderRootType.CLASSES);
    sdkModificator.commitChanges();
  }

  private static VirtualFile findJar(String name) {
    String path = PathManager.getHomePath() + '/' + name;
    VirtualFile file = LocalFileSystem.getInstance().findFileByPath(path);
    assert file != null : "not found: " + path;
    VirtualFile jar = ArchiveVfsUtil.getJarRootForLocalFile(file);
    assert jar != null : "no .jar for: " + path;
    return jar;
  }
}
