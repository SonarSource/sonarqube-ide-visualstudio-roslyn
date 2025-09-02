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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sonar.api.config.PropertyDefinitions;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.resources.AbstractLanguage;
import org.sonar.api.utils.System2;

import static org.assertj.core.api.Assertions.assertThat;

class CSharpLanguageTests {

  private MapSettings settings;
  private CSharpLanguage csharp;

  @BeforeEach
  void init() {
    PropertyDefinitions defs = new PropertyDefinitions(System2.INSTANCE,
      new SqvsRoslynPluginPropertyDefinitions().create());
    settings = new MapSettings(defs);
    csharp = new CSharpLanguage(settings.asConfig());
  }

  @Test
  void shouldHaveExpectedProperties() {
    assertThat(CSharpLanguage.LANGUAGE_KEY).isEqualTo("cs");
    assertThat(CSharpLanguage.LANGUAGE_NAME).isEqualTo("C#");
    assertThat(CSharpLanguage.REPOSITORY_KEY).isEqualTo("csharpsquid");
    assertThat(CSharpLanguage.FILE_SUFFIXES_DEFVALUE).isEqualTo(".cs,.cshtml,.razor");
    assertThat(CSharpLanguage.FILE_SUFFIXES_KEY).isEqualTo("sonar.cs.sqvsRoslynPlugin.file.suffixes");
  }

  @Test
  void shouldGetDefaultFileSuffixes() {
    assertThat(csharp.getFileSuffixes()).contains(".cs", ".cshtml", ".razor");
  }

  @Test
  void shouldGetCustomFileSuffixes() {
    settings.setProperty(CSharpLanguage.FILE_SUFFIXES_KEY, ".cs,.csharp");
    assertThat(csharp.getFileSuffixes()).containsOnly(".cs", ".csharp");
  }

  @Test
  void equals_and_hashCode_considers_configuration() {
    MapSettings otherSettings = new MapSettings();
    otherSettings.setProperty("key", "value");
    CSharpLanguage otherCSharp = new CSharpLanguage(otherSettings.asConfig());
    CSharpLanguage sameCSharp = new CSharpLanguage(settings.asConfig());
    FakeCSharp fakeCSharp = new FakeCSharp();

    assertThat(csharp).isEqualTo(sameCSharp)
      .isNotEqualTo(otherCSharp)
      .isNotEqualTo(fakeCSharp)
      .isNotEqualTo(null)
      .hasSameHashCodeAs(sameCSharp);
    assertThat(csharp.hashCode()).isNotEqualTo(otherCSharp.hashCode());
  }

  private class FakeCSharp extends AbstractLanguage {

    public FakeCSharp() {
      super(CSharpLanguage.LANGUAGE_KEY, CSharpLanguage.LANGUAGE_NAME);
    }

    @Override
    public String[] getFileSuffixes() {
      return new String[0];
    }
  }
}
