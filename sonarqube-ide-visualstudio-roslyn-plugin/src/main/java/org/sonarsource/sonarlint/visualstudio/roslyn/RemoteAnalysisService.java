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

import org.sonar.api.batch.rule.ActiveRule;
import org.sonar.api.scanner.ScannerSide;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonarsource.api.sonarlint.SonarLintSide;
import org.sonarsource.sonarlint.visualstudio.roslyn.http.AnalyzerInfoDto;
import org.sonarsource.sonarlint.visualstudio.roslyn.http.HttpAnalysisRequestHandler;
import org.sonarsource.sonarlint.visualstudio.roslyn.protocol.RoslynIssue;

import java.util.Collection;
import java.util.Map;

/**
 * Factory for creating AnalysisManager instances.
 * This class has instance-based scope in SonarLint.
 */
@ScannerSide
@SonarLintSide
public class RemoteAnalysisService {

  private final AnalysisCancellationService analysisCancellationService;
  private final HttpAnalysisRequestHandler httpAnalysisRequestHandler;
  private final SensorContext sensorContext;

  public RemoteAnalysisService(
    AnalysisCancellationService analysisCancellationService,
    HttpAnalysisRequestHandler httpAnalysisRequestHandler,
    SensorContext sensorContext) {
    this.analysisCancellationService = analysisCancellationService;
    this.httpAnalysisRequestHandler = httpAnalysisRequestHandler;
    this.sensorContext = sensorContext;
  }

  public Collection<RoslynIssue> analyze(
    Collection<String> inputFiles,
    Collection<ActiveRule> activeRules,
    Map<String, String> analysisProperties,
    AnalyzerInfoDto analyzerInfo) {
    try (var tracker = new AnalysisTrackerImpl(sensorContext, httpAnalysisRequestHandler, analysisCancellationService)) {
      return httpAnalysisRequestHandler.analyze(inputFiles, activeRules, analysisProperties, analyzerInfo, tracker.getAnalysisId());
    }
  }
}
