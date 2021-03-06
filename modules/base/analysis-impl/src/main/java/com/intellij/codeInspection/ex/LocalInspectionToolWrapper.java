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

package com.intellij.codeInspection.ex;

import com.intellij.codeInspection.GlobalInspectionContext;
import com.intellij.codeInspection.InspectionProfile;
import com.intellij.codeInspection.LocalInspectionEP;
import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NotNullLazyValue;
import com.intellij.profile.codeInspection.InspectionProjectProfileManager;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.TestOnly;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

/**
 * @author max
 */
public class LocalInspectionToolWrapper extends InspectionToolWrapper<LocalInspectionTool, LocalInspectionEP> {
  /** This should be used in tests primarily */
  @TestOnly
  public LocalInspectionToolWrapper(@Nonnull LocalInspectionTool tool) {
    super(tool, ourEPMap.getValue().get(tool.getShortName()));
  }

  public LocalInspectionToolWrapper(@Nonnull LocalInspectionEP ep) {
    super(ep);
  }

  private LocalInspectionToolWrapper(@Nonnull LocalInspectionToolWrapper other) {
    super(other);
  }

  @Nonnull
  @Override
  public LocalInspectionToolWrapper createCopy() {
    return new LocalInspectionToolWrapper(this);
  }

  @Override
  @Nonnull
  public JobDescriptor[] getJobDescriptors(@Nonnull GlobalInspectionContext context) {
    return context.getStdJobDescriptors().LOCAL_ANALYSIS_ARRAY;
  }


  public boolean isUnfair() {
    return myEP == null ? getTool() instanceof UnfairLocalInspectionTool : myEP.unfair;
  }

  public String getID() {
    return myEP == null ? getTool().getID() : myEP.id == null ? myEP.getShortName() : myEP.id;
  }

  @Nullable
  public String getAlternativeID() {
    return myEP == null ? getTool().getAlternativeID() : myEP.alternativeId;
  }

  public boolean runForWholeFile() {
    return myEP == null ? getTool().runForWholeFile() : myEP.runForWholeFile;
  }

  private static final NotNullLazyValue<Map<String, LocalInspectionEP>> ourEPMap = new NotNullLazyValue<Map<String, LocalInspectionEP>>() {
    @Nonnull
    @Override
    protected Map<String, LocalInspectionEP> compute() {
      HashMap<String, LocalInspectionEP> map = new HashMap<>();
      for (LocalInspectionEP ep : LocalInspectionEP.LOCAL_INSPECTION.getExtensionList()) {
        map.put(ep.getShortName(), ep);
      }
      return map;
    }
  };

  public static InspectionToolWrapper findTool2RunInBatch(@Nonnull Project project, @Nullable PsiElement element, @Nonnull String name) {
    final InspectionProfile inspectionProfile = InspectionProjectProfileManager.getInstance(project).getInspectionProfile();
    final InspectionToolWrapper toolWrapper = element == null
                                              ? inspectionProfile.getInspectionTool(name, project)
                                              : inspectionProfile.getInspectionTool(name, element);
    if (toolWrapper instanceof LocalInspectionToolWrapper && ((LocalInspectionToolWrapper)toolWrapper).isUnfair()) {
      final LocalInspectionTool inspectionTool = ((LocalInspectionToolWrapper)toolWrapper).getTool();
      if (inspectionTool instanceof PairedUnfairLocalInspectionTool) {
        final String oppositeShortName = ((PairedUnfairLocalInspectionTool)inspectionTool).getInspectionForBatchShortName();
        return element == null
               ? inspectionProfile.getInspectionTool(oppositeShortName, project)
               : inspectionProfile.getInspectionTool(oppositeShortName, element);
      }
      return null;
    }
    return toolWrapper;
  }
}
