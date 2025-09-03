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

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.config.internal.MapSettings;

import static org.assertj.core.api.Assertions.assertThat;

class AnalysisPropertiesProviderTest {
  private SensorContextTester sensorContext;
  private AnalysisPropertiesProvider underTest;

  @BeforeEach
  void prepare(@TempDir Path tmp) throws IOException {
    var baseDir = tmp.toRealPath();
    sensorContext = SensorContextTester.create(baseDir);
    underTest = new AnalysisPropertiesProvider(sensorContext);
  }

  @Test
  void csharpPropertiesDefined_returnsAsExpected() {
    mockAnalysisProperties(Map.of("sonar.cs.disableRazor", "true"));

    var result = underTest.getAnalysisProperties();

    assertThat(result).hasSize(1).contains(Map.entry("sonar.cs.disableRazor", "true"));
  }

  @Test
  void vbPropertiesDefined_returnsAsExpected() {
    mockAnalysisProperties(Map.of("sonar.vbnet.disableRazor", "true"));

    var result = underTest.getAnalysisProperties();

    assertThat(result).hasSize(1).contains(Map.entry("sonar.vbnet.disableRazor", "true"));
  }

  @Test
  void multipleAnalysisPropertiesDefined_returnsOnlyCsharpAndVbNet() {
    var analysisProperties = Map.of("sonar.vbnet.disableRazor", "false", "sonar.js.bundledNodePath", "somePath", "otherProp", "someValue", "sonar.cs.disableRazor", "true");
    mockAnalysisProperties(analysisProperties);

    var result = underTest.getAnalysisProperties();

    assertThat(result).hasSize(2)
      .contains(Map.entry("sonar.vbnet.disableRazor", "false"))
      .contains(Map.entry("sonar.cs.disableRazor", "true"));
  }

  @Test
  void repackagingPropertiesDefined_filtersThemOut() {
    var analysisProperties = Map.of("sonar.cs.internal.shouldUseCsharpEnterprise", "true", "sonar.cs.internal.shouldUseVbEnterprise", "true");
    mockAnalysisProperties(analysisProperties);

    var result = underTest.getAnalysisProperties();

    assertThat(result).isEmpty();
  }

  private void mockAnalysisProperties(Map<String, String> analysisProperties) {
    var mapSettings = new MapSettings();
    analysisProperties.forEach(mapSettings::setProperty);
    sensorContext.setSettings(mapSettings);
  }
}
