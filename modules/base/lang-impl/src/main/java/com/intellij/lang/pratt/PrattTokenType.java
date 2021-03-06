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
package com.intellij.lang.pratt;

import com.intellij.lang.Language;
import com.intellij.psi.PsiBundle;
import com.intellij.psi.tree.IElementType;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.jetbrains.annotations.NonNls;

/**
 * @author peter
 */
public class PrattTokenType extends IElementType {


  public PrattTokenType(@Nonnull @NonNls final String debugName,
                        @Nullable final Language language) {
    super(debugName, language);
  }

  public String getExpectedText(final PrattBuilder builder) {
    return PsiBundle.message("0.expected", toString());
  }

}
