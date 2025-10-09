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

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sonar.api.batch.rule.ActiveRule;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.config.Settings;
import org.sonar.api.rule.RuleKey;
import org.sonarsource.sonarlint.visualstudio.roslyn.SqvsRoslynPluginPropertyDefinitions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HttpClientHandlerTests {
  private SensorContext sensorContext;
  private JsonRequestBuilder jsonRequestBuilder;
  private HttpClient httpClient;
  private HttpClientHandler underTest;

  @BeforeEach
  void init() {
    sensorContext = mock(SensorContext.class);
    mockSettings("60000", "myToken");
    jsonRequestBuilder = mock(JsonRequestBuilder.class);
    when(jsonRequestBuilder.buildAnalyzeBody(any(), any(), any(), any(), any())).thenReturn("");
    when(jsonRequestBuilder.buildCancelBody(any())).thenReturn("");
    HttpClientProvider httpClientProvider = mock(HttpClientProvider.class);
    httpClient = mock(HttpClient.class);
    when(httpClientProvider.getHttpClient()).thenReturn(httpClient);
    underTest = new HttpClientHandler(sensorContext, jsonRequestBuilder, httpClientProvider);
  }

  @Test
  void createRequest_setsUriAsExpected() {
    var result = underTest.createRequest("", "myuri");

    assertThat(result.uri().toString()).hasToString("http://localhost:60000/myuri");
    assertThat(result.method()).isEqualTo("POST");
    HttpHeaders headers = result.headers();
    assertThat(headers.firstValue("Content-Type").get()).hasToString("application/json");
    assertThat(headers.firstValue("X-Auth-Token").get()).hasToString("myToken");
  }

  @Test
  void sendAnalyzeRequest_callsSerializerWithExpectedParameters() throws IOException, InterruptedException {

    Collection<String> fileNames = List.of("File1.cs", "File2.cs");
    Map<String, String> analysisProperties = Map.of();
    var analyzerInfo = new AnalyzerInfoDto(true, true);
    Collection<ActiveRule> activeRules = List.of(createMockActiveRule("S100"));
    var analysisId = UUID.randomUUID();

    underTest.sendAnalyzeRequest(fileNames, activeRules, analysisProperties, analyzerInfo, analysisId);

    verify(jsonRequestBuilder).buildAnalyzeBody(fileNames, activeRules, analysisProperties, analyzerInfo, analysisId);
    verify(httpClient).send(argThat(httpRequest -> httpRequest.uri().toString().endsWith("/analyze")), any());
  }

  @Test
  void sendCancelRequest_callsSerializerWithExpectedParameters(){
    var analysisId = UUID.randomUUID();

    underTest.sendCancelRequest(analysisId);

    verify(jsonRequestBuilder).buildCancelBody(analysisId);
    verify(httpClient).sendAsync(argThat(httpRequest -> httpRequest.uri().toString().endsWith("/cancel")), any());
  }

  private ActiveRule createMockActiveRule(String ruleId) {
    ActiveRule activeRule = mock(ActiveRule.class);
    RuleKey ruleKey = mock(RuleKey.class);
    when(ruleKey.rule()).thenReturn(ruleId);
    when(activeRule.ruleKey()).thenReturn(ruleKey);

    return activeRule;
  }

  private void mockSettings(String port, String token) {
    var mockSettings = mock(Settings.class);
    when(mockSettings.getString(SqvsRoslynPluginPropertyDefinitions.getServerPort())).thenReturn(port);
    when(mockSettings.getString(SqvsRoslynPluginPropertyDefinitions.getServerToken())).thenReturn(token);
    when(sensorContext.settings()).thenReturn(mockSettings);
  }
}
