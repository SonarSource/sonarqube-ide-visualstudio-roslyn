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
import java.net.http.HttpResponse;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.api.batch.rule.ActiveRule;
import org.sonar.api.testfixtures.log.LogTesterJUnit5;
import org.sonar.api.utils.log.LoggerLevel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HttpAnalysisRequestHandlerTests {

  private final Collection<String> fileNames = List.of("File1.cs", "File2.cs");
  private final Collection<ActiveRule> activeRules = List.of(mock(ActiveRule.class));
  private final AnalyzerInfoDto analyzerInfo = new AnalyzerInfoDto(false, false);
  private HttpClientHandler httpClientHandler;
  private HttpAnalysisRequestHandler analysisRequestHandler;
  @RegisterExtension
  private final LogTesterJUnit5 logTester = new LogTesterJUnit5();

  @BeforeEach
  void init() {
    httpClientHandler = mock(HttpClientHandler.class);
    analysisRequestHandler = new HttpAnalysisRequestHandler(httpClientHandler);
  }

  @Test
  void analyze_requestSucceeds_ReturnsIssues() throws IOException, InterruptedException {
    mockResponseWithOneIssue(200);

    var result = analysisRequestHandler.analyze(fileNames, activeRules, analyzerInfo);

    assertThat(result).hasSize(1);
    verify(httpClientHandler).sendRequest(fileNames, activeRules, analyzerInfo);
  }

  @Test
  void analyze_requestSucceedsWithEmptyBody_logsAndReturnsEmptyIssues() throws IOException, InterruptedException {
    mockResponse(200, "");

    var result = analysisRequestHandler.analyze(fileNames, activeRules, analyzerInfo);

    assertThat(result).isEmpty();
    assertThat(logTester.logs(LoggerLevel.WARN)).contains("No body received from the server.");
  }

  @Test
  void analyze_requestFails_returnsEmptyIssues() throws IOException, InterruptedException {
    mockResponseWithOneIssue(404);

    var result = analysisRequestHandler.analyze(fileNames, activeRules, analyzerInfo);

    assertThat(result).isEmpty();
    verify(httpClientHandler).sendRequest(fileNames, activeRules, analyzerInfo);
    assertThat(logTester.logs(LoggerLevel.ERROR)).contains("Response from server is 404.");
  }

  @Test
  void analyze_throws_logsAndReturnsEmptyIssues() throws IOException, InterruptedException {
    var exceptionMessage = "message";
    when(httpClientHandler.sendRequest(fileNames, activeRules, analyzerInfo)).thenThrow(new RuntimeException(exceptionMessage));

    var thrown = assertThrows(IllegalStateException.class, () -> analysisRequestHandler.analyze(fileNames, activeRules, analyzerInfo));

    assertThat(thrown).hasMessageContaining("Response crashed due to: " + exceptionMessage);
  }

  private void mockResponseWithOneIssue(int statusCode) throws IOException, InterruptedException {
    mockResponse(statusCode, "{\"RoslynIssues\":[{\"RuleId\":\"S100\"}]}");
  }

  private void mockResponse(int statusCode, String body) throws IOException, InterruptedException {
    var mockResponse = mock(HttpResponse.class);
    when(mockResponse.statusCode()).thenReturn(statusCode);
    when(mockResponse.body()).thenReturn(body);
    when(httpClientHandler.sendRequest(fileNames, activeRules, analyzerInfo)).thenReturn(mockResponse);
  }
}
