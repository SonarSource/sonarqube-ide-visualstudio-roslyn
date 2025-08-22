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

class VbNetLanguageTests {
  private MapSettings settings;
  private VbNetLanguage vbNetLanguage;

  @BeforeEach
  void init() {
    var propertyDefinitions = new PropertyDefinitions(System2.INSTANCE, new SqvsRoslynPluginPropertyDefinitions().create());
    settings = new MapSettings(propertyDefinitions);
    vbNetLanguage = new VbNetLanguage(settings.asConfig());
  }

  @Test
  void shouldHaveExpectedProperties() {
    assertThat(VbNetLanguage.LANGUAGE_KEY).isEqualTo("vbnet");
    assertThat(VbNetLanguage.LANGUAGE_NAME).isEqualTo("VB.NET");
    assertThat(VbNetLanguage.REPOSITORY_KEY).isEqualTo("vbnet");
    assertThat(VbNetLanguage.FILE_SUFFIXES_DEFVALUE).isEqualTo(".vb,.vbhtml");
    assertThat(VbNetLanguage.FILE_SUFFIXES_KEY).isEqualTo("sonar.vbnet.sqvs.file.suffixes");
  }

  @Test
  void shouldGetDefaultFileSuffixes() {
    assertThat(vbNetLanguage.getFileSuffixes()).contains(".vb", ".vbhtml");
  }

  @Test
  void shouldGetCustomFileSuffixes() {
    settings.setProperty(VbNetLanguage.FILE_SUFFIXES_KEY, ".vb, .vbnet");

    assertThat(vbNetLanguage.getFileSuffixes()).containsOnly(".vb", ".vbnet");
  }

  @Test
  void equals_and_hashCode_considers_configuration() {
    var otherSettings = new MapSettings();
    otherSettings.setProperty("key", "value");
    var otherVbNet = new VbNetLanguage(otherSettings.asConfig());
    var sameVbNet = new VbNetLanguage(settings.asConfig());
    var fakeVbNet = new VbNetLanguageTests.FakeVbNet();

    assertThat(vbNetLanguage).isEqualTo(sameVbNet)
      .isNotEqualTo(otherVbNet)
      .isNotEqualTo(fakeVbNet)
      .isNotEqualTo(null)
      .hasSameHashCodeAs(sameVbNet);
    assertThat(vbNetLanguage.hashCode()).isNotEqualTo(otherVbNet.hashCode());
  }

  private class FakeVbNet extends AbstractLanguage {

    public FakeVbNet() {
      super(VbNetLanguage.LANGUAGE_KEY, VbNetLanguage.LANGUAGE_NAME);
    }

    @Override
    public String[] getFileSuffixes() {
      return new String[0];
    }
  }
}
