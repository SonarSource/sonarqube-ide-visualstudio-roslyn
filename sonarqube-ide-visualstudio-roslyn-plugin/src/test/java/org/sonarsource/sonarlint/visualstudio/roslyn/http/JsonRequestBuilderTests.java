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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sonar.api.batch.rule.ActiveRule;
import org.sonar.api.rule.RuleKey;

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
    Collection<String> fileNames = Collections.emptyList();
    Collection<ActiveRule> activeRules = Collections.emptyList();
    var expected = "{\"FileNames\":[],\"ActiveRules\":[]}";

    var result = jsonParser.buildBody(fileNames, activeRules);

    assertThat(result).isEqualTo(expected);
  }

  @Test
  void buildBody_withFileNamesAndActiveRules_shouldReturnValidJson() {
    Collection<String> fileNames = List.of("File1.cs", "File2.cs");
    Collection<ActiveRule> activeRules = List.of(createMockActiveRule("S100", new HashMap<>()), createMockActiveRule("S101", new HashMap<>()));
    var expected = "{\"FileNames\":[\"File1.cs\",\"File2.cs\"],\"ActiveRules\":[{\"RuleKey\":\"S100\",\"Params\":{}},{\"RuleKey\":\"S101\",\"Params\":{}}]}";

    var result = jsonParser.buildBody(fileNames, activeRules);

    assertThat(result).isEqualTo(expected);
  }

  @Test
  void buildBody_withActiveRulesWithParams_shouldIncludeParams() {
    Collection<String> fileNames = Collections.emptyList();
    Map<String, String> params = new HashMap<>();
    params.put("maximum", "10");
    params.put("isRegularExpression", "true");
    ActiveRule ruleWithParams = createMockActiveRule("S1003", params);
    Collection<ActiveRule> activeRules = Collections.singletonList(ruleWithParams);
    var expected = "{\"FileNames\":[],\"ActiveRules\":[{\"RuleKey\":\"S1003\",\"Params\":{\"maximum\":\"10\",\"isRegularExpression\":\"true\"}}]}";

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
    Collection<String> fileNames = Arrays.asList(
      "file with spaces.cs",
      "file\"with\"quotes.cs",
      "file\\with\\backslashes.cs");
    Collection<ActiveRule> activeRules = Collections.emptyList();
    var expected = "{\"FileNames\":[\"file with spaces.cs\",\"file\\\"with\\\"quotes.cs\",\"file\\\\with\\\\backslashes.cs\"],\"ActiveRules\":[]}";

    var result = jsonParser.buildBody(fileNames, activeRules);

    assertThat(result).isEqualTo(expected);
    var fileNamesArray = JsonParser.parseString(result).getAsJsonObject().get("FileNames").getAsJsonArray();
    assertThat(fileNamesArray).hasSize(3);
    assertThat(fileNamesArray.get(0).getAsString()).hasToString("file with spaces.cs");
    assertThat(fileNamesArray.get(1).getAsString()).hasToString("file\"with\"quotes.cs");
    assertThat(fileNamesArray.get(2).getAsString()).hasToString("file\\with\\backslashes.cs");
  }

  private ActiveRule createMockActiveRule(String ruleId, Map<String, String> params) {
    ActiveRule activeRule = mock(ActiveRule.class);
    RuleKey ruleKey = mock(RuleKey.class);

    when(ruleKey.rule()).thenReturn(ruleId);
    when(activeRule.ruleKey()).thenReturn(ruleKey);
    when(activeRule.params()).thenReturn(params);

    return activeRule;
  }
}
