/*
 * Copyright 2013-2018 consulo.io
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
package consulo.injecting.pico;

import consulo.injecting.InjectingContainer;
import consulo.injecting.InjectingContainerBuilder;
import consulo.injecting.RootInjectingContainerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author VISTALL
 * @since 2018-08-24
 */
public class PicoRootInjectingContainerFactory implements RootInjectingContainerFactory {
  private static final InjectingContainer ROOT = new InjectingContainer() {
    @Nullable
    @Override
    public <T> T getInstance(@Nonnull Class<T> clazz) {
      throw new UnsupportedOperationException();
    }

    @Nonnull
    @Override
    public <T> T getUnbindedInstance(@Nonnull Class<T> clazz) {
      throw new UnsupportedOperationException();
    }

    @Nonnull
    @Override
    public InjectingContainerBuilder childBuilder() {
      return new PicoInjectingContainerBuilder(null);
    }

    @Override
    public void dispose() {
    }
  };

  @Nonnull
  @Override
  public InjectingContainer getRoot() {
    return ROOT;
  }
}
