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
package consulo.web.gwt.client.ui.image;

import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import consulo.web.gwt.shared.ui.state.image.MultiImageState;
import org.jetbrains.annotations.NotNull;

/**
 * @author VISTALL
 * @since 11-Sep-17
 */
public class ImageConverter {
  @NotNull
  public static Widget create(@NotNull MultiImageState state) {
    Widget widget = null;
    if (state.myImageState != null) {
      Image box = new Image();
      box.setStyleName("ui-image");
      box.setUrl(state.myImageState.myURL);

      widget = box;
    }
    else if (state.myFoldedImageState != null) {
      FlowPanel flowPanel = new FlowPanel();
      flowPanel.setStyleName("ui-layered-image");
      flowPanel.setWidth(state.myWidth + "px");
      flowPanel.setHeight(state.myHeight + "px");

      for (MultiImageState child : state.myFoldedImageState.myChildren) {
        flowPanel.add(create(child));
      }

      widget = flowPanel;
    }

    if (widget == null) {
      return new Label("Unsupported " + state.getClass().getName());
    }

    widget.setWidth(state.myWidth + "pm");
    widget.setWidth(state.myHeight + "pm");
    return widget;
  }
}
