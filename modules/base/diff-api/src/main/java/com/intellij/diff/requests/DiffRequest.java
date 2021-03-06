/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package com.intellij.diff.requests;

import consulo.util.dataholder.Key;
import consulo.util.dataholder.UserDataHolder;
import consulo.util.dataholder.UserDataHolderBase;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import consulo.ui.annotation.RequiredUIAccess;

public abstract class DiffRequest implements UserDataHolder {
  protected final UserDataHolderBase myUserDataHolder = new UserDataHolderBase();

  @Nullable
  public abstract String getTitle();

  /*
   * Called when DiffRequest is shown
   *
   * Implementors may use this notification to add and remove listeners to avoid memory leaks.
   * DiffRequest could be shown multiple times, so implementors should count assignments
   *
   * @param isAssigned true means request processing started, false means processing has stopped.
   *                   Total number of calls with true should be same as for false
   */
  @RequiredUIAccess
  public void onAssigned(boolean isAssigned) {
  }

  @javax.annotation.Nullable
  @Override
  public <T> T getUserData(@Nonnull Key<T> key) {
    return myUserDataHolder.getUserData(key);
  }

  @Override
  public <T> void putUserData(@Nonnull Key<T> key, @Nullable T value) {
    myUserDataHolder.putUserData(key, value);
  }
}
