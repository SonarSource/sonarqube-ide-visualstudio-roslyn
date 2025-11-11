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
package org.sonarsource.sonarlint.visualstudio.roslyn;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class AnalysisCancellationServiceTest {
  private AnalysisTracker createMockTrackerWithLatch(CountDownLatch latch) {
    var mockTracker = Mockito.mock(AnalysisTrackerImpl.class);
    when(mockTracker.cancelIfNeeded()).thenAnswer(invocation -> {
      latch.countDown();
      return latch.getCount() == 0;
    });
    return mockTracker;
  }

  @Test
  void testAnalysisPollingWhenCancellationNeeded() throws InterruptedException {
    var service = new AnalysisCancellationService();
    final var latch = new CountDownLatch(3);
    var mockTracker = createMockTrackerWithLatch(latch);

    try {
      service.registerAnalysis(mockTracker);
      assertTrue(latch.await(400, TimeUnit.MILLISECONDS));
      verify(mockTracker, times(3)).cancelIfNeeded();
    } finally {
      service.stop();
    }
  }

  @Test
  void testMultipleAnalysisRegistration() throws InterruptedException {
    var service = new AnalysisCancellationService();

    final var latch1 = new CountDownLatch(1);
    final var latch2 = new CountDownLatch(1);

    var mockTracker1 = createMockTrackerWithLatch(latch1);
    var mockTracker2 = createMockTrackerWithLatch(latch2);

    try {
      service.registerAnalysis(mockTracker1);
      service.registerAnalysis(mockTracker2);

      assertTrue(latch1.await(200, TimeUnit.MILLISECONDS));
      assertTrue(latch2.await(200, TimeUnit.MILLISECONDS));

      verify(mockTracker1, atLeastOnce()).cancelIfNeeded();
      verify(mockTracker2, atLeastOnce()).cancelIfNeeded();
    } finally {
      service.stop();
    }
  }

  @Test
  void testRegisterAnalysisAfterStop() throws InterruptedException {
    var service = new AnalysisCancellationService();
    service.stop();
    final var latch = new CountDownLatch(Integer.MAX_VALUE);
    var mockTracker = createMockTrackerWithLatch(latch);

    assertThrows(RejectedExecutionException.class, () -> service.registerAnalysis(mockTracker));
    assertFalse(latch.await(300, TimeUnit.MILLISECONDS));
  }
}
