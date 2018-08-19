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

package com.intellij.openapi.vcs.impl;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.vcs.ProjectLevelVcsManager;
import org.jdom.Element;

/**
 * @author yole
 */
@State(name = "VcsDirectoryMappings", storages = @Storage("vcs.xml"))
public class VcsDirectoryMappingStorage implements PersistentStateComponent<Element> {
  private final ProjectLevelVcsManager myVcsManager;

  public VcsDirectoryMappingStorage(final ProjectLevelVcsManager vcsManager) {
    myVcsManager = vcsManager;
  }

  @Override
  public Element getState() {
    final Element e = new Element("state");
    ((ProjectLevelVcsManagerImpl)myVcsManager).writeDirectoryMappings(e);
    return e;
  }

  @Override
  public void loadState(Element state) {
    ((ProjectLevelVcsManagerImpl)myVcsManager).readDirectoryMappings(state);
  }
}
