/*
 * Copyright 2013-2017 consulo.io
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
package consulo.web.wm.impl;

import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.WindowInfo;
import com.intellij.openapi.wm.impl.InternalDecoratorListener;
import com.intellij.openapi.wm.impl.WindowInfoImpl;
import com.intellij.util.EventDispatcher;
import consulo.ui.*;
import consulo.ui.ex.ToolWindowInternalDecorator;
import consulo.wm.impl.UnifiedToolWindowImpl;
import org.jetbrains.annotations.NotNull;

/**
 * @author VISTALL
 * @since 12-Oct-17
 */
public class WebToolWindowInternalDecorator implements ToolWindowInternalDecorator {
  private final WindowInfoImpl myWindowInfo;
  private final UnifiedToolWindowImpl myToolWindow;
  private final EventDispatcher<InternalDecoratorListener> myDispatcher = EventDispatcher.create(InternalDecoratorListener.class);

  private DockLayout myLayout;

  @RequiredUIAccess
  public WebToolWindowInternalDecorator(Project project, WindowInfoImpl windowInfo, UnifiedToolWindowImpl toolWindow, boolean canWorkInDumbMode) {
    myWindowInfo = windowInfo;
    myToolWindow = toolWindow;

    myLayout = DockLayout.create();
    myLayout.top(Label.create(toolWindow.getTitle()));
    myLayout.center(toolWindow.getUIComponent());
  }

  @NotNull
  public Component getComponent() {
    return myLayout;
  }

  @NotNull
  @Override
  public WindowInfo getWindowInfo() {
    return myWindowInfo;
  }

  @Override
  public void apply(@NotNull WindowInfo windowInfo) {

  }

  @NotNull
  @Override
  public ToolWindow getToolWindow() {
    return myToolWindow;
  }

  @Override
  public void addInternalDecoratorListener(InternalDecoratorListener l) {
    myDispatcher.addListener(l);
  }

  @Override
  public void removeInternalDecoratorListener(InternalDecoratorListener l) {
    myDispatcher.removeListener(l);
  }

  @Override
  public void fireActivated() {
    myDispatcher.getMulticaster().activated(this);
  }

  @Override
  public void fireHidden() {
    myDispatcher.getMulticaster().hidden(this);
  }

  @Override
  public void fireHiddenSide() {
    myDispatcher.getMulticaster().hiddenSide(this);
  }

  @NotNull
  @Override
  public ActionGroup createPopupGroup() {
    return new DefaultActionGroup();
  }

  @Override
  public boolean isFocused() {
    return false;
  }

  @Override
  public boolean hasFocus() {
    return false;
  }

  @Override
  public void dispose() {

  }
}
