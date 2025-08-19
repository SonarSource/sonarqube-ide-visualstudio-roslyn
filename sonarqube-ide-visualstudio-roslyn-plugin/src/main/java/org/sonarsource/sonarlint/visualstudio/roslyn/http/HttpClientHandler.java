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
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Collection;
import org.sonar.api.batch.rule.ActiveRule;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.scanner.ScannerSide;
import org.sonarsource.api.sonarlint.SonarLintSide;
import org.sonarsource.sonarlint.visualstudio.roslyn.SqvsRoslynPluginPropertyDefinitions;

@ScannerSide
@SonarLintSide(lifespan = "SINGLE_ANALYSIS")
public class HttpClientHandler {
  private final SensorContext context;
  private final JsonRequestBuilder jsonRequestBuilder;

  public HttpClientHandler(SensorContext context, JsonRequestBuilder jsonRequestBuilder) {
    this.context = context;
    this.jsonRequestBuilder = jsonRequestBuilder;
  }

  public HttpResponse<String> sendRequest(Collection<String> fileNames, Collection<ActiveRule> activeRules) throws IOException, InterruptedException {
    HttpClient client = HttpClient.newHttpClient();
    var jsonPayload = jsonRequestBuilder.buildBody(fileNames, activeRules);
    var request = createRequest(jsonPayload);
    return client.send(request, HttpResponse.BodyHandlers.ofString());
  }

  public HttpRequest createRequest(String jsonPayload) {
    var settings = context.settings();
    var port = settings.getString(SqvsRoslynPluginPropertyDefinitions.getServerPort());
    var token = settings.getString(SqvsRoslynPluginPropertyDefinitions.getServerToken());
    var uri = String.format("http://localhost:%s/analyze", port);
    return HttpRequest.newBuilder()
      .uri(URI.create(uri))
      .header("Content-Type", "application/json")
      .header("X-Auth-Token", token)
      .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
      .build();
  }
}
