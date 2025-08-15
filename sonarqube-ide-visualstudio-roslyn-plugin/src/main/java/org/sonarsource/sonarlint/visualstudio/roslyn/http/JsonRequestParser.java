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

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.Collection;
import java.util.Map;
import org.sonar.api.batch.rule.ActiveRule;
import org.sonar.api.scanner.ScannerSide;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonarsource.api.sonarlint.SonarLintSide;

@ScannerSide
@SonarLintSide(lifespan = "INSTANCE")
public class JsonRequestParser {
  private static final Logger LOG = Loggers.get(JsonRequestParser.class);

  public String buildBody(Collection<String> fileNames, Collection<ActiveRule> activeRules) {
    if (fileNames == null || activeRules == null) {
      LOG.warn("fileNames or activeRules are null");
      return "";
    }
    var bodyJsonObj = new JsonObject();
    bodyJsonObj.add("fileNames", buildFileNames(fileNames));
    bodyJsonObj.add("activeRules", buildRulesConfig(activeRules));
    return bodyJsonObj.toString();
  }

  private static JsonArray buildFileNames(Collection<String> fileNames) {
    JsonArray filesJson = new JsonArray();
    for (String fileName : fileNames) {
      filesJson.add(fileName);
    }

    return filesJson;
  }

  private static JsonArray buildRulesConfig(Collection<ActiveRule> activeRules) {
    JsonArray rulesJson = new JsonArray();
    for (ActiveRule activeRule : activeRules) {
      JsonObject ruleJson = new JsonObject();
      ruleJson.addProperty("ruleId", activeRule.ruleKey().rule());
      if (!activeRule.params().isEmpty()) {
        JsonObject paramsJson = new JsonObject();
        for (Map.Entry<String, String> param : activeRule.params().entrySet()) {
          paramsJson.addProperty(param.getKey(), param.getValue());
        }
        ruleJson.add("params", paramsJson);
      }
      rulesJson.add(ruleJson);
    }
    return rulesJson;
  }
}
