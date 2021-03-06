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
package consulo.ui.desktop.internal;

import consulo.awt.impl.FromSwingComponentWrapper;
import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.ui.Button;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.desktop.internal.base.SwingComponentDelegate;
import consulo.ui.event.ClickEvent;
import consulo.ui.event.ClickListener;

import javax.annotation.Nonnull;
import javax.swing.*;

/**
 * @author VISTALL
 * @since 13-Sep-17
 */
class DesktopButtonImpl extends SwingComponentDelegate<JButton> implements Button {
  class MyButton extends JButton implements FromSwingComponentWrapper {
    private final LocalizeValue myTextValue;

    MyButton(LocalizeValue textValue) {
      super("");
      myTextValue = textValue;

      updateText();
    }

    @Override
    public void updateUI() {
      super.updateUI();

      updateText();
    }

    @Nonnull
    @Override
    public Component toUIComponent() {
      return DesktopButtonImpl.this;
    }

    private void updateText() {
      // first call from super constructor
      if (myTextValue == null) {
        return;
      }

      setText(myTextValue.getValue());
    }
  }

  public DesktopButtonImpl(LocalizeValue text) {
    initialize(new MyButton(text));

    toAWTComponent().addActionListener(e -> getListenerDispatcher(ClickListener.class).clicked(new ClickEvent(this)));
  }

  @Override
  public Disposable addClickListener(@Nonnull ClickListener clickListener) {
    return addListener(ClickListener.class, clickListener);
  }

  @Nonnull
  @Override
  public String getText() {
    return toAWTComponent().getText();
  }

  @RequiredUIAccess
  @Override
  public void setText(@Nonnull String text) {
    toAWTComponent().setText(text);
  }
}
