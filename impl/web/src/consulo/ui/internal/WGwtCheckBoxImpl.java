/*
 * Copyright 2013-2016 must-be.org
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
package consulo.ui.internal;

import com.intellij.util.SmartList;
import consulo.ui.CheckBox;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

/**
 * @author VISTALL
 * @since 11-Jun-16
 */
public class WGwtCheckBoxImpl extends WGwtComponentImpl implements CheckBox {
  private boolean mySelected;
  private String myText;
  private List<SelectListener> mySelectListenerList = new SmartList<SelectListener>();

  public WGwtCheckBoxImpl(boolean selected, String text) {
    mySelected = selected;
    myText = text;
  }

  public String getText() {
    return myText;
  }

  @Override
  protected void initVariables(Map<String, String> map) {
    super.initVariables(map);

    map.put("text", myText);
    map.put("selected", String.valueOf(mySelected));
  }

  @Override
  public void invokeListeners(Map<String, String> variables) {
    mySelected = Boolean.parseBoolean(variables.get("selected"));

    for (SelectListener selectListener : mySelectListenerList) {
      selectListener.selectChanged(this);
    }
  }

  @Override
  public boolean isSelected() {
    return mySelected;
  }

  @Override
  public void setSelected(boolean value) {
    mySelected = value;
  }

  @Override
  public void addSelectListener(@NotNull SelectListener selectListener) {
    mySelectListenerList.add(selectListener);
  }

  @Override
  public void removeSelectListener(@NotNull SelectListener selectListener) {
    mySelectListenerList.remove(selectListener);
  }
}
