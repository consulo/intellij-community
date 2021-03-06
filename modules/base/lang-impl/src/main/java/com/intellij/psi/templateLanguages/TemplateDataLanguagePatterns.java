/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package com.intellij.psi.templateLanguages;

import com.intellij.lang.Language;
import com.intellij.openapi.components.*;
import com.intellij.openapi.fileTypes.FileNameMatcher;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.fileTypes.impl.FileTypeAssocTable;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import gnu.trove.THashMap;
import jakarta.inject.Singleton;
import org.jdom.Element;

import javax.annotation.Nullable;
import java.util.List;

/**
 * @author peter
 */
@Singleton
@State(name = "TemplateDataLanguagePatterns", storages = @Storage("templateLanguages.xml"))
public class TemplateDataLanguagePatterns implements PersistentStateComponent<Element> {
  private FileTypeAssocTable<Language> myAssocTable = new FileTypeAssocTable<Language>();

  private static final String SEPARATOR = ";";

  public static TemplateDataLanguagePatterns getInstance() {
    return ServiceManager.getService(TemplateDataLanguagePatterns.class);
  }

  public FileTypeAssocTable<Language> getAssocTable() {
    return myAssocTable.copy();
  }

  @Nullable
  public Language getTemplateDataLanguageByFileName(VirtualFile file) {
    return myAssocTable.findAssociatedFileType(file.getName());
  }

  public void setAssocTable(FileTypeAssocTable<Language> assocTable) {
    myAssocTable = assocTable.copy();
  }

  @Override
  public void loadState(Element state) {
    myAssocTable = new FileTypeAssocTable<Language>();

    final THashMap<String, Language> dialectMap = new THashMap<String, Language>();
    for (Language dialect : TemplateDataLanguageMappings.getTemplateableLanguages()) {
      dialectMap.put(dialect.getID(), dialect);
    }
    final List<Element> files = state.getChildren("pattern");
    for (Element fileElement : files) {
      final String patterns = fileElement.getAttributeValue("value");
      final String langId = fileElement.getAttributeValue("lang");
      final Language dialect = dialectMap.get(langId);
      if (dialect == null || StringUtil.isEmpty(patterns)) continue;

      for (String pattern : patterns.split(SEPARATOR)) {
        myAssocTable.addAssociation(FileTypeManager.parseFromString(pattern), dialect);
      }

    }
  }

  @Override
  public Element getState() {
    Element state = new Element("x");
    for (final Language language : TemplateDataLanguageMappings.getTemplateableLanguages()) {
      final List<FileNameMatcher> matchers = myAssocTable.getAssociations(language);
      if (!matchers.isEmpty()) {
        final Element child = new Element("pattern");
        state.addContent(child);
        child.setAttribute("value", StringUtil.join(matchers, FileNameMatcher::getPresentableString, SEPARATOR));
        child.setAttribute("lang", language.getID());
      }
    }
    return state;
  }

}