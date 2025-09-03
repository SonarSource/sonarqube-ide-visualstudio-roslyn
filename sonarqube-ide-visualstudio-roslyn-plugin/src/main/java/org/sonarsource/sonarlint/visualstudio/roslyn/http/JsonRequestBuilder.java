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
package org.sonarsource.sonarlint.visualstudio.roslyn.http;

import com.google.gson.Gson;
import java.util.Collection;
import java.util.Map;
import org.sonar.api.batch.rule.ActiveRule;
import org.sonar.api.scanner.ScannerSide;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonarsource.api.sonarlint.SonarLintSide;

@ScannerSide
@SonarLintSide(lifespan = "INSTANCE")
public class JsonRequestBuilder {
  private static final Logger LOG = Loggers.get(JsonRequestBuilder.class);

  public String buildBody(Collection<String> fileNames, Collection<ActiveRule> activeRules, Map<String, String> analysisProperties, AnalyzerInfoDto analyzerInfo) {
    var activeRuleDtos = activeRules.stream()
      .map(rule -> new ActiveRuleDto(
        rule.ruleKey().toString(),
        rule.params()))
      .toList();
    var analysisRequest = new AnalysisRequestDto(fileNames, activeRuleDtos, analysisProperties, analyzerInfo);

    return new Gson().toJson(analysisRequest);
  }
}
