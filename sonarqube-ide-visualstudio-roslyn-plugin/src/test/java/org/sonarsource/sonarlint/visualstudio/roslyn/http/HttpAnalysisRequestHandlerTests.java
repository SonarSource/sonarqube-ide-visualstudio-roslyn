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
  private HttpClientHandler httpClientHandler;
  private HttpAnalysisRequestHandler analysisRequestHandler;
  @RegisterExtension
  private LogTesterJUnit5 logTester = new LogTesterJUnit5();

  @BeforeEach
  void init() {
    httpClientHandler = mock(HttpClientHandler.class);
    analysisRequestHandler = new HttpAnalysisRequestHandler(httpClientHandler);
  }

  @Test
  void analyze_requestSucceeds_ReturnsDiagnostics() throws IOException, InterruptedException {
    mockResponseWithOneDiagnostic(200);

    var result = analysisRequestHandler.analyze(fileNames, activeRules);

    assertThat(result).hasSize(1);
    verify(httpClientHandler).sendRequest(fileNames, activeRules);
  }

  @Test
  void analyze_requestFails_returnsEmptyDiagnostics() throws IOException, InterruptedException {
    mockResponseWithOneDiagnostic(404);

    var result = analysisRequestHandler.analyze(fileNames, activeRules);

    assertThat(result).isEmpty();
    verify(httpClientHandler).sendRequest(fileNames, activeRules);
    assertThat(logTester.logs(LoggerLevel.ERROR)).contains("Response from server is 404.");
  }

  @Test
  void analyze_throws_logsAndReturnsEmptyDiagnostics() throws IOException, InterruptedException {
    var exceptionMessage = "message";
    when(httpClientHandler.sendRequest(fileNames, activeRules)).thenThrow(new RuntimeException(exceptionMessage));

    var thrown = assertThrows(IllegalStateException.class, () -> analysisRequestHandler.analyze(fileNames, activeRules));

    assertThat(thrown).hasMessageContaining("Response crashed due to: " + exceptionMessage);
  }

  private HttpResponse<String> mockResponseWithOneDiagnostic(int statusCode) throws IOException, InterruptedException {
    var mockResponse = mock(HttpResponse.class);
    when(mockResponse.statusCode()).thenReturn(statusCode);
    when(mockResponse.body()).thenReturn("{\"RoslynIssues\":[{\"RuleId\":\"S100\"}]}");
    when(httpClientHandler.sendRequest(fileNames, activeRules)).thenReturn(mockResponse);

    return mockResponse;
  }
}
