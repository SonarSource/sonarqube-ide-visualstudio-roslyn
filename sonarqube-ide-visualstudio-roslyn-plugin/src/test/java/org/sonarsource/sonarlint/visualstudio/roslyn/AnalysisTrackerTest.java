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

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonarsource.sonarlint.visualstudio.roslyn.http.HttpAnalysisRequestHandler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class AnalysisTrackerTest {
  private SensorContext sensorContext;
  private HttpAnalysisRequestHandler handler;
  private AnalysisCancellationService analysisCancellationService;
  private AnalysisTrackerImpl underTest;

  @BeforeEach
  void setUp() {
    sensorContext = mock(SensorContext.class);
    handler = mock(HttpAnalysisRequestHandler.class);
    analysisCancellationService = mock(AnalysisCancellationService.class);
    underTest = new AnalysisTrackerImpl(sensorContext, handler, analysisCancellationService);
  }

  @Test
  void shouldRegisterWithCancellationService() {
    verify(analysisCancellationService).registerAnalysis(underTest);
  }

  @Test
  void shouldGenerateRandomAnalysisId() {
    UUID analysisId = underTest.getAnalysisId();
    assertThat(analysisId).isNotNull();
  }

  @Test
  void cancelIfNeeded_shouldReturnTrueWhenAlreadyCompleted() {
    underTest.close();

    boolean result = underTest.cancelIfNeeded();

    assertThat(result).isTrue();
    verifyNoInteractions(handler);
    verifyNoInteractions(sensorContext);
  }

  @Test
  void cancelIfNeeded_shouldReturnTrueWhenSensorContextIsCancelled() {
    when(sensorContext.isCancelled()).thenReturn(true);

    boolean result = underTest.cancelIfNeeded();

    assertThat(result).isTrue();
    verify(handler).cancelAnalysis(underTest.getAnalysisId());
  }

  @Test
  void cancelIfNeeded_shouldReturnFalseWhenAnalysisIsNotCancelled() {
    when(sensorContext.isCancelled()).thenReturn(false);

    boolean result = underTest.cancelIfNeeded();

    assertThat(result).isFalse();
    verifyNoInteractions(handler);
  }

  @Test
  void cancelIfNeeded_shouldOnlyCancelOnce() {
    when(sensorContext.isCancelled()).thenReturn(true);

    boolean firstResult = underTest.cancelIfNeeded();
    assertThat(firstResult).isTrue();
    verify(handler).cancelAnalysis(underTest.getAnalysisId());

    boolean secondResult = underTest.cancelIfNeeded();
    assertThat(secondResult).isTrue();
    verify(handler, times(1)).cancelAnalysis(any());
  }
}
