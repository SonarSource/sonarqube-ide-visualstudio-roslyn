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

import com.google.gson.Gson;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;

import org.sonar.api.batch.rule.ActiveRule;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonarsource.api.sonarlint.SonarLintSide;
import org.sonarsource.sonarlint.visualstudio.roslyn.protocol.RoslynIssue;

@SonarLintSide
public class HttpAnalysisRequestHandler {
  private static final Logger LOG = Loggers.get(HttpAnalysisRequestHandler.class);
  private final HttpClientHandler httpClientHandler;

  public HttpAnalysisRequestHandler(HttpClientHandler httpClientHandler) {
    this.httpClientHandler = httpClientHandler;
  }

  public Collection<RoslynIssue> analyze(
    Collection<URI> fileUris,
    Collection<ActiveRule> activeRules,
    Map<String, String> analysisProperties,
    AnalyzerInfoDto analyzerInfo,
    UUID analysisId) {
    Collection<RoslynIssue> roslynIssues = new ArrayList<>();
    try {
      var response = httpClientHandler.sendAnalyzeRequest(fileUris, activeRules, analysisProperties, analyzerInfo, analysisId);
      if (response.statusCode() != HttpURLConnection.HTTP_OK) {
        LOG.error("Response from server is {}.", response.statusCode());
        return roslynIssues;
      }

      var responseDto = new Gson().fromJson(response.body(), AnalysisResponseDto.class);
      if (responseDto == null) {
        LOG.warn("No body received from the server.");
        return roslynIssues;
      }

      roslynIssues = responseDto.roslynIssues();
    } catch (InterruptedException e) {
      LOG.debug("Interrupted!", e);
      Thread.currentThread().interrupt();
    } catch (Exception e) {
      throw new IllegalStateException("Response crashed due to: " + e.getMessage(), e.fillInStackTrace());
    }

    return roslynIssues;
  }

  public void cancelAnalysis(UUID analysisId) {
    var requestFuture = httpClientHandler.sendCancelRequest(analysisId);

    requestFuture.exceptionally(e -> {
      LOG.error("Failed to cancel analysis due to: {}", e.getMessage(), e);
      return null;
    }).thenApply(response -> {
      if (response != null && response.statusCode() != HttpURLConnection.HTTP_OK) {
        LOG.error("Response from cancel request is {}.", response.statusCode());
      }
      return null;
    });
  }
}
