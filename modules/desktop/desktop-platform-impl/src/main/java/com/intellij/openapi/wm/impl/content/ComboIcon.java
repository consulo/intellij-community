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
package com.intellij.openapi.wm.impl.content;

import com.intellij.icons.AllIcons;
import consulo.awt.TargetAWT;
import consulo.ui.image.Image;
import consulo.ui.image.ImageEffects;
import consulo.ui.image.ImageState;

import java.awt.*;

public abstract class ComboIcon {
  private final ImageState<Boolean> myState = new ImageState<>(Boolean.FALSE);

  private final Image myImage;

  public ComboIcon() {
    myImage = Image.stated(myState, active -> active ? AllIcons.General.Combo2 : ImageEffects.grayed(AllIcons.General.Combo2));
  }

  public void paintIcon(final Component c, final Graphics g) {
    myState.setState(isActive());

    final Rectangle moreRect = getIconRec();

    if (moreRect == null) return;

    int iconY = getIconY(moreRect);
    int iconX = getIconX(moreRect);

    TargetAWT.to(myImage).paintIcon(c, g, iconX, iconY);
  }

  protected int getIconX(final Rectangle iconRec) {
    return iconRec.x + iconRec.width / 2 - getIconWidth() / 2;
  }

  public int getIconWidth() {
    return myImage.getWidth();
  }

  protected int getIconY(final Rectangle iconRec) {
    return iconRec.y + iconRec.height / 2 - getIconHeight() / 2 + 1;
  }

  public int getIconHeight() {
    return myImage.getHeight();
  }

  public abstract Rectangle getIconRec();

  public abstract boolean isActive();
}
