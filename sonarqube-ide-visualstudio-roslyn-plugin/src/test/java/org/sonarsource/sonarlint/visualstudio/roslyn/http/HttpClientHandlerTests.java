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
import java.net.http.HttpHeaders;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sonar.api.batch.rule.ActiveRule;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.config.Settings;
import org.sonar.api.rule.RuleKey;
import org.sonarsource.sonarlint.visualstudio.roslyn.SqvsRoslynPluginPropertyDefinitions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HttpClientHandlerTests {
  private SensorContext sensorContext;

  @BeforeEach
  void init() {
    sensorContext = mock(SensorContext.class);
  }

  @Test
  void createRequest_setsUriAsExpected() {
    mockSettings("60000", "myToken");
    var httpClientHandler = new HttpClientHandler(sensorContext, mock(JsonRequestBuilder.class));

    var result = httpClientHandler.createRequest("");

    assertThat(result.uri().toString()).hasToString("http://localhost:60000/analyze");
    assertThat(result.method()).isEqualTo("POST");
    HttpHeaders headers = result.headers();
    assertThat(headers.firstValue("Content-Type").get()).hasToString("application/json");
    assertThat(headers.firstValue("X-Auth-Token").get()).hasToString("myToken");
  }

  @Test
  void sendRequest_callsParserWithExpectedParameters() throws IOException, InterruptedException {
    JsonRequestBuilder myMock = mock(JsonRequestBuilder.class);
    Collection<String> fileNames = List.of("File1.cs", "File2.cs");
    var analyzerInfo = new AnalyzerInfoDto(true, true);
    Collection<ActiveRule> activeRules = List.of(createMockActiveRule("S100"));
    var httpClientHandler = new HttpClientHandler(sensorContext, myMock);

    try {
      httpClientHandler.sendRequest(fileNames, activeRules, analyzerInfo);
    } catch (Exception ex) {
      // expecting request to fail
    }

    verify(myMock).buildBody(fileNames, activeRules, analyzerInfo);
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
