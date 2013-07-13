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
package com.intellij.openapi.vfs.impl.jar;

import com.intellij.openapi.vfs.ArchiveEntry;
import com.intellij.openapi.vfs.ArchiveFile;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * @author VISTALL
 * @since 18:40/13.07.13
 */
public class JarArchiveFile implements ArchiveFile {
  private final ZipFile myZipFile;

  public JarArchiveFile(ZipFile zipFile) {
    myZipFile = zipFile;
  }

  @Override
  public ArchiveEntry getEntry(String name) {
    ZipEntry entry = myZipFile.getEntry(name);
    if (entry == null) return null;
    return new JarArchiveEntry(entry);
  }

  @Override
  public InputStream getInputStream(ArchiveEntry entry) throws IOException {
    return myZipFile.getInputStream(((JarArchiveEntry)entry).getEntry());
  }

  @Override
  public Iterator<? extends ArchiveEntry> entries() {
    final Enumeration<? extends ZipEntry> entries = myZipFile.entries();
    return new Iterator<ArchiveEntry>() {
      @Override
      public boolean hasNext() {
        return entries.hasMoreElements();
      }

      @Override
      public ArchiveEntry next() {
        ZipEntry entry = entries.nextElement();
        if (entry == null) return null;
        return new JarArchiveEntry(entry);
      }

      @Override
      public void remove() {
      }
    };
  }

  @Nullable
  @Override
  public ZipFile getZipFile() {
    return myZipFile;
  }
}