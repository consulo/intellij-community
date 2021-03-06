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
package com.intellij.openapi.actionSystem.impl;

import com.intellij.openapi.extensions.ExtensionPointName;
import javax.annotation.Nonnull;

/**
 * @author Roman.Chernyatchik
 */
public interface BundledQuickListsProvider {
    ExtensionPointName<BundledQuickListsProvider> EP_NAME = ExtensionPointName.create("com.intellij.bundledQuickListsProvider");

  /**
   * Provides custom bundled actions quick lists.
   * @return Array of relative paths without extensions for lists.
   * E.g. : ["/quickLists/myList", "otherList"] for quickLists/myList.xml, otherList.xml
   */
  @Nonnull
  String[] getBundledListsRelativePaths();
}
