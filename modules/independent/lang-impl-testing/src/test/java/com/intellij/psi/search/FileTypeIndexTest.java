/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package com.intellij.psi.search;

import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.fileTypes.ex.FakeFileType;
import com.intellij.openapi.fileTypes.ex.FileTypeManagerEx;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.fixtures.LightPlatformCodeInsightFixtureTestCase;
import javax.annotation.Nonnull;

/**
 * @author Dmitry Avdeev
 *         Date: 7/24/12
 */
public abstract class FileTypeIndexTest extends LightPlatformCodeInsightFixtureTestCase {
  public void testAddFileType() throws Exception {
    FileType foo = new FakeFileType() {
      @Override
      public boolean isMyFileType(VirtualFile file) {
        return true;
      }

      @Nonnull
      @Override
      public String getId() {
        return "foo";
      }

      @Nonnull
      @Override
      public String getDescription() {
        return "";
      }
    };
    FileTypeIndex index = new FileTypeIndex(FileTypeManager.getInstance());
    int version = index.getVersion();

    try {
      FileTypeManagerEx.getInstanceEx().registerFileType(foo);
      assertNotSame(version, index.getVersion());
    }
    finally {
      FileTypeManagerEx.getInstanceEx().unregisterFileType(foo);
    }
  }
}
