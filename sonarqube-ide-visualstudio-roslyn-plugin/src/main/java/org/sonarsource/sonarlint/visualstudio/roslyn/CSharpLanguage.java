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

import java.util.Objects;
import org.sonar.api.config.Configuration;
import org.sonar.api.resources.AbstractLanguage;

public class CSharpLanguage extends AbstractLanguage {

  public static final String LANGUAGE_KEY = "cs";
  public static final String LANGUAGE_NAME = "C#";
  public static final String REPOSITORY_KEY = "csharpsquid";
  public static final String FILE_SUFFIXES_DEFVALUE = ".cs,.cshtml,.razor";
  public static final String FILE_SUFFIXES_KEY = SqvsRoslynPluginPropertyDefinitions.PROP_PREFIX + LANGUAGE_KEY + "." + SqvsRoslynPluginPropertyDefinitions.PLUGIN_PREFIX
    + "file.suffixes";
  public static final String FILE_SUFFIXES_NAME = "CSharp file suffixes";
  public static final String RAZOR_EXTENSION = "cshtml";
  private final Configuration configuration;

  public CSharpLanguage(Configuration configuration) {
    super(LANGUAGE_KEY, LANGUAGE_NAME);
    this.configuration = configuration;
  }

  @Override
  public String[] getFileSuffixes() {
    return configuration.getStringArray(FILE_SUFFIXES_KEY);
  }

  @Override
  public boolean equals(Object o) {
    return super.equals(o) && o instanceof CSharpLanguage cSharpLanguage && configuration == cSharpLanguage.configuration;
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), configuration.hashCode());
  }
}
