/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package com.intellij.execution.impl.statistics;

import com.intellij.execution.RunManager;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.ConfigurationType;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.internal.statistic.AbstractApplicationUsagesCollector;
import com.intellij.internal.statistic.beans.UsageDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.util.containers.ContainerUtil;
import java.util.HashSet;
import com.intellij.util.ui.UIUtil;

import javax.annotation.Nonnull;
import java.util.Set;

/**
 * @author Nikolay Matveev
 */
public abstract class AbstractRunConfigurationTypeUsagesCollector extends AbstractApplicationUsagesCollector {

  protected abstract boolean isApplicable(@Nonnull RunManager runManager, @Nonnull RunConfiguration runConfiguration);

  @Nonnull
  @Override
  public final Set<UsageDescriptor> getProjectUsages(@Nonnull final Project project) {
    final Set<String> runConfigurationTypes = new HashSet<String>();
    UIUtil.invokeAndWaitIfNeeded(new Runnable() {
      @Override
      public void run() {
        if (project.isDisposed()) return;
        final RunManager runManager = RunManager.getInstance(project);
        for (RunConfiguration runConfiguration : runManager.getAllConfigurationsList()) {
          if (runConfiguration != null && isApplicable(runManager, runConfiguration)) {
            final ConfigurationFactory configurationFactory = runConfiguration.getFactory();
            final ConfigurationType configurationType = configurationFactory.getType();
            final StringBuilder keyBuilder = new StringBuilder();
            keyBuilder.append(configurationType.getId());
            if (configurationType.getConfigurationFactories().length > 1) {
              keyBuilder.append(".").append(configurationFactory.getName());
            }
            runConfigurationTypes.add(keyBuilder.toString());
          }
        }
      }
    });
    return ContainerUtil.map2Set(runConfigurationTypes, runConfigurationType -> new UsageDescriptor(runConfigurationType, 1));
  }
}
