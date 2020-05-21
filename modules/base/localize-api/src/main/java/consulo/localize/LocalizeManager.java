/*
 * Copyright 2013-2019 consulo.io
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
package consulo.localize;

import consulo.annotation.ReviewAfterMigrationToJRE;
import consulo.disposer.Disposable;

import javax.annotation.Nonnull;
import java.util.Iterator;
import java.util.Locale;
import java.util.ServiceLoader;
import java.util.Set;

/**
 * @author VISTALL
 * @since 2019-04-11
 */
public abstract class LocalizeManager {
  private static LocalizeManager ourInstance = loadSingleOrError(LocalizeManager.class);

  @Nonnull
  @ReviewAfterMigrationToJRE(value = 9, description = "Use consulo.util.ServiceLoaderUtil")
  private static <T> T loadSingleOrError(@Nonnull Class<T> clazz) {
    ServiceLoader<T> serviceLoader = ServiceLoader.load(clazz, clazz.getClassLoader());
    Iterator<T> iterator = serviceLoader.iterator();
    if (iterator.hasNext()) {
      return iterator.next();
    }
    throw new Error("Unable to find '" + clazz.getName() + "' implementation");
  }

  @Nonnull
  public static LocalizeManager getInstance() {
    return ourInstance;
  }

  /**
   * Return unformatted localize text
   *
   * @throws IllegalArgumentException if key is invalid
   */
  @Nonnull
  public abstract String getUnformattedText(@Nonnull LocalizeKey key);

  public abstract void setLocale(@Nonnull Locale locale);

  @Nonnull
  public abstract Locale getLocale();

  @Nonnull
  public abstract Set<Locale> getAvaliableLocales();

  public abstract void addListener(@Nonnull LocalizeManagerListener listener, @Nonnull Disposable disposable);

  public abstract int getModificationCount();

  @Nonnull
  public abstract String formatText(String unformattedText, Object... arg);
}
