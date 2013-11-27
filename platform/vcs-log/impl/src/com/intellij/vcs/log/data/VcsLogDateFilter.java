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
package com.intellij.vcs.log.data;

import com.intellij.vcs.log.VcsFullCommitDetails;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Date;

public class VcsLogDateFilter implements VcsLogDetailsFilter {

  @Nullable private final Date myAfter;
  @Nullable private final Date myBefore;

  public VcsLogDateFilter(@Nullable Date after, @Nullable Date before) {
    myAfter = after;
    myBefore = before;
  }

  @Override
  public boolean matches(@NotNull VcsFullCommitDetails details) {
    Date date = new Date(details.getCommitTime());  // behave like Git does: it filters by commit time, not author time
    boolean matches = true;
    if (myAfter != null) {
      matches &= date.after(myAfter);
    }
    if (myBefore != null) {
      matches &= date.before(myBefore);
    }
    return matches;
  }

  @Nullable
  public Date getAfter() {
    return myAfter;
  }

  @Nullable
  public Date getBefore() {
    return myBefore;
  }

}