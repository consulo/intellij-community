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

/**
 * @author Yura Cangea
 */
package com.intellij.openapi.editor.colors.impl;

import com.intellij.ide.ui.UISettings;
import com.intellij.openapi.editor.HighlighterColors;
import com.intellij.openapi.editor.colors.*;
import com.intellij.openapi.editor.markup.EffectType;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.options.FontSize;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.WriteExternalException;
import com.intellij.util.containers.ContainerUtilRt;
import com.intellij.util.containers.HashMap;
import com.intellij.util.ui.JBUI;
import gnu.trove.THashMap;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.*;
import java.util.List;

public abstract class AbstractColorsScheme implements EditorColorsScheme {
  private static final String OS_VALUE_PREFIX = SystemInfo.isWindows ? "windows" : SystemInfo.isMac ? "mac" : "linux";
  private static final int CURR_VERSION = 124;

  private static final FontSize DEFAULT_FONT_SIZE = FontSize.SMALL;

  protected EditorColorsScheme myParentScheme;
  protected final EditorColorsManager myEditorColorsManager;

  protected FontSize myQuickDocFontSize = DEFAULT_FONT_SIZE;
  protected float myLineSpacing;

  @NotNull private final Map<EditorFontType, Font> myFonts                  = new EnumMap<EditorFontType, Font>(EditorFontType.class);
  @NotNull private final FontPreferences           myFontPreferences        = new FontPreferences();
  @NotNull private final FontPreferences           myConsoleFontPreferences = new FontPreferences();

  private final ValueElementReader myValueReader = new TextAttributesReader();
  private String myFallbackFontName;
  private String mySchemeName;

  private float myConsoleLineSpacing = -1;

  // version influences XML format and triggers migration
  private int myVersion = CURR_VERSION;

  protected Map<ColorKey, Color>                   myColorsMap     = ContainerUtilRt.newHashMap();
  protected Map<TextAttributesKey, TextAttributes> myAttributesMap = ContainerUtilRt.newHashMap();

  @NonNls private static final String EDITOR_FONT       = "font";
  @NonNls private static final String CONSOLE_FONT      = "console-font";
  @NonNls private static final String EDITOR_FONT_NAME  = "EDITOR_FONT_NAME";
  @NonNls private static final String CONSOLE_FONT_NAME = "CONSOLE_FONT_NAME";
  private                      Color  myDeprecatedBackgroundColor    = null;
  @NonNls private static final String SCHEME_ELEMENT                 = "scheme";
  @NonNls public static final  String NAME_ATTR                      = "name";
  @NonNls private static final String VERSION_ATTR                   = "version";
  @NonNls private static final String DEFAULT_SCHEME_ATTR            = "default_scheme";
  @NonNls private static final String PARENT_SCHEME_ATTR             = "parent_scheme";
  @NonNls private static final String OPTION_ELEMENT                 = "option";
  @NonNls private static final String COLORS_ELEMENT                 = "colors";
  @NonNls private static final String ATTRIBUTES_ELEMENT             = "attributes";
  @NonNls private static final String VALUE_ELEMENT                  = "value";
  @NonNls private static final String BACKGROUND_COLOR_NAME          = "BACKGROUND";
  @NonNls private static final String LINE_SPACING                   = "LINE_SPACING";
  @NonNls private static final String CONSOLE_LINE_SPACING           = "CONSOLE_LINE_SPACING";
  @NonNls private static final String EDITOR_FONT_SIZE               = "EDITOR_FONT_SIZE";
  @NonNls private static final String CONSOLE_FONT_SIZE              = "CONSOLE_FONT_SIZE";
  @NonNls private static final String EDITOR_LIGATURES               = "EDITOR_LIGATURES";
  @NonNls private static final String CONSOLE_LIGATURES              = "CONSOLE_LIGATURES";
  @NonNls private static final String EDITOR_QUICK_JAVADOC_FONT_SIZE = "EDITOR_QUICK_DOC_FONT_SIZE";

  protected AbstractColorsScheme(@Nullable EditorColorsScheme parentScheme, @NotNull EditorColorsManager editorColorsManager) {
    myParentScheme = parentScheme;
    myEditorColorsManager = editorColorsManager;
    myFontPreferences.setChangeListener(new Runnable() {
      @Override
      public void run() {
        initFonts();
      }
    });
  }

  @NotNull
  @Override
  public Color getDefaultBackground() {
    final Color c = getAttributes(HighlighterColors.TEXT).getBackgroundColor();
    return c != null ? c : Color.white;
  }

  @NotNull
  @Override
  public Color getDefaultForeground() {
    final Color c = getAttributes(HighlighterColors.TEXT).getForegroundColor();
    return c != null ? c : Color.black;
  }

  @NotNull
  @Override
  public String getName() {
    return mySchemeName;
  }

  @Override
  public void setFont(EditorFontType key, Font font) {
    myFonts.put(key, font);
  }

  @Override
  public abstract EditorColorsScheme clone();

  public void copyTo(AbstractColorsScheme newScheme) {
    myFontPreferences.copyTo(newScheme.myFontPreferences);
    newScheme.myLineSpacing = myLineSpacing;
    newScheme.myQuickDocFontSize = myQuickDocFontSize;
    myConsoleFontPreferences.copyTo(newScheme.myConsoleFontPreferences);
    newScheme.myConsoleLineSpacing = myConsoleLineSpacing;

    final Set<EditorFontType> types = myFonts.keySet();
    for (EditorFontType type : types) {
      Font font = myFonts.get(type);
      newScheme.setFont(type, font);
    }

    newScheme.myAttributesMap = new HashMap<TextAttributesKey, TextAttributes>(myAttributesMap);
    newScheme.myColorsMap = new HashMap<ColorKey, Color>(myColorsMap);
    newScheme.myVersion = myVersion;
  }

  @Override
  public void setEditorFontName(String fontName) {
    int editorFontSize = getEditorFontSizeInternal();
    myFontPreferences.clear();
    myFontPreferences.register(fontName, editorFontSize);
    initFonts();
  }

  @Override
  public void setEditorFontSize(int fontSize) {
    myFontPreferences.register(getEditorFontName(), fontSize);
    initFonts();
  }

  @Override
  public void setQuickDocFontSize(@NotNull FontSize fontSize) {
    myQuickDocFontSize = fontSize;
  }

  @Override
  public void setLineSpacing(float lineSpacing) {
    myLineSpacing = lineSpacing;
  }

  @Override
  public Font getFont(EditorFontType key) {
    if (UISettings.getInstance().PRESENTATION_MODE) {
      final Font font = myFonts.get(key);
      return new Font(font.getName(), font.getStyle(), UISettings.getInstance().PRESENTATION_MODE_FONT_SIZE);
    }
    return myFonts.get(key);
  }

  @Override
  public void setName(@NotNull String name) {
    mySchemeName = name;
  }

  @NotNull
  @Override
  public FontPreferences getFontPreferences() {
    return myFontPreferences;
  }

  @Override
  public void setFontPreferences(@NotNull FontPreferences preferences) {
    preferences.copyTo(myFontPreferences);
    initFonts();
  }

  @Override
  public String getEditorFontName() {
    if (myFallbackFontName != null) {
      return myFallbackFontName;
    }
    return myFontPreferences.getFontFamily();
  }

  @Override
  public int getEditorFontSize() {
    return getEditorFontSize(true);
  }

  @Override
  public int getEditorFontSize(boolean scale) {
    int editorFontSizeInternal = getEditorFontSizeInternal();
    return scale ? JBUI.scale(editorFontSizeInternal) : editorFontSizeInternal;
  }

  public int getEditorFontSizeInternal() {
    return myFontPreferences.getSize(getEditorFontName());
  }

  @NotNull
  @Override
  public FontSize getQuickDocFontSize() {
    return myQuickDocFontSize;
  }

  @Override
  public float getLineSpacing() {
    float spacing = myLineSpacing;
    return spacing <= 0 ? 1.0f : spacing;
  }

  protected void initFonts() {
    String editorFontName = getEditorFontName();
    int editorFontSize = getEditorFontSizeInternal();

    myFallbackFontName = FontPreferences.getFallbackName(editorFontName, editorFontSize, myParentScheme);
    if (myFallbackFontName != null) {
      editorFontName = myFallbackFontName;
    }
    Font plainFont = new Font(editorFontName, Font.PLAIN, editorFontSize);
    Font boldFont = new Font(editorFontName, Font.BOLD, editorFontSize);
    Font italicFont = new Font(editorFontName, Font.ITALIC, editorFontSize);
    Font boldItalicFont = new Font(editorFontName, Font.BOLD | Font.ITALIC, editorFontSize);

    myFonts.put(EditorFontType.PLAIN, plainFont);
    myFonts.put(EditorFontType.BOLD, boldFont);
    myFonts.put(EditorFontType.ITALIC, italicFont);
    myFonts.put(EditorFontType.BOLD_ITALIC, boldItalicFont);

    String consoleFontName = getConsoleFontName();
    int consoleFontSize = getConsoleFontSizeInternal();

    Font consolePlainFont = new Font(consoleFontName, Font.PLAIN, consoleFontSize);
    Font consoleBoldFont = new Font(consoleFontName, Font.BOLD, consoleFontSize);
    Font consoleItalicFont = new Font(consoleFontName, Font.ITALIC, consoleFontSize);
    Font consoleBoldItalicFont = new Font(consoleFontName, Font.BOLD | Font.ITALIC, consoleFontSize);

    myFonts.put(EditorFontType.CONSOLE_PLAIN, consolePlainFont);
    myFonts.put(EditorFontType.CONSOLE_BOLD, consoleBoldFont);
    myFonts.put(EditorFontType.CONSOLE_ITALIC, consoleItalicFont);
    myFonts.put(EditorFontType.CONSOLE_BOLD_ITALIC, consoleBoldItalicFont);
  }

  public String toString() {
    return getName();
  }

  @Override
  public void readExternal(Element parentNode) {
    if (SCHEME_ELEMENT.equals(parentNode.getName())) {
      readScheme(parentNode);
    }
    else {
      for (Element element : parentNode.getChildren(SCHEME_ELEMENT)) {
        readScheme(element);
      }
    }
    initFonts();
    myVersion = CURR_VERSION;
  }

  private void readScheme(Element node) {
    myDeprecatedBackgroundColor = null;
    if (!SCHEME_ELEMENT.equals(node.getName())) {
      return;
    }

    setName(node.getAttributeValue(NAME_ATTR));
    int readVersion = Integer.parseInt(node.getAttributeValue(VERSION_ATTR, "0"));
    if (readVersion > CURR_VERSION) {
      throw new IllegalStateException("Unsupported color scheme version: " + readVersion);
    }

    myVersion = readVersion;
    String isDefaultScheme = node.getAttributeValue(DEFAULT_SCHEME_ATTR);
    if (isDefaultScheme == null || !Boolean.parseBoolean(isDefaultScheme)) {
      String defaultAttributeValue = node.getAttributeValue(PARENT_SCHEME_ATTR, DEFAULT_SCHEME_NAME);
      myParentScheme = myEditorColorsManager.getBundledSchemes().get(defaultAttributeValue);
    }

    for (final Element childNode : node.getChildren()) {
      String childName = childNode.getName();
      if (OPTION_ELEMENT.equals(childName)) {
        readSettings(childNode);
      }
      else if (EDITOR_FONT.equals(childName)) {
        readFontSettings(childNode, myFontPreferences);
      }
      else if (CONSOLE_FONT.equals(childName)) {
        readFontSettings(childNode, myConsoleFontPreferences);
      }
      else if (COLORS_ELEMENT.equals(childName)) {
        readColors(childNode);
      }
      else if (ATTRIBUTES_ELEMENT.equals(childName)) {
        readAttributes(childNode);
      }
    }

    if (myDeprecatedBackgroundColor != null) {
      TextAttributes textAttributes = myAttributesMap.get(HighlighterColors.TEXT);
      if (textAttributes == null) {
        textAttributes = new TextAttributes(Color.black, myDeprecatedBackgroundColor, null, EffectType.BOXED, Font.PLAIN);
        myAttributesMap.put(HighlighterColors.TEXT, textAttributes);
      }
      else {
        textAttributes.setBackgroundColor(myDeprecatedBackgroundColor);
      }
    }

    if (myConsoleFontPreferences.getEffectiveFontFamilies().isEmpty()) {
      myFontPreferences.copyTo(myConsoleFontPreferences);
    }

    initFonts();
  }

  protected void readAttributes(@NotNull Element childNode) {
    for (Element e : childNode.getChildren(OPTION_ELEMENT)) {
      TextAttributesKey name = TextAttributesKey.find(e.getAttributeValue(NAME_ATTR));
      TextAttributes attr = new TextAttributes(e.getChild(VALUE_ELEMENT));
      myAttributesMap.put(name, attr);
      migrateErrorStripeColorFrom45(name, attr);
    }
  }

  private void migrateErrorStripeColorFrom45(final TextAttributesKey name, final TextAttributes attr) {
    if (myVersion != 0) return;
    Color defaultColor = DEFAULT_ERROR_STRIPE_COLOR.get(name.getExternalName());
    if (defaultColor != null && attr.getErrorStripeColor() == null) {
      attr.setErrorStripeColor(defaultColor);
    }
  }
  public static final Map<String, Color> DEFAULT_ERROR_STRIPE_COLOR = new THashMap<String, Color>();
  static {
    DEFAULT_ERROR_STRIPE_COLOR.put(CodeInsightColors.ERRORS_ATTRIBUTES.getExternalName(), Color.red);
    DEFAULT_ERROR_STRIPE_COLOR.put(CodeInsightColors.WRONG_REFERENCES_ATTRIBUTES.getExternalName(), Color.red);
    DEFAULT_ERROR_STRIPE_COLOR.put(CodeInsightColors.WARNINGS_ATTRIBUTES.getExternalName(), Color.yellow);
    DEFAULT_ERROR_STRIPE_COLOR.put(CodeInsightColors.INFO_ATTRIBUTES.getExternalName(), Color.yellow.brighter());
    DEFAULT_ERROR_STRIPE_COLOR.put(CodeInsightColors.WEAK_WARNING_ATTRIBUTES.getExternalName(), Color.yellow.brighter());
    DEFAULT_ERROR_STRIPE_COLOR.put(CodeInsightColors.NOT_USED_ELEMENT_ATTRIBUTES.getExternalName(), Color.yellow);
    DEFAULT_ERROR_STRIPE_COLOR.put(CodeInsightColors.NOT_USED_ELEMENT_ATTRIBUTES.getExternalName(), Color.yellow);
    DEFAULT_ERROR_STRIPE_COLOR.put(CodeInsightColors.DEPRECATED_ATTRIBUTES.getExternalName(), Color.yellow);
  }

  private void readColors(Element childNode) {
    for (final Object o : childNode.getChildren(OPTION_ELEMENT)) {
      Element colorElement = (Element)o;
      Color valueColor = readColorValue(colorElement);
      final String colorName = colorElement.getAttributeValue(NAME_ATTR);
      if (BACKGROUND_COLOR_NAME.equals(colorName)) {
        // This setting has been deprecated to usages of HighlighterColors.TEXT attributes.
        myDeprecatedBackgroundColor = valueColor;
      }

      ColorKey name = ColorKey.find(colorName);
      myColorsMap.put(name, valueColor);
    }
  }

  private static Color readColorValue(final Element colorElement) {
    String value = getValue(colorElement);
    Color valueColor = null;
    if (value != null && value.trim().length() > 0) {
      try {
        valueColor = new Color(Integer.parseInt(value, 16));
      }
      catch (NumberFormatException ignored) {
      }
    }
    return valueColor;
  }

  private void readSettings(Element childNode) {
    String name = childNode.getAttributeValue(NAME_ATTR);
    if (LINE_SPACING.equals(name)) {
      Float value = myValueReader.read(Float.class, childNode);
      if (value != null) myLineSpacing = value;
    }
    else if (EDITOR_FONT_SIZE.equals(name)) {
      Integer value = myValueReader.read(Integer.class, childNode);
      if (value  != null) setEditorFontSize(value);
    }
    else if (EDITOR_FONT_NAME.equals(name)) {
      String value = myValueReader.read(String.class, childNode);
      if (value != null) setEditorFontName(value);
    }
    else if (CONSOLE_LINE_SPACING.equals(name)) {
      Float value = myValueReader.read(Float.class, childNode);
      if (value != null) setConsoleLineSpacing(value);
    }
    else if (CONSOLE_FONT_SIZE.equals(name)) {
      Integer value = myValueReader.read(Integer.class, childNode);
      if (value != null) setConsoleFontSize(value);
    }
    else if (CONSOLE_FONT_NAME.equals(name)) {
      String value = myValueReader.read(String.class, childNode);
      if (value != null) setConsoleFontName(value);
    }
    else if (EDITOR_QUICK_JAVADOC_FONT_SIZE.equals(name)) {
      FontSize value = myValueReader.read(FontSize.class, childNode);
      if (value != null) myQuickDocFontSize = value;
    }
    else if (EDITOR_LIGATURES.equals(name)) {
      Boolean value = myValueReader.read(Boolean.class, childNode);
      if (value != null) myFontPreferences.setUseLigatures(value);
    }
    else if (CONSOLE_LIGATURES.equals(name)) {
      Boolean value = myValueReader.read(Boolean.class, childNode);
      if (value != null) myConsoleFontPreferences.setUseLigatures(value);
    }
  }

  private static void readFontSettings(@NotNull Element element, @NotNull FontPreferences preferences) {
    List children = element.getChildren(OPTION_ELEMENT);
    String fontFamily = null;
    int size = -1;
    for (Object child : children) {
      Element e = (Element)child;
      if (EDITOR_FONT_NAME.equals(e.getAttributeValue(NAME_ATTR))) {
        fontFamily = getValue(e);
      }
      else if (EDITOR_FONT_SIZE.equals(e.getAttributeValue(NAME_ATTR))) {
        try {
          size = Integer.parseInt(getValue(e));
        }
        catch (NumberFormatException ex) {
          // ignore
        }
      }
    }
    if (fontFamily != null && size > 1) {
      preferences.register(fontFamily, size);
    }
    else if (fontFamily != null) {
      preferences.addFontFamily(fontFamily);
    }
  }

  private static String getValue(Element e) {
    final String value = e.getAttributeValue(OS_VALUE_PREFIX);
    return value == null ? e.getAttributeValue(VALUE_ELEMENT) : value;
  }

  public void writeExternal(Element parentNode) throws WriteExternalException {
    parentNode.setAttribute(NAME_ATTR, getName());
    parentNode.setAttribute(VERSION_ATTR, Integer.toString(myVersion));

    if (myParentScheme != null) {
      parentNode.setAttribute(PARENT_SCHEME_ATTR, myParentScheme.getName());
    }

    Element element = new Element(OPTION_ELEMENT);
    element.setAttribute(NAME_ATTR, LINE_SPACING);
    element.setAttribute(VALUE_ELEMENT, String.valueOf(getLineSpacing()));
    parentNode.addContent(element);

    // IJ has used a 'single customizable font' mode for ages. That's why we want to support that format now, when it's possible
    // to specify fonts sequence (see getFontPreferences()), there are big chances that many clients still will use a single font.
    // That's why we want to use old format when zero or one font is selected and 'extended' format otherwise.
    boolean useOldFontFormat = myFontPreferences.getEffectiveFontFamilies().size() <= 1;
    if (useOldFontFormat) {
      element = new Element(OPTION_ELEMENT);
      element.setAttribute(NAME_ATTR, EDITOR_FONT_SIZE);
      element.setAttribute(VALUE_ELEMENT, String.valueOf(getEditorFontSizeInternal()));
      parentNode.addContent(element);
    }
    else {
      writeFontPreferences(EDITOR_FONT, parentNode, myFontPreferences);
    }

    writeLigaturesPreferences(parentNode, myFontPreferences, EDITOR_LIGATURES);

    if (!myFontPreferences.equals(myConsoleFontPreferences)) {
      if (myConsoleFontPreferences.getEffectiveFontFamilies().size() <= 1) {
        element = new Element(OPTION_ELEMENT);
        element.setAttribute(NAME_ATTR, CONSOLE_FONT_NAME);
        element.setAttribute(VALUE_ELEMENT, getConsoleFontName());
        parentNode.addContent(element);

        if (getConsoleFontSizeInternal() != getEditorFontSizeInternal()) {
          element = new Element(OPTION_ELEMENT);
          element.setAttribute(NAME_ATTR, CONSOLE_FONT_SIZE);
          element.setAttribute(VALUE_ELEMENT, Integer.toString(getConsoleFontSizeInternal()));
          parentNode.addContent(element);
        }
      }
      else {
        writeFontPreferences(CONSOLE_FONT, parentNode, myConsoleFontPreferences);
      }
      writeLigaturesPreferences(parentNode, myConsoleFontPreferences, CONSOLE_LIGATURES);
    }

    if (getConsoleLineSpacing() != getLineSpacing()) {
      element = new Element(OPTION_ELEMENT);
      element.setAttribute(NAME_ATTR, CONSOLE_LINE_SPACING);
      element.setAttribute(VALUE_ELEMENT, Float.toString(getConsoleLineSpacing()));
      parentNode.addContent(element);
    }

    if (DEFAULT_FONT_SIZE != getQuickDocFontSize()) {
      element = new Element(OPTION_ELEMENT);
      element.setAttribute(NAME_ATTR, EDITOR_QUICK_JAVADOC_FONT_SIZE);
      element.setAttribute(VALUE_ELEMENT, getQuickDocFontSize().toString());
      parentNode.addContent(element);
    }

    if (useOldFontFormat) {
      element = new Element(OPTION_ELEMENT);
      element.setAttribute(NAME_ATTR, EDITOR_FONT_NAME);
      element.setAttribute(VALUE_ELEMENT, getEditorFontName());
      parentNode.addContent(element);
    }

    Element colorElements = new Element(COLORS_ELEMENT);
    Element attrElements = new Element(ATTRIBUTES_ELEMENT);

    writeColors(colorElements);
    writeAttributes(attrElements);

    if (colorElements.getChildren().size() > 0) {
      parentNode.addContent(colorElements);
    }
    if (attrElements.getChildren().size() > 0) {
      parentNode.addContent(attrElements);
    }
  }

  private static void writeLigaturesPreferences(Element parentNode, FontPreferences preferences, String optionName) {
    if (preferences.useLigatures()) {
      Element element = new Element(OPTION_ELEMENT);
      element.setAttribute(NAME_ATTR, optionName);
      element.setAttribute(VALUE_ELEMENT, String.valueOf(true));
      parentNode.addContent(element);
    }
  }


  private static void writeFontPreferences(@NotNull String key, @NotNull Element parent, @NotNull FontPreferences preferences) {
    for (String fontFamily : preferences.getRealFontFamilies()) {
      Element element = new Element(key);
      Element e = new Element(OPTION_ELEMENT);
      e.setAttribute(NAME_ATTR, EDITOR_FONT_NAME);
      e.setAttribute(VALUE_ELEMENT, fontFamily);
      element.addContent(e);

      e = new Element(OPTION_ELEMENT);
      e.setAttribute(NAME_ATTR, EDITOR_FONT_SIZE);
      e.setAttribute(VALUE_ELEMENT, String.valueOf(preferences.getSize(fontFamily)));
      element.addContent(e);

      parent.addContent(element);
    }
  }

  private static boolean haveToWrite(final TextAttributesKey key, final TextAttributes value, final TextAttributes defaultAttribute) {
    return !(key.getFallbackAttributeKey() != null && value.isFallbackEnabled()) && !value.equals(defaultAttribute);
  }

  private void writeAttributes(Element attrElements) throws WriteExternalException {
    List<TextAttributesKey> list = new ArrayList<TextAttributesKey>(myAttributesMap.keySet());
    Collections.sort(list);

    for (TextAttributesKey key: list) {
      TextAttributes defaultAttr = myParentScheme != null ? myParentScheme.getAttributes(key) : new TextAttributes();
      TextAttributes value = myAttributesMap.get(key);
      if (!haveToWrite(key,value,defaultAttr)) continue;
      Element element = new Element(OPTION_ELEMENT);
      element.setAttribute(NAME_ATTR, key.getExternalName());
      Element valueElement = new Element(VALUE_ELEMENT);
      value.writeExternal(valueElement);
      element.addContent(valueElement);
      attrElements.addContent(element);
    }
  }

  protected Color getOwnColor(ColorKey key) {
    return myColorsMap.get(key);
  }

  private void writeColors(Element colorElements) {
    List<ColorKey> list = new ArrayList<ColorKey>(myColorsMap.keySet());
    Collections.sort(list);

    for (ColorKey key : list) {
      if (haveToWrite(key)) {
        Color value = myColorsMap.get(key);
        Element element = new Element(OPTION_ELEMENT);
        element.setAttribute(NAME_ATTR, key.getExternalName());
        element.setAttribute(VALUE_ELEMENT, value != null ? Integer.toString(value.getRGB() & 0xFFFFFF, 16) : "");
        colorElements.addContent(element);
      }
    }
  }

  private boolean haveToWrite(final ColorKey key) {
    Color value = myColorsMap.get(key);
    if (myParentScheme != null) {
      if (myParentScheme instanceof AbstractColorsScheme) {
        if (Comparing.equal(((AbstractColorsScheme)myParentScheme).getOwnColor(key), value) && ((AbstractColorsScheme)myParentScheme).myColorsMap.containsKey(key)) {
          return false;
        }
      }
      else {
        if (Comparing.equal((myParentScheme).getColor(key), value)) {
          return false;
        }
      }
    }
    return true;

  }

  @NotNull
  @Override
  public FontPreferences getConsoleFontPreferences() {
    return myConsoleFontPreferences;
  }

  @Override
  public void setConsoleFontPreferences(@NotNull FontPreferences preferences) {
    preferences.copyTo(myConsoleFontPreferences);
    initFonts();
  }

  @Override
  public String getConsoleFontName() {
    return myConsoleFontPreferences.getFontFamily();
  }

  @Override
  public void setConsoleFontName(String fontName) {
    int consoleFontSize = getConsoleFontSizeInternal();
    myConsoleFontPreferences.clear();
    myConsoleFontPreferences.register(fontName, consoleFontSize);
  }

  @Override
  public int getConsoleFontSize() {
    return getConsoleFontSize(true);
  }

  @Override
  public int getConsoleFontSize(boolean scale) {
    int consoleFontSizeInternal = getConsoleFontSizeInternal();
    return scale ? JBUI.scale(consoleFontSizeInternal) : consoleFontSizeInternal;
  }

  public int getConsoleFontSizeInternal() {
    String font = getConsoleFontName();
    UISettings uiSettings = UISettings.getInstance();
    if ((uiSettings == null || !uiSettings.PRESENTATION_MODE) && myConsoleFontPreferences.hasSize(font)) {
      return myConsoleFontPreferences.getSize(font);
    }
    return getEditorFontSizeInternal();
  }

  @Override
  public void setConsoleFontSize(int fontSize) {
    myConsoleFontPreferences.register(getConsoleFontName(), fontSize);
    initFonts();
  }

  @Override
  public float getConsoleLineSpacing() {
    float consoleLineSpacing = myConsoleLineSpacing;
    if (consoleLineSpacing == -1) {
      return getLineSpacing();
    }
    return consoleLineSpacing;
  }

  @Override
  public void setConsoleLineSpacing(float lineSpacing) {
    myConsoleLineSpacing = lineSpacing;
  }

  protected TextAttributes getFallbackAttributes(TextAttributesKey fallbackKey) {
    if (fallbackKey == null) return null;
    if (myAttributesMap.containsKey(fallbackKey)) {
      TextAttributes fallbackAttributes = myAttributesMap.get(fallbackKey);
      if (fallbackAttributes != null && (!fallbackAttributes.isFallbackEnabled() || fallbackKey.getFallbackAttributeKey() == null)) {
        return fallbackAttributes;
      }
    }
    return getFallbackAttributes(fallbackKey.getFallbackAttributeKey());
  }

}
