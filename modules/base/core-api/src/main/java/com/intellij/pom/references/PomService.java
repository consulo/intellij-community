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
package com.intellij.pom.references;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.pom.PomTarget;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiTarget;
import javax.annotation.Nonnull;

/**
 * @author peter
 */
public abstract class PomService {

  private static PomService getInstance(Project project) {
    return ServiceManager.getService(project, PomService.class);
  }

  @Nonnull
  protected abstract PsiElement convertToPsi(@Nonnull PomTarget target);

  public static PsiElement convertToPsi(@Nonnull Project project, @Nonnull PomTarget target) {
    return getInstance(project).convertToPsi(target);
  }

  public static PsiElement convertToPsi(@Nonnull PsiTarget target) {
    return getInstance(target.getNavigationElement().getProject()).convertToPsi((PomTarget)target);
  }

}
