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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sonar.api.batch.rule.ActiveRule;
import org.sonar.api.config.Configuration;
import org.sonar.api.rule.RuleKey;

import java.io.IOException;
import java.net.http.HttpHeaders;
import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class HttpClientHandlerTests {
  private Configuration configuration;
  // TODO by https://sonarsource.atlassian.net/browse/SLVS-2470: mock port and token with values from configuration
  private final int port = 60000;
  private final String token = "myToken";

  @BeforeEach
  void init() {
    configuration = mock(Configuration.class);
  }

  @Test
  void createRequest_setsUriAsExpected() {
    var httpClientHandler = new HttpClientHandler(configuration, mock(JsonRequestBuilder.class));

    var result = httpClientHandler.createRequest("");

    assertThat(result.uri().toString()).hasToString(String.format("http://localhost:%d/analyze", port));
    assertThat(result.method()).isEqualTo("POST");
    HttpHeaders headers = result.headers();
    assertThat(headers.firstValue("Content-Type").get()).hasToString("application/json");
    assertThat(headers.firstValue("X-Auth-Token").get()).hasToString(token);
  }

  @Test
  void sendRequest_callsParserWithExpectedParameters() throws IOException, InterruptedException {
    JsonRequestBuilder myMock = mock(JsonRequestBuilder.class);
    Collection<String> fileNames = List.of("File1.cs", "File2.cs");
    Collection<ActiveRule> activeRules = List.of(createMockActiveRule("S100"));
    var httpClientHandler = new HttpClientHandler(configuration, myMock);

    try {
      httpClientHandler.sendRequest(fileNames, activeRules);
    } catch (Exception ex) {
      // expecting request to fail
    }

    verify(myMock).buildBody(fileNames, activeRules);
  }

  private ActiveRule createMockActiveRule(String ruleId) {
    ActiveRule activeRule = mock(ActiveRule.class);
    RuleKey ruleKey = mock(RuleKey.class);
    when(ruleKey.rule()).thenReturn(ruleId);
    when(activeRule.ruleKey()).thenReturn(ruleKey);

    return activeRule;
  }
}
