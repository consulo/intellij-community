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
package com.intellij.packaging.impl.compiler;

import com.intellij.openapi.components.*;
import com.intellij.openapi.project.Project;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.packaging.artifacts.ArtifactManager;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.xmlb.annotations.AbstractCollection;
import com.intellij.util.xmlb.annotations.Tag;

import javax.annotation.Nonnull;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author nik
 */
@Singleton
@State(name = "ArtifactsWorkspaceSettings", storages = {@Storage(StoragePathMacros.WORKSPACE_FILE)})
public class ArtifactsWorkspaceSettings implements PersistentStateComponent<ArtifactsWorkspaceSettings.ArtifactsWorkspaceSettingsState> {
  public static class ArtifactsWorkspaceSettingsState {
    @Tag("artifacts-to-build")
    @AbstractCollection(surroundWithTag = false, elementTag = "artifact", elementValueAttribute = "name")
    public List<String> myArtifactsToBuild = new ArrayList<>();
  }

  public static ArtifactsWorkspaceSettings getInstance(@Nonnull Project project) {
    return ServiceManager.getService(project, ArtifactsWorkspaceSettings.class);
  }

  private final ArtifactManager myArtifactManager;
  private ArtifactsWorkspaceSettingsState myState = new ArtifactsWorkspaceSettingsState();

  @Inject
  public ArtifactsWorkspaceSettings(ArtifactManager artifactManager) {
    myArtifactManager = artifactManager;
  }

  public List<Artifact> getArtifactsToBuild() {
    final List<Artifact> result = new ArrayList<>();
    for (String name : myState.myArtifactsToBuild) {
      ContainerUtil.addIfNotNull(result, myArtifactManager.findArtifact(name));
    }
    return result;
  }

  public void setArtifactsToBuild(@Nonnull Collection<? extends Artifact> artifacts) {
    myState.myArtifactsToBuild.clear();
    for (Artifact artifact : artifacts) {
      myState.myArtifactsToBuild.add(artifact.getName());
    }
    Collections.sort(myState.myArtifactsToBuild);
  }

  @Override
  public ArtifactsWorkspaceSettingsState getState() {
    return myState;
  }

  @Override
  public void loadState(ArtifactsWorkspaceSettingsState state) {
    myState = state;
  }
}
