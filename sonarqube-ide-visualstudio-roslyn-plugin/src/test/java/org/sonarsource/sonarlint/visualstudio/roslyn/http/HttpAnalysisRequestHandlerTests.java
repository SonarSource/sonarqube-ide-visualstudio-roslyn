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
import java.net.URI;
import java.net.http.HttpResponse;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

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

  private final Collection<URI> fileUris = List.of(
      URI.create("file:///C:/project/src/File1.cs"),
      URI.create("file://localhost/$c/project/src/File2.cs")
  );

  private final Collection<ActiveRule> activeRules = List.of(mock(ActiveRule.class));
  private final Map<String, String> analysisProperties = Map.of("sonar.cs.disableRazor", "true");
  private final AnalyzerInfoDto analyzerInfo = new AnalyzerInfoDto(false, false);
  private final UUID analysisId = UUID.randomUUID();
  @RegisterExtension
  private final LogTesterJUnit5 logTester = new LogTesterJUnit5();
  private HttpClientHandler httpClientHandler;
  private HttpAnalysisRequestHandler analysisRequestHandler;

  @BeforeEach
  void init() {
    httpClientHandler = mock(HttpClientHandler.class);
    analysisRequestHandler = new HttpAnalysisRequestHandler(httpClientHandler);
  }

  @Test
  void analyze_requestSucceeds_ReturnsIssues() throws IOException, InterruptedException {
    mockResponseWithOneIssue(200);

    var result = analysisRequestHandler.analyze(fileUris, activeRules, analysisProperties, analyzerInfo, analysisId);

    assertThat(result).hasSize(1);
    verify(httpClientHandler).sendAnalyzeRequest(fileUris, activeRules, analysisProperties, analyzerInfo, analysisId);
  }

  @Test
  void analyze_requestSucceedsWithEmptyBody_logsAndReturnsEmptyIssues() throws IOException, InterruptedException {
    mockResponse(200, "");

    var result = analysisRequestHandler.analyze(fileUris, activeRules, analysisProperties, analyzerInfo, analysisId);

    assertThat(result).isEmpty();
    assertThat(logTester.logs(LoggerLevel.WARN)).contains("No body received from the server.");
  }

  @Test
  void analyze_requestFails_returnsEmptyIssues() throws IOException, InterruptedException {
    mockResponseWithOneIssue(404);

    var result = analysisRequestHandler.analyze(fileUris, activeRules, analysisProperties, analyzerInfo, analysisId);

    assertThat(result).isEmpty();
    verify(httpClientHandler).sendAnalyzeRequest(fileUris, activeRules, analysisProperties, analyzerInfo, analysisId);
    assertThat(logTester.logs(LoggerLevel.ERROR)).contains("Response from server is 404.");
  }

  @Test
  void analyze_throws_logsAndReturnsEmptyIssues() throws IOException, InterruptedException {
    var exceptionMessage = "message";
    when(httpClientHandler.sendAnalyzeRequest(fileUris, activeRules, analysisProperties, analyzerInfo, analysisId)).thenThrow(new RuntimeException(exceptionMessage));

    var thrown = assertThrows(IllegalStateException.class, () -> analysisRequestHandler.analyze(fileUris, activeRules, analysisProperties, analyzerInfo, analysisId));

    assertThat(thrown).hasMessageContaining("Response crashed due to: " + exceptionMessage);
  }

  @Test
  void cancelAnalysis_shouldSendCancelRequest() {
    HttpResponse<Void> mockResponse = mock(HttpResponse.class);
    when(mockResponse.statusCode()).thenReturn(200);
    CompletableFuture<HttpResponse<Void>> future = CompletableFuture.completedFuture(mockResponse);
    when(httpClientHandler.sendCancelRequest(analysisId)).thenReturn(future);

    analysisRequestHandler.cancelAnalysis(analysisId);

    verify(httpClientHandler).sendCancelRequest(analysisId);
  }

  @Test
  void cancelAnalysis_shouldHandleExceptions() {
    var exceptionMessage = "Connection error";
    CompletableFuture<HttpResponse<Void>> future = new CompletableFuture<>();
    future.completeExceptionally(new RuntimeException(exceptionMessage));
    when(httpClientHandler.sendCancelRequest(analysisId)).thenReturn(future);

    analysisRequestHandler.cancelAnalysis(analysisId);

    verify(httpClientHandler).sendCancelRequest(analysisId);
    assertThat(logTester.logs(LoggerLevel.ERROR)).contains("Failed to cancel analysis due to: " + exceptionMessage);
  }

  @Test
  void cancelAnalysis_shouldHandleNonOkStatusCode() {
    HttpResponse<Void> mockResponse = mock(HttpResponse.class);
    when(mockResponse.statusCode()).thenReturn(404);
    CompletableFuture<HttpResponse<Void>> future = CompletableFuture.completedFuture(mockResponse);
    when(httpClientHandler.sendCancelRequest(analysisId)).thenReturn(future);

    analysisRequestHandler.cancelAnalysis(analysisId);

    verify(httpClientHandler).sendCancelRequest(analysisId);
    assertThat(logTester.logs(LoggerLevel.ERROR)).contains("Response from cancel request is 404.");
  }

  private void mockResponseWithOneIssue(int statusCode) throws IOException, InterruptedException {
    mockResponse(statusCode, "{\"RoslynIssues\":[{\"RuleId\":\"S100\"}]}");
  }

  private void mockResponse(int statusCode, String body) throws IOException, InterruptedException {
    var mockResponse = mock(HttpResponse.class);
    when(mockResponse.statusCode()).thenReturn(statusCode);
    when(mockResponse.body()).thenReturn(body);
    when(httpClientHandler.sendAnalyzeRequest(fileUris, activeRules, analysisProperties, analyzerInfo, analysisId)).thenReturn(mockResponse);
  }
}
