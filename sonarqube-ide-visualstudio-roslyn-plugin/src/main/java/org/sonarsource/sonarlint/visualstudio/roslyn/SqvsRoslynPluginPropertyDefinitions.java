/*
 * SonarQube Ide VisualStudio Roslyn Plugin
 * Copyright (C) 2025-2025 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonarsource.sonarlint.visualstudio.roslyn;

import java.util.ArrayList;
import java.util.List;
import org.sonar.api.config.PropertyDefinition;

import static org.sonarsource.sonarlint.visualstudio.roslyn.CSharpLanguage.LANGUAGE_KEY;

public class SqvsRoslynPluginPropertyDefinitions {

  public static final String PROP_PREFIX = "sonar.";
  public static final String PLUGIN_PREFIX = "sqvsRoslynPlugin";
  public static final String HTML_LANGUAGE_KEY = "web";
  private static final String FILE_SUFFIXES_DESCRIPTION = "Comma-separated list of suffixes of files to analyze.";

  public static String getShouldUseCsharpEnterprise() {
    return PROP_PREFIX + LANGUAGE_KEY + ".internal.shouldUseCsharpEnterprise";
  }

  public static String getShouldUseVbEnterprise() {
    return PROP_PREFIX + LANGUAGE_KEY + ".internal.shouldUseVbEnterprise";
  }

  public static String getServerPort() {
    return PROP_PREFIX + PLUGIN_PREFIX + ".internal.serverPort";
  }

  public static String getServerToken() {
    return PROP_PREFIX + PLUGIN_PREFIX + ".internal.serverToken";
  }

  public List<PropertyDefinition> create() {
    List<PropertyDefinition> result = new ArrayList<>();
    result.add(
      PropertyDefinition.builder(CSharpLanguage.FILE_SUFFIXES_KEY)
        .category(CSharpLanguage.LANGUAGE_NAME)
        .defaultValue(CSharpLanguage.FILE_SUFFIXES_DEFVALUE)
        .name(CSharpLanguage.FILE_SUFFIXES_NAME)
        .description(FILE_SUFFIXES_DESCRIPTION)
        .multiValues(true)
        .onConfigScopes(PropertyDefinition.ConfigScope.PROJECT)
        .build());
    result.add(
      PropertyDefinition.builder(VbNetLanguage.FILE_SUFFIXES_KEY)
        .category(VbNetLanguage.LANGUAGE_NAME)
        .defaultValue(VbNetLanguage.FILE_SUFFIXES_DEFVALUE)
        .name(VbNetLanguage.FILE_SUFFIXES_NAME)
        .description(FILE_SUFFIXES_DESCRIPTION)
        .multiValues(true)
        .onConfigScopes(PropertyDefinition.ConfigScope.PROJECT)
        .build());
    result.add(
      PropertyDefinition.builder(getServerPort())
        .hidden()
        .build());
    result.add(
      PropertyDefinition.builder(getServerToken())
        .hidden()
        .build());
    result.add(
      PropertyDefinition.builder(getShouldUseVbEnterprise())
        .hidden()
        .build());
    result.add(
      PropertyDefinition.builder(getShouldUseCsharpEnterprise())
        .hidden()
        .build());
    return result;
  }

}
