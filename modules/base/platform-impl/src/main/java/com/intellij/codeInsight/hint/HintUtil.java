// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.hint;

import com.intellij.icons.AllIcons;
import com.intellij.ide.IdeTooltipManager;
import com.intellij.openapi.editor.colors.EditorColorKey;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.util.Ref;
import com.intellij.ui.*;
import com.intellij.util.Consumer;
import com.intellij.util.ui.Html;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import consulo.awt.TargetAWT;
import consulo.ui.color.ColorValue;
import consulo.ui.color.RGBColor;
import consulo.ui.ex.util.LightDarkColorValue;
import consulo.ui.image.Image;
import org.intellij.lang.annotations.JdkConstants;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.event.HyperlinkListener;
import java.awt.*;
import java.awt.event.MouseListener;

import static com.intellij.openapi.editor.colors.EditorColorsUtil.getGlobalOrDefaultColor;
import static com.intellij.util.ObjectUtils.notNull;

public class HintUtil {
  /**
   * @deprecated use getInformationColor()
   */
  @Deprecated
  public static final Color INFORMATION_COLOR = new JBColor(0xF7F7F7, 0x4B4D4D);
  public static final Color INFORMATION_BORDER_COLOR = JBColor.namedColor("InformationHint.borderColor", new JBColor(0xE0E0E0, 0x5C5E61));
  /**
   * @deprecated use getErrorColor()
   */
  @Deprecated
  public static final Color ERROR_COLOR = new JBColor(0xffdcdc, 0x781732);

  public static final EditorColorKey INFORMATION_COLOR_KEY = EditorColorKey.createColorKey("INFORMATION_HINT", new LightDarkColorValue(new RGBColor(247, 247, 247), new RGBColor(75, 77, 77)));
  public static final EditorColorKey QUESTION_COLOR_KEY = EditorColorKey.createColorKey("QUESTION_HINT", new LightDarkColorValue(new RGBColor(181, 208, 251), new RGBColor(55, 108, 137)));
  public static final EditorColorKey ERROR_COLOR_KEY = EditorColorKey.createColorKey("ERROR_HINT", new LightDarkColorValue(new RGBColor(255, 220, 220), new RGBColor(120, 23, 50)));

  public static final Color QUESTION_UNDERSCORE_COLOR = JBColor.foreground();

  public static final EditorColorKey RECENT_LOCATIONS_SELECTION_KEY =
          EditorColorKey.createColorKey("RECENT_LOCATIONS_SELECTION", new LightDarkColorValue(new RGBColor(233, 238, 245), new RGBColor(56, 56, 56)));

  private HintUtil() {
  }

  @Nonnull
  public static ColorValue getInformationColor() {
    return notNull(getGlobalOrDefaultColor(INFORMATION_COLOR_KEY), INFORMATION_COLOR_KEY.getDefaultColorValue());
  }

  @Nonnull
  public static ColorValue getQuestionColor() {
    return notNull(getGlobalOrDefaultColor(QUESTION_COLOR_KEY), QUESTION_COLOR_KEY.getDefaultColorValue());
  }

  @Nonnull
  public static ColorValue getErrorColor() {
    return notNull(getGlobalOrDefaultColor(ERROR_COLOR_KEY), ERROR_COLOR_KEY.getDefaultColorValue());
  }

  @Nonnull
  public static ColorValue getRecentLocationsSelectionColor(EditorColorsScheme colorsScheme) {
    return notNull(colorsScheme.getColor(RECENT_LOCATIONS_SELECTION_KEY), RECENT_LOCATIONS_SELECTION_KEY.getDefaultColorValue());
  }

  public static JComponent createInformationLabel(@Nonnull String text) {
    return createInformationLabel(text, null, null, null);
  }

  public static JComponent createInformationLabel(@Nonnull String text,
                                                  @Nullable HyperlinkListener hyperlinkListener,
                                                  @Nullable MouseListener mouseListener,
                                                  @Nullable Ref<? super Consumer<? super String>> updatedTextConsumer) {
    HintHint hintHint = getInformationHint();
    HintLabel label = createLabel(text, null, hintHint.getTextBackground(), hintHint);
    configureLabel(label, hyperlinkListener, mouseListener, updatedTextConsumer);
    return label;
  }

  @Nonnull
  public static HintHint getInformationHint() {
    return new HintHint().setBorderColor(INFORMATION_BORDER_COLOR).setTextBg(TargetAWT.to(getInformationColor())).setTextFg(UIUtil.isUnderDarcula() ? UIUtil.getLabelForeground() : Color.black)
            .setFont(getBoldFont()).setAwtTooltip(true);
  }

  public static CompoundBorder createHintBorder() {
    //noinspection UseJBColor
    return BorderFactory.createCompoundBorder(new ColoredSideBorder(Color.white, Color.white, Color.gray, Color.gray, 1), BorderFactory.createEmptyBorder(2, 2, 2, 2));
  }

  @Nonnull
  public static JComponent createInformationLabel(SimpleColoredText text) {
    return createInformationLabel(text, null);
  }

  public static JComponent createQuestionLabel(String text) {
    final Image icon = AllIcons.General.ContextHelp;
    return createQuestionLabel(text, icon);
  }

  public static JComponent createQuestionLabel(String text, Image icon) {
    Color bg = TargetAWT.to(getQuestionColor());
    HintHint hintHint = new HintHint().setTextBg(bg).setTextFg(JBColor.foreground()).setFont(getBoldFont()).setAwtTooltip(true);

    return createLabel(text, icon, bg, hintHint);
  }

  @Nullable
  public static String getHintLabel(JComponent hintComponent) {
    if (hintComponent instanceof HintLabel) {
      return ((HintLabel)hintComponent).getText();
    }
    return null;
  }

  @Nullable
  public static Icon getHintIcon(JComponent hintComponent) {
    if (hintComponent instanceof HintLabel) {
      return ((HintLabel)hintComponent).getIcon();
    }
    return null;
  }

  @Nonnull
  public static SimpleColoredComponent createInformationComponent() {
    SimpleColoredComponent component = new SimpleColoredComponent();
    component.setBackground(TargetAWT.to(getInformationColor()));
    component.setForeground(JBColor.foreground());
    component.setFont(getBoldFont());
    return component;
  }

  @Nonnull
  public static JComponent createInformationLabel(@Nonnull SimpleColoredText text, @Nullable Image icon) {
    SimpleColoredComponent component = createInformationComponent();
    component.setIcon(icon);
    text.appendToComponent(component);
    return new HintLabel(component);
  }

  public static JComponent createErrorLabel(@Nonnull String text,
                                            @Nullable HyperlinkListener hyperlinkListener,
                                            @Nullable MouseListener mouseListener,
                                            @Nullable Ref<? super Consumer<? super String>> updatedTextConsumer) {
    Color bg = TargetAWT.to(getErrorColor());
    HintHint hintHint = new HintHint().setTextBg(bg).setTextFg(JBColor.foreground()).setFont(getBoldFont()).setAwtTooltip(true);

    HintLabel label = createLabel(text, null, bg, hintHint);
    configureLabel(label, hyperlinkListener, mouseListener, updatedTextConsumer);
    return label;
  }

  @Nonnull
  public static JComponent createErrorLabel(@Nonnull String text) {
    return createErrorLabel(text, null, null, null);
  }

  @Nonnull
  private static HintLabel createLabel(String text, @Nullable Image icon, @Nonnull Color color, @Nonnull HintHint hintHint) {
    HintLabel label = new HintLabel();
    label.setText(text, hintHint);
    label.setIcon(TargetAWT.to(icon));

    if (!hintHint.isAwtTooltip()) {
      label.setBorder(createHintBorder());
      label.setForeground(JBColor.foreground());
      label.setFont(getBoldFont());
      label.setBackground(color);
      label.setOpaque(true);
    }
    return label;
  }

  private static Font getBoldFont() {
    return UIUtil.getLabelFont().deriveFont(Font.BOLD);
  }

  @Nonnull
  public static JLabel createAdComponent(final String bottomText, final Border border, @JdkConstants.HorizontalAlignment int alignment) {
    JLabel label = new JLabel();
    label.setText(bottomText);
    label.setHorizontalAlignment(alignment);
    label.setForeground(JBUI.CurrentTheme.Advertiser.foreground());
    label.setBackground(JBUI.CurrentTheme.Advertiser.background());
    label.setOpaque(true);
    label.setFont(label.getFont().deriveFont((float)(label.getFont().getSize() - 2)));
    if (bottomText != null) {
      label.setBorder(border);
    }
    return label;
  }

  @Nonnull
  public static String prepareHintText(@Nonnull String text, @Nonnull HintHint hintHint) {
    return prepareHintText(new Html(text), hintHint);
  }

  public static String prepareHintText(@Nonnull Html text, @Nonnull HintHint hintHint) {
    String htmlBody = UIUtil.getHtmlBody(text);
    return String.format("<html><head>%s</head><body>%s</body></html>",
                         UIUtil.getCssFontDeclaration(hintHint.getTextFont(), hintHint.getTextForeground(), hintHint.getLinkForeground(), hintHint.getUlImg()), htmlBody);
  }

  private static void configureLabel(@Nonnull HintLabel label,
                                     @Nullable HyperlinkListener hyperlinkListener,
                                     @Nullable MouseListener mouseListener,
                                     @Nullable Ref<? super Consumer<? super String>> updatedTextConsumer) {
    if (hyperlinkListener != null) {
      label.myPane.addHyperlinkListener(hyperlinkListener);
    }
    if (mouseListener != null) {
      label.myPane.addMouseListener(mouseListener);
    }
    if (updatedTextConsumer != null) {
      Consumer<? super String> consumer = s -> {
        label.myPane.setText(s);

        // Force preferred size recalculation.
        label.setPreferredSize(null);
        label.myPane.setPreferredSize(null);
      };
      updatedTextConsumer.set(consumer);
    }
  }

  private static class HintLabel extends JPanel {
    private JEditorPane myPane;
    private SimpleColoredComponent myColored;
    private JLabel myIcon;

    private HintLabel() {
      setLayout(new BorderLayout());
    }

    private HintLabel(@Nonnull SimpleColoredComponent component) {
      this();
      setText(component);
    }

    @Override
    public boolean requestFocusInWindow() {
      // Forward the focus to the tooltip contents so that screen readers announce
      // the tooltip contents right away.
      if (myPane != null) {
        return myPane.requestFocusInWindow();
      }
      if (myColored != null) {
        return myColored.requestFocusInWindow();
      }
      if (myIcon != null) {
        return myIcon.requestFocusInWindow();
      }
      return super.requestFocusInWindow();
    }

    public void setText(@Nonnull SimpleColoredComponent colored) {
      clearText();

      myColored = colored;
      add(myColored, BorderLayout.CENTER);

      setOpaque(true);
      setBackground(colored.getBackground());

      revalidate();
      repaint();
    }

    public void setText(String s, HintHint hintHint) {
      clearText();

      if (s != null) {
        myPane = IdeTooltipManager.initPane(s, hintHint, null);
        add(myPane, BorderLayout.CENTER);
      }

      setOpaque(true);
      setBackground(hintHint.getTextBackground());

      revalidate();
      repaint();
    }

    private void clearText() {
      if (myPane != null) {
        remove(myPane);
        myPane = null;
      }

      if (myColored != null) {
        remove(myColored);
        myColored = null;
      }
    }

    public void setIcon(Icon icon) {
      if (myIcon != null) {
        remove(myIcon);
      }

      myIcon = new JLabel(icon, SwingConstants.CENTER);
      myIcon.setVerticalAlignment(SwingConstants.TOP);

      add(myIcon, BorderLayout.WEST);

      revalidate();
      repaint();
    }

    @Override
    public String toString() {
      return "Hint: text='" + getText() + "'";
    }

    public String getText() {
      return myPane != null ? myPane.getText() : "";
    }

    @Nullable
    public Icon getIcon() {
      return myIcon != null ? myIcon.getIcon() : null;
    }
  }
}
