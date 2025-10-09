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
package org.sonarsource.sonarlint.visualstudio.roslyn;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sonar.api.batch.rule.ActiveRule;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonarsource.sonarlint.visualstudio.roslyn.http.AnalyzerInfoDto;
import org.sonarsource.sonarlint.visualstudio.roslyn.http.HttpAnalysisRequestHandler;
import org.sonarsource.sonarlint.visualstudio.roslyn.protocol.RoslynIssue;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RemoteAnalysisServiceTest {

  private AnalysisCancellationService analysisCancellationService;
  private HttpAnalysisRequestHandler httpAnalysisRequestHandler;
  private RemoteAnalysisService underTest;

  private final Collection<String> fileNames = List.of("File1.cs", "File2.cs");
  private final Collection<ActiveRule> activeRules = List.of(mock(ActiveRule.class));
  private final Map<String, String> analysisProperties = Map.of("sonar.cs.disableRazor", "true");
  private final AnalyzerInfoDto analyzerInfo = new AnalyzerInfoDto(false, false);

  @BeforeEach
  void setUp() {
    analysisCancellationService = mock(AnalysisCancellationService.class);
    httpAnalysisRequestHandler = mock(HttpAnalysisRequestHandler.class);
    var sensorContext = mock(SensorContext.class);

    underTest = new RemoteAnalysisService(
      analysisCancellationService,
      httpAnalysisRequestHandler,
      sensorContext);
  }

  @Test
  void analyze_ShouldReturnRoslynIssues() {
    var mockIssues = mockIssues();

    var result = underTest.analyze(
      fileNames,
      activeRules,
      analysisProperties,
      analyzerInfo);

    assertSame(mockIssues, result);
    verify(httpAnalysisRequestHandler).analyze(
      eq(fileNames),
      eq(activeRules),
      eq(analysisProperties),
      eq(analyzerInfo),
      any(UUID.class));
    verify(analysisCancellationService).registerAnalysis(any(AnalysisTrackerImpl.class));
  }

  private List<RoslynIssue> mockIssues() {
    var mockIssue = mock(RoslynIssue.class);
    var expectedIssues = List.of(mockIssue);
    when(httpAnalysisRequestHandler.analyze(
      any(), any(), any(), any(), any(UUID.class)))
        .thenReturn(expectedIssues);
    return expectedIssues;
  }
}
