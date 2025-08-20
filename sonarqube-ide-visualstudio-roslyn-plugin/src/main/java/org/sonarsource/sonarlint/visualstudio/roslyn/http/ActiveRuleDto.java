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

import com.google.gson.annotations.SerializedName;
import java.util.Map;

public class ActiveRuleDto {
  @SerializedName("RuleId")
  private final String ruleId;

  @SerializedName("Parameters")
  private final Map<String, String> parameters;

  public ActiveRuleDto(String ruleId, Map<String, String> parameters) {
    this.ruleId = ruleId;
    this.parameters = parameters;
  }

  public String getRuleId() {
    return ruleId;
  }

  public Map<String, String> getParameters() {
    return parameters;
  }
}
