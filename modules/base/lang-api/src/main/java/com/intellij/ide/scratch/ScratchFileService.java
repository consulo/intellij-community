/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package com.intellij.ide.scratch;

import com.intellij.lang.Language;
import com.intellij.lang.PerFileMappings;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.vfs.VirtualFile;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;

public abstract class ScratchFileService {

  public enum Option { existing_only, create_if_missing, create_new_always }

  public static ScratchFileService getInstance() {
    return ServiceManager.getService(ScratchFileService.class);
  }

  @Nonnull
  public abstract String getRootPath(@Nonnull RootType rootId);

  @Nullable
  public abstract RootType getRootType(@Nullable VirtualFile file);

  public abstract VirtualFile findFile(@Nonnull RootType rootType, @Nonnull String pathName, @Nonnull Option option) throws IOException;

  @Nonnull
  public abstract PerFileMappings<Language> getScratchesMapping();

  @Nullable
  public static RootType findRootType(@Nullable VirtualFile file) {
    if (file == null || !file.isInLocalFileSystem()) return null;
    VirtualFile parent = file.isDirectory() ? file : file.getParent();
    return getInstance().getRootType(parent);
  }

  /**
   * @deprecated use {@link ScratchFileService#findRootType(VirtualFile)} or {@link ScratchUtil#isScratch(VirtualFile)}
   */
  @Deprecated
  public static boolean isInScratchRoot(@Nullable VirtualFile file) {
    return findRootType(file) != null;
  }
}
