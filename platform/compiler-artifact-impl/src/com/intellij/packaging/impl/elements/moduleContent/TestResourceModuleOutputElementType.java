/*
 * Copyright 2013 Consulo.org
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
package com.intellij.packaging.impl.elements.moduleContent;

import org.mustbe.consulo.roots.impl.TestResourceContentFolderTypeProvider;

/**
 * @author VISTALL
 * @since 9:54/31.05.13
 */
public class TestResourceModuleOutputElementType extends ModuleOutputElementTypeBase {
  public static TestResourceModuleOutputElementType getInstance() {
    return getInstance(TestResourceModuleOutputElementType.class);
  }

  public TestResourceModuleOutputElementType() {
    super("module-test-resource-output", TestResourceContentFolderTypeProvider.getInstance());
  }
}