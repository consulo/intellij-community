/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package com.intellij.openapi.editor.actions;

import com.intellij.application.options.EditorFontsConstants;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorBundle;
import com.intellij.openapi.editor.impl.DesktopEditorImpl;
import com.intellij.openapi.project.DumbAware;
import consulo.editor.internal.EditorInternal;
import consulo.ui.annotation.RequiredUIAccess;

import javax.annotation.Nullable;

/**
 * @author Konstantin Bulenkov
 */
public abstract class ChangeEditorFontSizeAction extends AnAction implements DumbAware {
  private final int myStep;

  protected ChangeEditorFontSizeAction(@Nullable String text, int increaseStep) {
    super(text);
    myStep = increaseStep;
  }

  @RequiredUIAccess
  @Override
  public void actionPerformed(AnActionEvent e) {
    final EditorInternal editor = getEditor(e);
    if (editor != null) {
      final int size = editor.getFontSize() + myStep;
      if (size >= 8 && size <= EditorFontsConstants.getMaxEditorFontSize()) {
        editor.setFontSize(size);
      }
    }
  }

  @Nullable
  private static EditorInternal getEditor(AnActionEvent e) {
    final Editor editor = e.getData(CommonDataKeys.EDITOR);
    if (editor instanceof EditorInternal) {
      return (EditorInternal)editor;
    }
    return null;
  }

  @RequiredUIAccess
  @Override
  public void update(AnActionEvent e) {
    e.getPresentation().setEnabled(getEditor(e) != null);
  }

  public static class IncreaseEditorFontSize extends ChangeEditorFontSizeAction {
    public IncreaseEditorFontSize() {
      super(EditorBundle.message("increase.editor.font"), 1);
    }
  }

  public static class DecreaseEditorFontSize extends ChangeEditorFontSizeAction {
    public DecreaseEditorFontSize() {
      super(EditorBundle.message("decrease.editor.font"), -1);
    }
  }
}
