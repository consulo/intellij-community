/*
 * Copyright 2013-2020 consulo.io
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
package consulo.ide.settings.impl;

import com.intellij.ide.ui.UISettings;
import com.intellij.openapi.application.ApplicationBundle;
import com.intellij.openapi.options.Configurable;
import consulo.options.SimpleConfigurableByProperties;
import consulo.ui.*;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.LabeledLayout;
import consulo.ui.layout.VerticalLayout;
import consulo.ui.border.BorderPosition;
import consulo.ui.border.BorderStyle;
import consulo.ui.util.LabeledComponents;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 2020-04-25
 */
public class EditorTabsConfigurable extends SimpleConfigurableByProperties implements Configurable {
  enum ActiveTabState {
    LEFT,
    RIGHT,
    MOST_RECENT
  }

  @RequiredUIAccess
  @Nonnull
  @Override
  protected Component createLayout(PropertyBuilder propertyBuilder) {
    UISettings uiSettings = UISettings.getInstance();

    VerticalLayout layout = VerticalLayout.create();

    VerticalLayout tabAppearanceLayout = VerticalLayout.create();

    ComboBox.Builder<Integer> tabPlacement = ComboBox.builder();
    tabPlacement.add(UISettings.PLACEMENT_EDITOR_TAB_NONE, ApplicationBundle.message("combobox.tab.placement.none"));
    tabPlacement.add(UISettings.PLACEMENT_EDITOR_TAB_TOP, ApplicationBundle.message("combobox.tab.placement.top"));
    tabPlacement.add(UISettings.PLACEMENT_EDITOR_TAB_BOTTOM, ApplicationBundle.message("combobox.tab.placement.bottom"));
    tabPlacement.add(UISettings.PLACEMENT_EDITOR_TAB_LEFT, ApplicationBundle.message("combobox.tab.placement.left"));
    tabPlacement.add(UISettings.PLACEMENT_EDITOR_TAB_RIGHT, ApplicationBundle.message("combobox.tab.placement.right"));

    ComboBox<Integer> tabPlacementBox = tabPlacement.build();
    propertyBuilder.add(tabPlacementBox, () -> uiSettings.EDITOR_TAB_PLACEMENT, v -> uiSettings.EDITOR_TAB_PLACEMENT = v);

    tabAppearanceLayout.add(LabeledComponents.leftWithRight(ApplicationBundle.message("combobox.editor.tab.placement"), tabPlacementBox));

    CheckBox showTabsInSingleRow = CheckBox.create(ApplicationBundle.message("checkbox.editor.tabs.in.single.row"));
    showTabsInSingleRow.setEnabled(false);
    propertyBuilder.add(showTabsInSingleRow, () -> uiSettings.SCROLL_TAB_LAYOUT_IN_EDITOR, v -> uiSettings.SCROLL_TAB_LAYOUT_IN_EDITOR = v);
    tabAppearanceLayout.add(showTabsInSingleRow);

    CheckBox hideExtensionsInTabs = CheckBox.create(ApplicationBundle.message("checkbox.hide.file.extension.in.editor.tabs"));
    hideExtensionsInTabs.setEnabled(false);
    propertyBuilder.add(hideExtensionsInTabs, () -> uiSettings.HIDE_KNOWN_EXTENSION_IN_TABS, v -> uiSettings.HIDE_KNOWN_EXTENSION_IN_TABS = v);
    tabAppearanceLayout.add(hideExtensionsInTabs);

    CheckBox showDirectoryWithNotUniqueName = CheckBox.create("Show directory in editor tabs for non-unique filenames");
    showDirectoryWithNotUniqueName.setEnabled(false);
    propertyBuilder.add(showDirectoryWithNotUniqueName, () -> uiSettings.SHOW_DIRECTORY_FOR_NON_UNIQUE_FILENAMES, v -> uiSettings.SHOW_DIRECTORY_FOR_NON_UNIQUE_FILENAMES = v);
    tabAppearanceLayout.add(showDirectoryWithNotUniqueName);

    CheckBox showCloseButton = CheckBox.create(ApplicationBundle.message("checkbox.editor.tabs.show.close.button"));
    showCloseButton.setEnabled(false);
    propertyBuilder.add(showCloseButton, () -> uiSettings.SHOW_CLOSE_BUTTON, v -> uiSettings.SHOW_CLOSE_BUTTON = v);
    tabAppearanceLayout.add(showCloseButton);

    CheckBox markModifiedTabsWithAsterisk = CheckBox.create(ApplicationBundle.message("checkbox.mark.modified.tabs.with.asterisk"));
    markModifiedTabsWithAsterisk.setEnabled(false);
    propertyBuilder.add(markModifiedTabsWithAsterisk, () -> uiSettings.MARK_MODIFIED_TABS_WITH_ASTERISK, v -> uiSettings.MARK_MODIFIED_TABS_WITH_ASTERISK = v);
    tabAppearanceLayout.add(markModifiedTabsWithAsterisk);

    CheckBox showTabsTooltip = CheckBox.create(ApplicationBundle.message("checkbox.show.tabs.tooltips"));
    showTabsTooltip.setEnabled(false);
    propertyBuilder.add(showTabsTooltip, () -> uiSettings.SHOW_TABS_TOOLTIPS, v -> uiSettings.SHOW_TABS_TOOLTIPS = v);
    tabAppearanceLayout.add(showTabsTooltip);

    tabPlacementBox.addValueListener(event -> {
      boolean isNotNoneTabPlacement = event.getValue() != UISettings.PLACEMENT_EDITOR_TAB_NONE;

      showTabsInSingleRow.setEnabled(isNotNoneTabPlacement);
      hideExtensionsInTabs.setEnabled(isNotNoneTabPlacement);
      showDirectoryWithNotUniqueName.setEnabled(isNotNoneTabPlacement);
      showCloseButton.setEnabled(isNotNoneTabPlacement);
      markModifiedTabsWithAsterisk.setEnabled(isNotNoneTabPlacement);
      showTabsTooltip.setEnabled(isNotNoneTabPlacement);

      if (event.getValue() == UISettings.PLACEMENT_EDITOR_TAB_TOP) {
        showTabsInSingleRow.setEnabled(true);
      }
      else {
        showTabsInSingleRow.setValue(true);
        showTabsInSingleRow.setEnabled(false);
      }
    });

    layout.add(LabeledLayout.create(ApplicationBundle.message("group.tab.appearance"), tabAppearanceLayout));

    VerticalLayout tabClosingPolicyLayout = VerticalLayout.create();

    IntBox tabLimitBox = IntBox.create();
    propertyBuilder.add(tabLimitBox, () -> uiSettings.EDITOR_TAB_LIMIT, v -> uiSettings.EDITOR_TAB_LIMIT = v);
    tabClosingPolicyLayout.add(LabeledComponents.leftWithRight(ApplicationBundle.message("editbox.tab.limit"), tabLimitBox));

    tabClosingPolicyLayout.add(Label.create(ApplicationBundle.message("label.when.number.of.opened.editors.exceeds.tab.limit")));

    RadioButton closeNotModifiedFiles = RadioButton.create(ApplicationBundle.message("radio.close.non.modified.files.first"));
    RadioButton closeLessFrequentlyFiles = RadioButton.create(ApplicationBundle.message("radio.close.less.frequently.used.files"));

    propertyBuilder.add(() -> {
      if (closeNotModifiedFiles.getValueOrError()) {
        return true;
      }

      if (closeLessFrequentlyFiles.getValueOrError()) {
        return false;
      }

      throw new IllegalArgumentException();
    }, v -> {
      if (v) {
        closeNotModifiedFiles.setValue(true);
      }
      else {
        closeLessFrequentlyFiles.setValue(true);
      }
    }, () -> uiSettings.CLOSE_NON_MODIFIED_FILES_FIRST, v -> uiSettings.CLOSE_NON_MODIFIED_FILES_FIRST = v);

    ValueGroup.createBool().add(closeNotModifiedFiles).add(closeLessFrequentlyFiles);

    VerticalLayout leftIndent = VerticalLayout.create().add(closeNotModifiedFiles).add(closeLessFrequentlyFiles);
    leftIndent.addBorder(BorderPosition.LEFT, BorderStyle.EMPTY, null, 15);
    tabClosingPolicyLayout.add(leftIndent);

    tabClosingPolicyLayout.add(Label.create(ApplicationBundle.message("label.when.closing.active.editor")));

    RadioButton activeLeft = RadioButton.create(ApplicationBundle.message("radio.activate.left.neighbouring.tab"));
    RadioButton activeRight = RadioButton.create(ApplicationBundle.message("radio.activate.right.neighbouring.tab"));
    RadioButton activeMost = RadioButton.create(ApplicationBundle.message("radio.activate.most.recently.opened.tab"));

    ValueGroup.createBool().add(activeLeft).add(activeRight).add(activeMost);

    propertyBuilder.add(() -> {
      if (activeLeft.getValueOrError()) {
        return ActiveTabState.LEFT;
      }
      else if (activeRight.getValueOrError()) {
        return ActiveTabState.RIGHT;
      }
      return ActiveTabState.MOST_RECENT;
    }, activeTabState -> {
      switch (activeTabState) {
        case LEFT:
          activeLeft.setValue(true);
          break;
        case RIGHT:
          activeRight.setValue(true);
          break;
        case MOST_RECENT:
          activeMost.setValue(true);
          break;
      }
    }, () -> {
      if (uiSettings.ACTIVATE_MRU_EDITOR_ON_CLOSE) {
        return ActiveTabState.MOST_RECENT;
      }

      if(uiSettings.ACTIVATE_RIGHT_EDITOR_ON_CLOSE) {
        return ActiveTabState.RIGHT;
      }
      return ActiveTabState.LEFT;
    }, t -> {
      uiSettings.ACTIVATE_MRU_EDITOR_ON_CLOSE = t == ActiveTabState.MOST_RECENT;
      uiSettings.ACTIVATE_RIGHT_EDITOR_ON_CLOSE = t == ActiveTabState.RIGHT;
    });

    leftIndent = VerticalLayout.create().add(activeLeft).add(activeRight).add(activeMost);
    leftIndent.addBorder(BorderPosition.LEFT, BorderStyle.EMPTY, null, 15);
    tabClosingPolicyLayout.add(leftIndent);

    layout.add(LabeledLayout.create(ApplicationBundle.message("group.tab.closing.policy"), tabClosingPolicyLayout));
    return layout;
  }

  @Override
  protected void afterApply() {
    UISettings.getInstance().fireUISettingsChanged();
  }
}
