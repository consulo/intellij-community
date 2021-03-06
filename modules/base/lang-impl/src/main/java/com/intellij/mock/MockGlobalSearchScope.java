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
package com.intellij.mock;

import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.module.Module;
import javax.annotation.Nonnull;

/**
 * @author peter
 */
public class MockGlobalSearchScope extends GlobalSearchScope {
  @Override
  public boolean contains(@Nonnull final VirtualFile file) {
    return true;
  }

  @Override
  public int compare(@Nonnull final VirtualFile file1, @Nonnull final VirtualFile file2) {
    return 0;
  }

  @Override
  public boolean isSearchInModuleContent(@Nonnull final Module aModule) {
    return true;
  }

  @Override
  public boolean isSearchInLibraries() {
    return true;
  }
}