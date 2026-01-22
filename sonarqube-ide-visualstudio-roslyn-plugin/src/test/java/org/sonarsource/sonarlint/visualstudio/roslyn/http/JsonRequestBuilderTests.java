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
package org.sonarsource.sonarlint.visualstudio.roslyn.http;

import com.google.gson.JsonParser;

import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
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
  private static final java.util.UUID analysisId = java.util.UUID.fromString("ed89f185-c2d6-4d03-aef1-334747e7fbdb");

  @BeforeEach
  void init() {
    jsonParser = new JsonRequestBuilder();
  }

  @Test
  void buildBody_withEmptyCollections_shouldReturnValidJson() {
    var fileUris = new ArrayList<java.net.URI>();
    var activeRules = new ArrayList<ActiveRule>();
    var analysisProperties = Map.<String, String>of();
    var analyzerInfo = new AnalyzerInfoDto(false, false);
    var expected = "{\"FileUris\":[],\"ActiveRules\":[],\"AnalysisProperties\":{},\"AnalyzerInfo\":{\"ShouldUseCsharpEnterprise\":false,\"ShouldUseVbEnterprise\":false},\"AnalysisId\":\"ed89f185-c2d6-4d03-aef1-334747e7fbdb\"}";

    var result = jsonParser.buildAnalyzeBody(fileUris, activeRules, analysisProperties, analyzerInfo, analysisId);

    assertThat(result).isEqualTo(expected);
  }

  @Test
  void buildBody_withAllParametersFilled_shouldReturnValidJson() {
    var fileUris = List.of(
      java.net.URI.create("file:///C:/project/src/File1.cs"),
      java.net.URI.create("file://localhost/$c/project/src/File2.cs")
    );
    var activeRules = List.of(createMockActiveRule("S100", CSharpLanguage.REPOSITORY_KEY, new HashMap<>()),
      createMockActiveRule("S101", VbNetLanguage.REPOSITORY_KEY, new HashMap<>()));
    var analysisProperties = Map.of("sonar.vb.disableRazor", "false");
    var analyzerInfo = new AnalyzerInfoDto(true, false);
    var expected = "{\"FileUris\":[\"file:///C:/project/src/File1.cs\",\"file://localhost/$c/project/src/File2.cs\"],\"ActiveRules\":[{\"RuleId\":\"csharpsquid:S100\",\"Parameters\":{}},{\"RuleId\":\"vbnet:S101\",\"Parameters\":{}}],\"AnalysisProperties\":{\"sonar.vb.disableRazor\":\"false\"},\"AnalyzerInfo\":{\"ShouldUseCsharpEnterprise\":true,\"ShouldUseVbEnterprise\":false},\"AnalysisId\":\"ed89f185-c2d6-4d03-aef1-334747e7fbdb\"}";

    var result = jsonParser.buildAnalyzeBody(fileUris, activeRules, analysisProperties, analyzerInfo, analysisId);

    assertThat(result).isEqualTo(expected);
  }

  @Test
  void buildBody_withActiveRules_shouldReturnRuleId() {
    var fileUris = new ArrayList<java.net.URI>();
    var activeRule = createMockActiveRule("S100", CSharpLanguage.REPOSITORY_KEY, new HashMap<>());
    var analysisProperties = Map.<String, String>of();
    var analyzerInfo = new AnalyzerInfoDto(false, true);
    var expected = "{\"FileUris\":[],\"ActiveRules\":[{\"RuleId\":\"csharpsquid:S100\",\"Parameters\":{}}],\"AnalysisProperties\":{},\"AnalyzerInfo\":{\"ShouldUseCsharpEnterprise\":false,\"ShouldUseVbEnterprise\":true},\"AnalysisId\":\"ed89f185-c2d6-4d03-aef1-334747e7fbdb\"}";

    var result = jsonParser.buildAnalyzeBody(fileUris, List.of(activeRule), analysisProperties, analyzerInfo, analysisId);

    assertThat(result).isEqualTo(expected);
  }

  @Test
  void buildBody_withActiveRulesWithParams_shouldIncludeParams() {
    var fileUris = new ArrayList<java.net.URI>();
    var analysisProperties = Map.<String, String>of();
    var params = new HashMap<String, String>();
    params.put("maximum", "10");
    params.put("isRegularExpression", "true");
    var csharpRuleWithParams = createMockActiveRule("S1003", CSharpLanguage.REPOSITORY_KEY, params);
    var vbnetRuleWithParams = createMockActiveRule("S1066", VbNetLanguage.REPOSITORY_KEY, params);
    var activeRules = List.of(csharpRuleWithParams, vbnetRuleWithParams);
    var analyzerInfo = new AnalyzerInfoDto(true, true);
    var expected = "{\"FileUris\":[],\"ActiveRules\":[{\"RuleId\":\"csharpsquid:S1003\",\"Parameters\":{\"maximum\":\"10\",\"isRegularExpression\":\"true\"}},{\"RuleId\":\"vbnet:S1066\",\"Parameters\":{\"maximum\":\"10\",\"isRegularExpression\":\"true\"}}],\"AnalysisProperties\":{},\"AnalyzerInfo\":{\"ShouldUseCsharpEnterprise\":true,\"ShouldUseVbEnterprise\":true},\"AnalysisId\":\"ed89f185-c2d6-4d03-aef1-334747e7fbdb\"}";

    var result = jsonParser.buildAnalyzeBody(fileUris, activeRules, analysisProperties, analyzerInfo, analysisId);

    assertThat(result).isEqualTo(expected);
  }

  @Test
  void buildBody_withNullCollections_throws() {
    Collection<java.net.URI> fileUris = null;
    Collection<ActiveRule> activeRules = null;
    Map<String, String> analysisProperties = null;
    AnalyzerInfoDto analyzerInfo = null;

    assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> jsonParser.buildAnalyzeBody(fileUris, activeRules, analysisProperties, analyzerInfo, analysisId));
  }

  @Test
  @DisabledOnOs(OS.LINUX) // Windows file paths in this test are not handled correctly on linux
  void buildBody_withSpecialCharactersInFileUris_shouldEscapeProperly() {
    String pathWithSpaces = "C:\\project\\src\\file with spaces.cs";
    String pathWithCharacters = "C:\\project\\src\\file(with)braces'and'quotes.cs";
    String uncPath = "\\\\server-name\\share-name\\path\\to\\uncfile.cs";
    var fileUris = Arrays.asList(
        Path.of(pathWithSpaces).toUri(),
        Path.of(pathWithCharacters).toUri(),
        Path.of(uncPath).toUri()
    );
    var activeRules = new ArrayList<ActiveRule>();
    var analysisProperties = Map.<String, String>of();
    var analyzerInfo = new AnalyzerInfoDto(false, false);
    var expected = "{\"FileUris\":[\"file:///C:/project/src/file%20with%20spaces.cs\",\"file:///C:/project/src/file(with)braces\\u0027and\\u0027quotes.cs\",\"file://server-name/share-name/path/to/uncfile.cs\"],\"ActiveRules\":[],\"AnalysisProperties\":{},\"AnalyzerInfo\":{\"ShouldUseCsharpEnterprise\":false,\"ShouldUseVbEnterprise\":false},\"AnalysisId\":\"ed89f185-c2d6-4d03-aef1-334747e7fbdb\"}";

    var result = jsonParser.buildAnalyzeBody(fileUris, activeRules, analysisProperties, analyzerInfo, analysisId);

    assertThat(result).isEqualTo(expected);
    var serializedFileUris = JsonParser.parseString(result).getAsJsonObject().get("FileUris").getAsJsonArray();
    assertThat(serializedFileUris).hasSize(3);
    assertThat(URI.create(serializedFileUris.get(0).getAsString())).isEqualTo(fileUris.get(0));
    assertThat(URI.create(serializedFileUris.get(1).getAsString())).isEqualTo(fileUris.get(1));
    assertThat(URI.create(serializedFileUris.get(2).getAsString())).isEqualTo(fileUris.get(2));
  }

  @Test
  void buildBody_withSpecialCharactersInFileUris_containsExpectedEscapeSequencesInJson() {
    String pathWithSpaces = "file with spaces.cs";
    String pathWithCharacters = "file(with)braces'and'quotes.cs";
    var fileUris = Arrays.asList(
        Path.of(pathWithSpaces).toUri(),
        Path.of(pathWithCharacters).toUri()
    );
    var activeRules = new ArrayList<ActiveRule>();
    var analysisProperties = Map.<String, String>of();
    var analyzerInfo = new AnalyzerInfoDto(false, false);

    var result = jsonParser.buildAnalyzeBody(fileUris, activeRules, analysisProperties, analyzerInfo, analysisId);

    assertThat(result).contains("file%20with%20spaces.cs", "file(with)braces\\u0027and\\u0027quotes.cs");
  }

  @Test
  void buildCancelBody_withValidAnalysisId_shouldReturnValidJson() {
    var expected = "{\"AnalysisId\":\"ed89f185-c2d6-4d03-aef1-334747e7fbdb\"}";

    var result = jsonParser.buildCancelBody(analysisId);

    assertThat(result).isEqualTo(expected);
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
