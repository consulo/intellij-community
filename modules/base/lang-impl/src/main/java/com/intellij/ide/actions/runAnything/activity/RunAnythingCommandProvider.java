// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.actions.runAnything.activity;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.configurations.PtyCommandLine;
import com.intellij.execution.runners.ExecutionEnvironmentBuilder;
import com.intellij.icons.AllIcons;
import com.intellij.ide.IdeBundle;
import com.intellij.ide.actions.runAnything.RunAnythingAction;
import com.intellij.ide.actions.runAnything.RunAnythingCache;
import com.intellij.ide.actions.runAnything.commands.RunAnythingCommandCustomizer;
import com.intellij.ide.actions.runAnything.execution.RunAnythingRunProfile;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.execution.ParametersListUtil;
import consulo.ui.image.Image;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.Collection;

import static com.intellij.ide.actions.runAnything.RunAnythingUtil.*;

public abstract class RunAnythingCommandProvider extends RunAnythingProviderBase<String> {
  @Override
  public void execute(@Nonnull DataContext dataContext, @Nonnull String value) {
    VirtualFile workDirectory = dataContext.getData(CommonDataKeys.VIRTUAL_FILE);
    Executor executor = dataContext.getData(RunAnythingAction.EXECUTOR_KEY);
    LOG.assertTrue(workDirectory != null);
    LOG.assertTrue(executor != null);

    runCommand(workDirectory, value, executor, dataContext);
  }

  public static void runCommand(@Nonnull VirtualFile workDirectory, @Nonnull String commandString, @Nonnull Executor executor, @Nonnull DataContext dataContext) {
    final Project project = dataContext.getData(CommonDataKeys.PROJECT);
    LOG.assertTrue(project != null);

    Collection<String> commands = RunAnythingCache.getInstance(project).getState().getCommands();
    commands.remove(commandString);
    commands.add(commandString);

    dataContext = RunAnythingCommandCustomizer.customizeContext(dataContext);

    GeneralCommandLine initialCommandLine = new GeneralCommandLine(ParametersListUtil.parse(commandString, false, true)).withParentEnvironmentType(GeneralCommandLine.ParentEnvironmentType.CONSOLE).withWorkDirectory(workDirectory.getPath());

    GeneralCommandLine commandLine = RunAnythingCommandCustomizer.customizeCommandLine(dataContext, workDirectory, initialCommandLine);
    try {
      RunAnythingRunProfile runAnythingRunProfile = new RunAnythingRunProfile(Registry.is("run.anything.use.pty", false) ? new PtyCommandLine(commandLine) : commandLine, commandString);
      ExecutionEnvironmentBuilder.create(project, executor, runAnythingRunProfile).dataContext(dataContext).buildAndExecute();
    }
    catch (ExecutionException e) {
      LOG.warn(e);
      Messages.showInfoMessage(project, e.getMessage(), IdeBundle.message("run.anything.console.error.title"));
    }
  }

  @Nullable
  @Override
  public String getAdText() {
    return AD_CONTEXT_TEXT + ", " + AD_DEBUG_TEXT + ", " + AD_DELETE_COMMAND_TEXT;
  }

  @Nonnull
  @Override
  public String getCommand(@Nonnull String value) {
    return value;
  }

  @Nullable
  @Override
  public Image getIcon(@Nonnull String value) {
    return AllIcons.Actions.Run_anything;
  }
}