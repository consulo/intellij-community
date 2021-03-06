// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.roots.impl;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.vfs.VirtualFile;
import javax.annotation.Nonnull;

import java.util.List;

public abstract class PushedFilePropertiesUpdater {
  public abstract void runConcurrentlyIfPossible(List<Runnable> tasks);

  @Nonnull
  public static PushedFilePropertiesUpdater getInstance(@Nonnull Project project) {
    return project.getComponent(PushedFilePropertiesUpdater.class);
  }

  public abstract void initializeProperties();

  public abstract void pushAll(final FilePropertyPusher<?>... pushers);

  /**
   * @deprecated Use {@link #filePropertiesChanged(VirtualFile, Condition)}
   */
  @Deprecated
  public abstract void filePropertiesChanged(@Nonnull final VirtualFile file);

  public abstract void pushAllPropertiesNow();

  public abstract <T> void findAndUpdateValue(final VirtualFile fileOrDir, final FilePropertyPusher<T> pusher, final T moduleValue);

  /**
   * Invalidates indices and other caches for the given file or its immediate children (in case it's a directory).
   * Only files matching the condition are processed.
   */
  public abstract void filePropertiesChanged(@Nonnull VirtualFile fileOrDir, @Nonnull Condition<? super VirtualFile> acceptFileCondition);
}
