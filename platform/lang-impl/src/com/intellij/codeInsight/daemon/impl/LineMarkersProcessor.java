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

package com.intellij.codeInsight.daemon.impl;

import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.LineMarkerProvider;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.mustbe.consulo.RequiredReadAction;

import java.util.List;

/**
 * @author cdr
 */
public interface LineMarkersProcessor {
  @RequiredReadAction
  void addLineMarkers(@NotNull List<PsiElement> elements,
                      @NotNull List<LineMarkerProvider> providers,
                      @NotNull List<LineMarkerInfo> result,
                      @NotNull ProgressIndicator progress) throws ProcessCanceledException;
}
