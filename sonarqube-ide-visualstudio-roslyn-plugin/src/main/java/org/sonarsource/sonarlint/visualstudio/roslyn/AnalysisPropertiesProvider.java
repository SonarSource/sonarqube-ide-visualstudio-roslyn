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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonarsource.api.sonarlint.SonarLintSide;

@SonarLintSide
public class AnalysisPropertiesProvider {

  private static final List<String> settingsKeysToExclude = List.of("sonar.cs.internal.shouldUseCsharpEnterprise", "sonar.cs.internal.shouldUseVbEnterprise");
  private final SensorContext context;

  public AnalysisPropertiesProvider(SensorContext context) {
    this.context = context;
  }

  public Map<String, String> getAnalysisProperties() {
    var settings = context.settings();

    var settingsKeys = new ArrayList<String>();
    settingsKeys.addAll(settings.getKeysStartingWith(SqvsRoslynPluginPropertyDefinitions.PROP_PREFIX + CSharpLanguage.LANGUAGE_KEY));
    settingsKeys.addAll(settings.getKeysStartingWith(SqvsRoslynPluginPropertyDefinitions.PROP_PREFIX + VbNetLanguage.LANGUAGE_KEY));
    settingsKeys.removeIf(settingsKeysToExclude::contains);

    var analysisProperties = new HashMap<String, String>();
    settingsKeys.forEach(settingsKey -> analysisProperties.put(settingsKey, settings.getString(settingsKey)));

    return analysisProperties;
  }
}
