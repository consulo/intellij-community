/*
 * Copyright 2000-2013 JetBrains s.r.o.
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

/*
 * Created by IntelliJ IDEA.
 * User: mike
 * Date: Sep 16, 2002
 * Time: 5:17:44 PM
 * To change template for new class use 
 * Code Style | Class Templates options (Tools | IDE Options).
 */
package com.intellij.openapi.application.ex;

import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.util.IconLoader;
import consulo.ide.webService.WebServiceApi;
import consulo.util.SandboxUtil;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

public abstract class ApplicationInfoEx extends ApplicationInfo {

  public static ApplicationInfoEx getInstanceEx() {
    return (ApplicationInfoEx) ApplicationInfo.getInstance();
  }

  public abstract String getSplashImageUrl();

  public abstract String getAboutImageUrl();

  public String getIconUrl() {
    return getUrl("/icon32");
  }

  public String getSmallIconUrl() {
    return getUrl("/icon16");
  }

  @Nullable
  public String getBigIconUrl() {
    return getUrl("/icon128");
  }

  public String getOpaqueIconUrl() {
    return getUrl("/icon32");
  }

  public static Icon getWelcomeScreenLogo() {
    return IconLoader.getIcon(getUrl("/Logo_welcomeScreen"));
  }

  private static String getUrl(String prefix) {
    return (SandboxUtil.isInsideSandbox() ? prefix + "-sandbox" : prefix) + ".png";
  }

  public abstract String getFullApplicationName();

  @Deprecated
  public abstract boolean isEAP();

  public abstract String getDocumentationUrl();

  public abstract String getSupportUrl();

  public abstract String getEAPFeedbackUrl();

  public abstract String getReleaseFeedbackUrl();

  public String getPluginManagerUrl() {
    return WebServiceApi.REPOSITORY_API.buildUrl();
  }

  public String getPluginsListUrl() {
    return WebServiceApi.REPOSITORY_API.buildUrl("list");
  }

  public String getPluginsDownloadUrl() {
    return WebServiceApi.REPOSITORY_API.buildUrl("download");
  }

  public abstract String getUpdatesInfoUrl();

  public abstract String getUpdatesDownloadUrl();

  public abstract String getStatisticsUrl();

  public abstract String getWebHelpUrl();

  public abstract String getWhatsNewUrl();

  public abstract String getWinKeymapUrl();

  public abstract String getMacKeymapUrl();

  public abstract Color getAboutForeground();
}
