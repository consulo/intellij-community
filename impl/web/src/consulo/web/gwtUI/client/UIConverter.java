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
package consulo.web.gwtUI.client;

import com.google.gwt.user.client.Window;
import consulo.web.gwtUI.client.ui.GwtCheckBoxImpl;
import consulo.web.gwtUI.client.ui.GwtComponentImpl;
import consulo.web.gwtUI.client.ui.GwtDockPanelImpl;
import consulo.web.gwtUI.shared.UIComponent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author VISTALL
 * @since 11-Jun-16
 */
public class UIConverter {
  interface Factory {
    GwtComponentImpl create();
  }

  private static Map<String, Factory> ourMap = new HashMap<String, Factory>();

  static {
    ourMap.put("consulo.ui.internal.WGwtCheckBoxImpl", new Factory() {
      @Override
      public GwtComponentImpl create() {
        return new GwtCheckBoxImpl();
      }
    });
    ourMap.put("consulo.ui.internal.WGwtDockPanelImpl", new Factory() {
      @Override
      public GwtComponentImpl create() {
        return new GwtDockPanelImpl();
      }
    });
  }

  private static Map<String, GwtComponentImpl> ourCache = new HashMap<String, GwtComponentImpl>();

  public static GwtComponentImpl create(WebSocketProxy proxy, UIComponent component) {
    final String type = component.getType();
    Factory factory = ourMap.get(type);
    if (factory == null) {
      Window.alert("Type " + type + " is not resolved");
      return null;
    }

    final GwtComponentImpl widget = factory.create();

    ourCache.put(component.getId(), widget);

    final Map<String, String> variables = component.getVariables();
    widget.init(proxy, component.getId());

    if(variables != null) {
      widget.updateState(variables);
    }

    final List<UIComponent.Child> children = component.getChildren();
    if(children != null) {
      for (UIComponent.Child child : children) {
        widget.addChildren(proxy, child);
      }
    }
    return widget;
  }

  public static GwtComponentImpl get(String id) {
    return ourCache.get(id);
  }
}
