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

import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sonar.api.batch.rule.ActiveRule;
import org.sonar.api.rule.RuleKey;
import org.sonarsource.sonarlint.visualstudio.roslyn.CSharpLanguage;
import org.sonarsource.sonarlint.visualstudio.roslyn.VbNetLanguage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JsonRequestBuilderTests {

  private JsonRequestBuilder jsonParser;

  @BeforeEach
  void init() {
    jsonParser = new JsonRequestBuilder();
  }

  @Test
  void buildBody_withEmptyCollections_shouldReturnValidJson() {
    var fileNames = new ArrayList<String>();
    var activeRules = new ArrayList<ActiveRule>();
    var expected = "{\"FileNames\":[],\"ActiveRules\":[]}";

    var result = jsonParser.buildBody(fileNames, activeRules);

    assertThat(result).isEqualTo(expected);
  }

  @Test
  void buildBody_withFileNamesAndActiveRules_shouldReturnValidJson() {
    var fileNames = List.of("File1.cs", "File2.vb");
    var activeRules = List.of(createMockActiveRule("S100", CSharpLanguage.REPOSITORY_KEY, new HashMap<>()),
      createMockActiveRule("S101", VbNetLanguage.REPOSITORY_KEY, new HashMap<>()));
    var expected = "{\"FileNames\":[\"File1.cs\",\"File2.vb\"],\"ActiveRules\":[{\"RuleId\":\"csharpsquid:S100\",\"Parameters\":{}},{\"RuleId\":\"vbnet:S101\",\"Parameters\":{}}]}";

    var result = jsonParser.buildBody(fileNames, activeRules);

    assertThat(result).isEqualTo(expected);
  }

  @Test
  void buildBody_withActiveRules_shouldReturnRuleId() {
    var fileNames = new ArrayList<String>();
    var activeRule = createMockActiveRule("S100", CSharpLanguage.REPOSITORY_KEY, new HashMap<>());
    var expected = "{\"FileNames\":[],\"ActiveRules\":[{\"RuleId\":\"csharpsquid:S100\",\"Parameters\":{}}]}";

    var result = jsonParser.buildBody(fileNames, List.of(activeRule));

    assertThat(result).isEqualTo(expected);
  }

  @Test
  void buildBody_withActiveRulesWithParams_shouldIncludeParams() {
    var fileNames = new ArrayList<String>();
    var params = new HashMap<String, String>();
    params.put("maximum", "10");
    params.put("isRegularExpression", "true");
    var csharpRuleWithParams = createMockActiveRule("S1003", CSharpLanguage.REPOSITORY_KEY, params);
    var vbnetRuleWithParams = createMockActiveRule("S1066", VbNetLanguage.REPOSITORY_KEY, params);
    var activeRules = List.of(csharpRuleWithParams, vbnetRuleWithParams);
    var expected = "{\"FileNames\":[],\"ActiveRules\":[{\"RuleId\":\"csharpsquid:S1003\",\"Parameters\":{\"maximum\":\"10\",\"isRegularExpression\":\"true\"}},{\"RuleId\":\"vbnet:S1066\",\"Parameters\":{\"maximum\":\"10\",\"isRegularExpression\":\"true\"}}]}";

    var result = jsonParser.buildBody(fileNames, activeRules);

    assertThat(result).isEqualTo(expected);
  }

  @Test
  void buildBody_withNullCollections_throws() {
    Collection<String> fileNames = null;
    Collection<ActiveRule> activeRules = null;

    assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> jsonParser.buildBody(fileNames, activeRules));
  }

  @Test
  void buildBody_withSpecialCharactersInFileNames_shouldEscapeProperly() {
    var fileNames = Arrays.asList(
      "file with spaces.cs",
      "file\"with\"quotes.cs",
      "file\\with\\backslashes.cs");
    var activeRules = new ArrayList<ActiveRule>();
    var expected = "{\"FileNames\":[\"file with spaces.cs\",\"file\\\"with\\\"quotes.cs\",\"file\\\\with\\\\backslashes.cs\"],\"ActiveRules\":[]}";

    var result = jsonParser.buildBody(fileNames, activeRules);

    assertThat(result).isEqualTo(expected);
    var fileNamesArray = JsonParser.parseString(result).getAsJsonObject().get("FileNames").getAsJsonArray();
    assertThat(fileNamesArray).hasSize(3);
    assertThat(fileNamesArray.get(0).getAsString()).hasToString("file with spaces.cs");
    assertThat(fileNamesArray.get(1).getAsString()).hasToString("file\"with\"quotes.cs");
    assertThat(fileNamesArray.get(2).getAsString()).hasToString("file\\with\\backslashes.cs");
  }

  private ActiveRule createMockActiveRule(String ruleKey, String repositoryKey, Map<String, String> params) {
    ActiveRule activeRule = mock(ActiveRule.class);
    RuleKey rule = mock(RuleKey.class);

    when(rule.toString()).thenReturn(repositoryKey + ":" + ruleKey);
    when(rule.rule()).thenReturn(ruleKey);
    when(activeRule.ruleKey()).thenReturn(rule);
    when(activeRule.params()).thenReturn(params);

    return activeRule;
  }
}
