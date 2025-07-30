/*
 * SonarQube Ide VisualStudio Roslyn Plugin
 * Copyright (C) 2025-2025 SonarSource SA
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
import org.sonar.api.resources.Qualifiers;

import static org.sonarsource.sonarlint.visualstudio.roslyn.SqvsRoslynPluginConstants.LANGUAGE_KEY;

public class SqvsRoslynPluginPropertyDefinitions {

  private static final String PROP_PREFIX = "sonar.";

  static final String FILE_SUFFIXES_KEY = PROP_PREFIX + SqvsRoslynPluginConstants.LANGUAGE_KEY + ".file.suffixes";
  static final String FILE_SUFFIXES_DEFVALUE = ".cs";

  public List<PropertyDefinition> create() {
    List<PropertyDefinition> result = new ArrayList<>();
    result.add(
      PropertyDefinition.builder(getFileSuffixProperty())
        .category(SqvsRoslynPluginConstants.LANGUAGE_NAME)
        .defaultValue(FILE_SUFFIXES_DEFVALUE)
        .name("File suffixes")
        .description("Comma-separated list of suffixes of files to analyze.")
        .multiValues(true)
              .onConfigScopes(PropertyDefinition.ConfigScope.PROJECT)
        .onQualifiers(Qualifiers.PROJECT)
        .build());
    result.add(
      PropertyDefinition.builder(getAnalyzerPath())
        .hidden()
        .build()
    );
    return result;
  }

  public static String getFileSuffixProperty() {
    return PROP_PREFIX + LANGUAGE_KEY + ".file.suffixes";
  }

  public static String getAnalyzerPath() {
    return PROP_PREFIX + LANGUAGE_KEY + ".internal.analyzerPath";
  }
}
