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
import org.sonar.api.batch.rule.ActiveRule;
import org.sonar.api.scanner.ScannerSide;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonarsource.api.sonarlint.SonarLintSide;
import org.sonarsource.sonarlint.visualstudio.roslyn.protocol.Diagnostic;

import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@ScannerSide
@SonarLintSide(lifespan = "INSTANCE")
public class HttpAnalysisRequestHandler {
  private static final Logger LOG = Loggers.get(HttpAnalysisRequestHandler.class);
  private HttpClientHandler httpClientFactory;

  public HttpAnalysisRequestHandler(HttpClientHandler httpClientFactory) {
    this.httpClientFactory = httpClientFactory;
  }

  public Collection<Diagnostic> analyze(Collection<String> fileNames, Collection<ActiveRule> activeRules) {
    Collection<Diagnostic> diagnostics = new ArrayList<>();
    try {
      var response = httpClientFactory.sendRequest(fileNames, activeRules);
      if (response.statusCode() != HttpURLConnection.HTTP_OK) {
        LOG.error("Response from server is {}.", response.statusCode());
        return diagnostics;
      }

      var responseDto = new Gson().fromJson(response.body(), AnalysisResponseDto.class);
      if (responseDto != null) {
        diagnostics = List.of(responseDto.diagnostics());
        // TODO by https://sonarsource.atlassian.net/browse/SLVS-2470: remove log that is here only for testing purposes
        LOG.info("sqvs-roslyn: received diagnostics {}.", diagnostics.stream().count());
      }
    } catch (InterruptedException e) {
      LOG.debug("Interrupted!", e);
      Thread.currentThread().interrupt();
    } catch (Exception e) {
      throw new IllegalStateException("Response crashed due to: " + e.getMessage(), e.fillInStackTrace());
    }

    return diagnostics;
  }
}
