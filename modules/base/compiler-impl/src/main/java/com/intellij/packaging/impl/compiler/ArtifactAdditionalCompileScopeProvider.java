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
package com.intellij.packaging.impl.compiler;

import com.intellij.compiler.impl.AdditionalCompileScopeProvider;
import com.intellij.compiler.impl.ModuleCompileScope;
import com.intellij.openapi.compiler.CompileScope;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.ThrowableComputable;
import com.intellij.packaging.artifacts.Artifact;
import consulo.application.AccessRule;

import javax.annotation.Nonnull;
import java.util.Set;

/**
 * @author nik
 */
public class ArtifactAdditionalCompileScopeProvider extends AdditionalCompileScopeProvider {
  @Override
  public CompileScope getAdditionalScope(@Nonnull final CompileScope baseScope, @Nonnull Condition<com.intellij.openapi.compiler.Compiler> filter, @Nonnull final Project project) {
    if (ArtifactCompileScope.getArtifacts(baseScope) != null) {
      return null;
    }
    final ArtifactsCompiler compiler = ArtifactsCompiler.getInstance(project);
    if (compiler == null || !filter.value(compiler)) {
      return null;
    }
    ThrowableComputable<ModuleCompileScope,RuntimeException> action = () -> {
      final Set<Artifact> artifacts = ArtifactCompileScope.getArtifactsToBuild(project, baseScope, false);
      return ArtifactCompileScope.createScopeForModulesInArtifacts(project, artifacts);
    };
    return AccessRule.read(action);
  }
}
