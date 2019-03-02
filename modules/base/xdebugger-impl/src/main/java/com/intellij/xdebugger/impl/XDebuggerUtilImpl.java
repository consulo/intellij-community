/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package com.intellij.xdebugger.impl;

import com.intellij.lang.Language;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.IdeActions;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.markup.HighlighterTargetArea;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.fileTypes.InternalStdFileTypes;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.PopupStep;
import com.intellij.openapi.ui.popup.util.BaseListPopupStep;
import com.intellij.openapi.util.AsyncResult;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.pom.Navigatable;
import com.intellij.pom.NonNavigatable;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.tree.injected.InjectedLanguageUtil;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.ui.popup.list.ListPopupImpl;
import com.intellij.util.DocumentUtil;
import com.intellij.util.Processor;
import com.intellij.xdebugger.*;
import com.intellij.xdebugger.breakpoints.*;
import com.intellij.xdebugger.breakpoints.ui.XBreakpointGroupingRule;
import com.intellij.xdebugger.evaluation.EvaluationMode;
import com.intellij.xdebugger.evaluation.XDebuggerEvaluator;
import com.intellij.xdebugger.frame.XExecutionStack;
import com.intellij.xdebugger.frame.XStackFrame;
import com.intellij.xdebugger.frame.XSuspendContext;
import com.intellij.xdebugger.frame.XValueContainer;
import com.intellij.xdebugger.impl.breakpoints.XBreakpointUtil;
import com.intellij.xdebugger.impl.breakpoints.XExpressionImpl;
import com.intellij.xdebugger.impl.breakpoints.ui.grouping.XBreakpointFileGroupingRule;
import com.intellij.xdebugger.impl.evaluate.quick.common.ValueLookupManager;
import com.intellij.xdebugger.impl.settings.XDebuggerSettingManagerImpl;
import com.intellij.xdebugger.impl.ui.DebuggerUIUtil;
import com.intellij.xdebugger.impl.ui.tree.actions.XDebuggerTreeActionBase;
import com.intellij.xdebugger.settings.XDebuggerSettings;
import com.intellij.xdebugger.ui.DebuggerColors;
import consulo.ui.RequiredUIAccess;
import consulo.annotations.RequiredReadAction;
import consulo.awt.TargetAWT;
import consulo.xdebugger.breakpoints.XLineBreakpointResolverTypeExtension;
import gnu.trove.THashMap;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Singleton;
import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.util.*;

/**
 * @author nik
 */
@Singleton
public class XDebuggerUtilImpl extends XDebuggerUtil {
  private XLineBreakpointType<?>[] myLineBreakpointTypes;
  private Map<Class<? extends XBreakpointType>, XBreakpointType<?, ?>> myBreakpointTypeByClass;

  @Override
  public XLineBreakpointType<?>[] getLineBreakpointTypes() {
    if (myLineBreakpointTypes == null) {
      List<XBreakpointType> types = XBreakpointUtil.getBreakpointTypes();
      List<XLineBreakpointType<?>> lineBreakpointTypes = new ArrayList<XLineBreakpointType<?>>();
      for (XBreakpointType type : types) {
        if (type instanceof XLineBreakpointType<?>) {
          lineBreakpointTypes.add((XLineBreakpointType<?>)type);
        }
      }
      myLineBreakpointTypes = lineBreakpointTypes.toArray(new XLineBreakpointType<?>[lineBreakpointTypes.size()]);
    }
    return myLineBreakpointTypes;
  }

  @Override
  public void toggleLineBreakpoint(@Nonnull final Project project, @Nonnull final VirtualFile file, final int line, boolean temporary) {
    XLineBreakpointType<?> breakpointType = XLineBreakpointResolverTypeExtension.INSTANCE.resolveBreakpointType(project, file, line);
    if (breakpointType != null) {
      toggleLineBreakpoint(project, breakpointType, file, line, temporary);
    }
  }

  @Override
  public boolean canPutBreakpointAt(@Nonnull Project project, @Nonnull VirtualFile file, int line) {
    return XLineBreakpointResolverTypeExtension.INSTANCE.resolveBreakpointType(project, file, line) != null;
  }

  @Override
  public <P extends XBreakpointProperties> void toggleLineBreakpoint(@Nonnull final Project project,
                                                                     @Nonnull final XLineBreakpointType<P> type,
                                                                     @Nonnull final VirtualFile file,
                                                                     final int line,
                                                                     final boolean temporary) {
    XSourcePositionImpl position = XSourcePositionImpl.create(file, line);
    if (position != null) {
      toggleAndReturnLineBreakpoint(project, type, position, temporary, null);
    }
  }

  @Nonnull
  public static <P extends XBreakpointProperties> AsyncResult<XLineBreakpoint> toggleAndReturnLineBreakpoint(@Nonnull final Project project,
                                                                                                             @Nonnull final XLineBreakpointType<P> type,
                                                                                                             @Nonnull final XSourcePosition position,
                                                                                                             final boolean temporary,
                                                                                                             @Nullable final Editor editor) {
    return WriteAction.compute(() -> {
      final VirtualFile file = position.getFile();
      final int line = position.getLine();
      final XBreakpointManager breakpointManager = XDebuggerManager.getInstance(project).getBreakpointManager();
      XLineBreakpoint<P> breakpoint = breakpointManager.findBreakpointAtLine(type, file, line);
      if (breakpoint != null) {
        breakpointManager.removeBreakpoint(breakpoint);
      }
      else {
        List<? extends XLineBreakpointType<P>.XLineBreakpointVariant> variants = type.computeVariants(project, position);
        if (!variants.isEmpty() && editor != null) {
          RelativePoint relativePoint = DebuggerUIUtil.getPositionForPopup(editor, line);
          if (variants.size() > 1 && relativePoint != null) {
            final AsyncResult<XLineBreakpoint> res = new AsyncResult<XLineBreakpoint>();
            class MySelectionListener implements ListSelectionListener {
              RangeHighlighter myHighlighter = null;

              @Override
              public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()) {
                  clearHighlighter();
                  Object value = ((JList)e.getSource()).getSelectedValue();
                  if (value instanceof XLineBreakpointType.XLineBreakpointVariant) {
                    TextRange range = ((XLineBreakpointType.XLineBreakpointVariant)value).getHighlightRange();
                    TextRange lineRange = DocumentUtil.getLineTextRange(editor.getDocument(), line);
                    if (range != null) {
                      range = range.intersection(lineRange);
                    }
                    else {
                      range = lineRange;
                    }
                    if (range != null && !range.isEmpty()) {
                      EditorColorsScheme scheme = EditorColorsManager.getInstance().getGlobalScheme();
                      TextAttributes attributes = scheme.getAttributes(DebuggerColors.BREAKPOINT_ATTRIBUTES);
                      myHighlighter = editor.getMarkupModel()
                              .addRangeHighlighter(range.getStartOffset(), range.getEndOffset(), DebuggerColors.BREAKPOINT_HIGHLIGHTER_LAYER, attributes,
                                                   HighlighterTargetArea.EXACT_RANGE);
                    }
                  }
                }
              }

              private void clearHighlighter() {
                if (myHighlighter != null) {
                  myHighlighter.dispose();
                }
              }
            }

            // calculate default item
            int caretOffset = editor.getCaretModel().getOffset();
            XLineBreakpointType<P>.XLineBreakpointVariant defaultVariant = null;
            for (XLineBreakpointType<P>.XLineBreakpointVariant variant : variants) {
              TextRange range = variant.getHighlightRange();
              if (range != null && range.contains(caretOffset)) {
                //noinspection ConstantConditions
                if (defaultVariant == null || defaultVariant.getHighlightRange().getLength() > range.getLength()) {
                  defaultVariant = variant;
                }
              }
            }
            final int defaultIndex = defaultVariant != null ? variants.indexOf(defaultVariant) : 0;

            final MySelectionListener selectionListener = new MySelectionListener();
            ListPopupImpl popup = new ListPopupImpl(new BaseListPopupStep<XLineBreakpointType.XLineBreakpointVariant>("Create breakpoint for", variants) {
              @Nonnull
              @Override
              public String getTextFor(XLineBreakpointType.XLineBreakpointVariant value) {
                return value.getText();
              }

              @Override
              public Icon getIconFor(XLineBreakpointType.XLineBreakpointVariant value) {
                return TargetAWT.to(value.getIcon());
              }

              @Override
              public void canceled() {
                selectionListener.clearHighlighter();
              }

              @Override
              @RequiredUIAccess
              public PopupStep onChosen(final XLineBreakpointType.XLineBreakpointVariant selectedValue, boolean finalChoice) {
                selectionListener.clearHighlighter();
                ApplicationManager.getApplication().runWriteAction(new Runnable() {
                  @Override
                  public void run() {
                    P properties = (P)selectedValue.createProperties();
                    res.setDone(breakpointManager.addLineBreakpoint(type, file.getUrl(), line, properties, temporary));
                  }
                });
                return FINAL_CHOICE;
              }

              @Override
              public int getDefaultOptionIndex() {
                return defaultIndex;
              }
            });
            DebuggerUIUtil.registerExtraHandleShortcuts(popup, IdeActions.ACTION_TOGGLE_LINE_BREAKPOINT);
            popup.setAdText(DebuggerUIUtil.getSelectionShortcutsAdText(IdeActions.ACTION_TOGGLE_LINE_BREAKPOINT));
            popup.addListSelectionListener(selectionListener);
            popup.show(relativePoint);
            return res;
          }
          else {
            P properties = variants.get(0).createProperties();
            return AsyncResult.done((XLineBreakpoint)breakpointManager.addLineBreakpoint(type, file.getUrl(), line, properties, temporary));
          }
        }
        P properties = type.createBreakpointProperties(file, line);
        return AsyncResult.done((XLineBreakpoint)breakpointManager.addLineBreakpoint(type, file.getUrl(), line, properties, temporary));
      }
      return AsyncResult.<XLineBreakpoint>rejected();
    });
  }


  public static <P extends XBreakpointProperties> XLineBreakpoint toggleAndReturnLineBreakpoint(@Nonnull final Project project,
                                                                                                @Nonnull final XLineBreakpointType<P> type,
                                                                                                @Nonnull final VirtualFile file,
                                                                                                final int line,
                                                                                                final boolean temporary) {
    return WriteAction.compute(() -> {
      XBreakpointManager breakpointManager = XDebuggerManager.getInstance(project).getBreakpointManager();
      XLineBreakpoint<P> breakpoint = breakpointManager.findBreakpointAtLine(type, file, line);
      if (breakpoint != null) {
        breakpointManager.removeBreakpoint(breakpoint);
        return null;
      }
      else {
        P properties = type.createBreakpointProperties(file, line);
        return breakpointManager.addLineBreakpoint(type, file.getUrl(), line, properties, temporary);
      }
    });
  }

  @Override
  public void removeBreakpoint(final Project project, final XBreakpoint<?> breakpoint) {
    WriteAction.run(() -> {
      XDebuggerManager.getInstance(project).getBreakpointManager().removeBreakpoint(breakpoint);
    });
  }

  @Override
  public <B extends XBreakpoint<?>> XBreakpointType<B, ?> findBreakpointType(@Nonnull Class<? extends XBreakpointType<B, ?>> typeClass) {
    if (myBreakpointTypeByClass == null) {
      myBreakpointTypeByClass = new THashMap<Class<? extends XBreakpointType>, XBreakpointType<?, ?>>();
      for (XBreakpointType<?, ?> breakpointType : XBreakpointUtil.getBreakpointTypes()) {
        myBreakpointTypeByClass.put(breakpointType.getClass(), breakpointType);
      }
    }
    XBreakpointType<?, ?> type = myBreakpointTypeByClass.get(typeClass);
    //noinspection unchecked
    return (XBreakpointType<B, ?>)type;
  }

  @Override
  public <T extends XDebuggerSettings<?>> T getDebuggerSettings(Class<T> aClass) {
    return XDebuggerSettingManagerImpl.getInstanceImpl().getSettings(aClass);
  }

  @Override
  public XValueContainer getValueContainer(DataContext dataContext) {
    return XDebuggerTreeActionBase.getSelectedValue(dataContext);
  }

  @Override
  @Nullable
  public XSourcePosition createPosition(final VirtualFile file, final int line) {
    return XSourcePositionImpl.create(file, line);
  }

  @Override
  @Nullable
  public XSourcePosition createPositionByOffset(final VirtualFile file, final int offset) {
    return XSourcePositionImpl.createByOffset(file, offset);
  }

  @Override
  @Nullable
  public XSourcePosition createPositionByElement(PsiElement element) {
    if (element == null) return null;

    PsiFile psiFile = element.getContainingFile();
    if (psiFile == null) return null;

    final VirtualFile file = psiFile.getVirtualFile();
    if (file == null) return null;

    final SmartPsiElementPointer<PsiElement> pointer =
            SmartPointerManager.getInstance(element.getProject()).createSmartPsiElementPointer(element);

    return new XSourcePosition() {
      private volatile XSourcePosition myDelegate;

      private XSourcePosition getDelegate() {
        if (myDelegate == null) {
          myDelegate = ApplicationManager.getApplication().runReadAction((Computable<XSourcePosition>)() -> {
            PsiElement elem = pointer.getElement();
            return XSourcePositionImpl.createByOffset(pointer.getVirtualFile(), elem != null ? elem.getTextOffset() : -1);
          });
        }
        return myDelegate;
      }

      @Override
      public int getLine() {
        return getDelegate().getLine();
      }

      @Override
      public int getOffset() {
        return getDelegate().getOffset();
      }

      @Nonnull
      @Override
      public VirtualFile getFile() {
        return file;
      }

      @Nonnull
      @Override
      public Navigatable createNavigatable(@Nonnull Project project) {
        // no need to create delegate here, it may be expensive
        if (myDelegate != null) {
          return myDelegate.createNavigatable(project);
        }
        PsiElement elem = pointer.getElement();
        if (elem instanceof Navigatable) {
          return ((Navigatable)elem);
        }
        return NonNavigatable.INSTANCE;
      }
    };
  }

  @Override
  public <B extends XLineBreakpoint<?>> XBreakpointGroupingRule<B, ?> getGroupingByFileRule() {
    return new XBreakpointFileGroupingRule<B>();
  }

  @Nullable
  public static XSourcePosition getCaretPosition(@Nonnull Project project, DataContext context) {
    Editor editor = getEditor(project, context);
    if (editor == null) return null;

    final Document document = editor.getDocument();
    final int line = editor.getCaretModel().getLogicalPosition().line;
    VirtualFile file = FileDocumentManager.getInstance().getFile(document);
    return XSourcePositionImpl.create(file, line);
  }

  @Nonnull
  public static Collection<XSourcePosition> getAllCaretsPositions(@Nonnull Project project, DataContext context) {
    Editor editor = getEditor(project, context);
    if (editor == null) {
      return Collections.emptyList();
    }

    final Document document = editor.getDocument();
    VirtualFile file = FileDocumentManager.getInstance().getFile(document);
    Collection<XSourcePosition> res = new ArrayList<XSourcePosition>();
    List<Caret> carets = editor.getCaretModel().getAllCarets();
    for (Caret caret : carets) {
      int line = caret.getLogicalPosition().line;
      XSourcePositionImpl position = XSourcePositionImpl.create(file, line);
      if (position != null) {
        res.add(position);
      }
    }
    return res;
  }

  @Nullable
  private static Editor getEditor(@Nonnull Project project, DataContext context) {
    Editor editor = context.getData(CommonDataKeys.EDITOR);
    if (editor == null) {
      return FileEditorManager.getInstance(project).getSelectedTextEditor();
    }
    return editor;
  }

  @Override
  public <B extends XBreakpoint<?>> Comparator<B> getDefaultBreakpointComparator(final XBreakpointType<B, ?> type) {
    return new Comparator<B>() {
      @Override
      public int compare(final B o1, final B o2) {
        return type.getDisplayText(o1).compareTo(type.getDisplayText(o2));
      }
    };
  }

  @Override
  public <P extends XBreakpointProperties> Comparator<XLineBreakpoint<P>> getDefaultLineBreakpointComparator() {
    return new Comparator<XLineBreakpoint<P>>() {
      @Override
      public int compare(final XLineBreakpoint<P> o1, final XLineBreakpoint<P> o2) {
        int fileCompare = o1.getFileUrl().compareTo(o2.getFileUrl());
        if (fileCompare != 0) return fileCompare;
        return o1.getLine() - o2.getLine();
      }
    };
  }

  @Nullable
  public static XDebuggerEvaluator getEvaluator(final XSuspendContext suspendContext) {
    XExecutionStack executionStack = suspendContext.getActiveExecutionStack();
    if (executionStack != null) {
      XStackFrame stackFrame = executionStack.getTopFrame();
      if (stackFrame != null) {
        return stackFrame.getEvaluator();
      }
    }
    return null;
  }

  @RequiredReadAction
  @Override
  public void iterateLine(@Nonnull Project project, @Nonnull Document document, int line, @Nonnull Processor<PsiElement> processor) {
    PsiFile file = PsiDocumentManager.getInstance(project).getPsiFile(document);
    if (file == null) {
      return;
    }

    int lineStart;
    int lineEnd;
    try {
      lineStart = document.getLineStartOffset(line);
      lineEnd = document.getLineEndOffset(line);
    }
    catch (IndexOutOfBoundsException ignored) {
      return;
    }

    PsiElement element;
    int offset = lineStart;
    while (offset < lineEnd) {
      element = file.findElementAt(offset);
      if (element != null) {
        if (!processor.process(element)) {
          return;
        }
        else {
          offset = element.getTextRange().getEndOffset();
        }
      }
      else {
        offset++;
      }
    }
  }

  @Override
  public <B extends XLineBreakpoint<?>> List<XBreakpointGroupingRule<B, ?>> getGroupingByFileRuleAsList() {
    return Collections.<XBreakpointGroupingRule<B, ?>>singletonList(this.<B>getGroupingByFileRule());
  }

  @RequiredReadAction
  @Override
  @Nullable
  public PsiElement findContextElement(@Nonnull VirtualFile virtualFile, int offset, @Nonnull Project project, boolean checkXml) {
    if (!virtualFile.isValid()) {
      return null;
    }

    Document document = FileDocumentManager.getInstance().getDocument(virtualFile);
    PsiFile file = document == null ? null : PsiManager.getInstance(project).findFile(virtualFile);
    if (file == null) {
      return null;
    }

    if (offset < 0) {
      offset = 0;
    }
    if (offset > document.getTextLength()) {
      offset = document.getTextLength();
    }
    int startOffset = offset;

    int lineEndOffset = document.getLineEndOffset(document.getLineNumber(offset));
    PsiElement result = null;
    do {
      PsiElement element = file.findElementAt(offset);
      if (!(element instanceof PsiWhiteSpace) && !(element instanceof PsiComment)) {
        result = element;
        break;
      }

      offset = element.getTextRange().getEndOffset() + 1;
    }
    while (offset < lineEndOffset);

    if (result == null) {
      result = file.findElementAt(startOffset);
    }

    if (checkXml && result != null && InternalStdFileTypes.XML.getLanguage().equals(result.getLanguage())) {
      PsiLanguageInjectionHost parent = PsiTreeUtil.getParentOfType(result, PsiLanguageInjectionHost.class);
      if (parent != null) {
        result = InjectedLanguageUtil.findElementInInjected(parent, offset);
      }
    }
    return result;
  }

  @Override
  public void disableValueLookup(@Nonnull Editor editor) {
    ValueLookupManager.DISABLE_VALUE_LOOKUP.set(editor, Boolean.TRUE);
  }

  @Nullable
  public static Editor createEditor(@Nonnull OpenFileDescriptor descriptor) {
    return descriptor.canNavigate() ? FileEditorManager.getInstance(descriptor.getProject()).openTextEditor(descriptor, false) : null;
  }

  public static void rebuildAllSessionsViews(@Nullable Project project) {
    if (project == null) return;
    for (XDebugSession session : XDebuggerManager.getInstance(project).getDebugSessions()) {
      if (session.isSuspended()) {
        session.rebuildViews();
      }
    }
  }

  @Nonnull
  @Override
  public XExpression createExpression(@Nonnull String text, Language language, String custom, EvaluationMode mode) {
    return new XExpressionImpl(text, language, custom, mode);
  }

  public static boolean isEmptyExpression(@Nullable XExpression expression) {
    return expression == null || StringUtil.isEmptyOrSpaces(expression.getExpression());
  }
}
