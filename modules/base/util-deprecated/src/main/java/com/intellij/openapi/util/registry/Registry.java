/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package com.intellij.openapi.util.registry;

import org.jdom.Element;
import org.jetbrains.annotations.NonNls;
import javax.annotation.Nonnull;
import org.jetbrains.annotations.PropertyKey;
import consulo.annotation.DeprecationInfo;

import java.awt.*;
import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Deprecated
@DeprecationInfo(value = "Use `org.consulo.ide.eap.EarlyAccessProgramDescriptor` or `com.intellij.ide.util.PropertiesComponent`", until = "4.0")
public class Registry  {
  private static Reference<ResourceBundle> ourBundle;

  @NonNls
  public static final String REGISTRY_BUNDLE = "misc.registry";

  private final Map<String, String> myUserProperties = new LinkedHashMap<String, String>();
  private final Map<String, String> myLoadedUserProperties = new HashMap<String, String>();
  private final Map<String, RegistryValue> myValues = new ConcurrentHashMap<String, RegistryValue>();

  private static final Registry ourInstance = new Registry();

  @Nonnull
  public static RegistryValue get(@PropertyKey(resourceBundle = REGISTRY_BUNDLE) @Nonnull String key) {
    final Registry registry = getInstance();

    RegistryValue value = registry.myValues.get(key);
    if (value == null) {
      value = new RegistryValue(registry, key);
      registry.myValues.put(key, value);
    }
    return value;
  }

  public static boolean is(@PropertyKey(resourceBundle = REGISTRY_BUNDLE) @Nonnull String key) throws MissingResourceException {
    return get(key).asBoolean();
  }

  public static boolean is(@PropertyKey(resourceBundle = REGISTRY_BUNDLE) @Nonnull String key, boolean defaultValue) {
    try {
      return get(key).asBoolean();
    }
    catch (MissingResourceException ex) {
      return defaultValue;
    }
  }

  public static int intValue(@PropertyKey(resourceBundle = REGISTRY_BUNDLE) @Nonnull String key) throws MissingResourceException {
    return get(key).asInteger();
  }

  public static int intValue(@PropertyKey(resourceBundle = REGISTRY_BUNDLE) @Nonnull String key, int defaultValue) {
    try {
      return get(key).asInteger();
    }
    catch (MissingResourceException ex) {
      return defaultValue;
    }
  }

  public static double doubleValue(@PropertyKey(resourceBundle = REGISTRY_BUNDLE) @Nonnull String key) throws MissingResourceException {
    return get(key).asDouble();
  }

  @Nonnull
  public static String stringValue(@PropertyKey(resourceBundle = REGISTRY_BUNDLE) @Nonnull String key) throws MissingResourceException {
    return get(key).asString();
  }

  public static Color getColor(@PropertyKey(resourceBundle = REGISTRY_BUNDLE) @Nonnull String key, Color defaultValue) throws MissingResourceException {
    return get(key).asColor(defaultValue);
  }

  @Nonnull
  static ResourceBundle getBundle() {
    ResourceBundle bundle = com.intellij.reference.SoftReference.dereference(ourBundle);
    if (bundle == null) {
      bundle = ResourceBundle.getBundle(REGISTRY_BUNDLE);
      ourBundle = new SoftReference<ResourceBundle>(bundle);
    }
    return bundle;
  }


  public static Registry getInstance() {
    return ourInstance;
  }

  @Nonnull
  public Element getState() {
    final Element state = new Element("registry");
    for (String eachKey : myUserProperties.keySet()) {
      final Element entry = new Element("entry");
      entry.setAttribute("key", eachKey);
      entry.setAttribute("value", myUserProperties.get(eachKey));
      state.addContent(entry);
    }
    return state;
  }

  public void loadState(@Nonnull Element state) {
    final List entries = state.getChildren("entry");
    for (Object each : entries) {
      final Element eachEntry = (Element) each;
      final String eachKey = eachEntry.getAttributeValue("key");
      final String eachValue = eachEntry.getAttributeValue("value");
      if (eachKey != null && eachValue != null) {
        myUserProperties.put(eachKey, eachValue);
      }
    }
    myLoadedUserProperties.putAll(myUserProperties);

    for (RegistryValue each : myValues.values()) {
      each.resetCache();
    }
  }

  @Nonnull
  Map<String, String> getUserProperties() {
    return myUserProperties;
  }

  @Nonnull
  public static List<RegistryValue> getAll() {
    final ResourceBundle bundle = getBundle();
    final Enumeration<String> keys = bundle.getKeys();

    List<RegistryValue> result = new ArrayList<RegistryValue>();

    while (keys.hasMoreElements()) {
      final String each = keys.nextElement();
      if (each.endsWith(".description") || each.endsWith(".restartRequired")) continue;
      result.add(get(each));
    }

    return result;
  }

  public void restoreDefaults() {
    Map<String, String> old = new HashMap<String, String>();
    old.putAll(myUserProperties);
    for (String each : old.keySet()) {
      get(each).resetToDefault();
    }
  }

  public boolean isInDefaultState() {
    return getUserProperties().isEmpty();
  }

  public boolean isRestartNeeded() {
    return isRestartNeeded(myUserProperties) || isRestartNeeded(myLoadedUserProperties);
  }

  private static boolean isRestartNeeded(@Nonnull Map<String, String> map) {
    for (String s : map.keySet()) {
      final RegistryValue eachValue = get(s);
      if (eachValue.isRestartRequired() && eachValue.isChangedSinceAppStart()) return true;
    }

    return false;
  }
}
