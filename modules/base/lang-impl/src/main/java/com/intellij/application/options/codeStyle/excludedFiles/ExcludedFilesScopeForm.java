// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.application.options.codeStyle.excludedFiles;

import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;

import javax.swing.*;
import java.awt.*;

public class ExcludedFilesScopeForm {
  private JPanel myTopPanel;
  private JBList<String> myScopesList;

  public ExcludedFilesScopeForm() {
    myTopPanel = new JPanel(new BorderLayout());
    myTopPanel.setMinimumSize(new Dimension(myTopPanel.getMinimumSize().width, JBUI.scale(100)));
    myScopesList = new JBList<>();
    myTopPanel.add(new JBScrollPane(myScopesList), BorderLayout.CENTER);
  }

  public JPanel getTopPanel() {
    return myTopPanel;
  }

  public JBList<String> getScopesList() {
    return myScopesList;
  }
}
