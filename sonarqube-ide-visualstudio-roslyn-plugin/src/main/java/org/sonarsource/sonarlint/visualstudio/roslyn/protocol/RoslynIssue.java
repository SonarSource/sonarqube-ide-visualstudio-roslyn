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
package org.sonarsource.sonarlint.visualstudio.roslyn.protocol;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class RoslynIssue {
  @SerializedName("RuleId")
  private String ruleId;

  @SerializedName("PrimaryLocation")
  private RoslynIssueLocation primaryLocation;

  @SerializedName("Flows")
  private List<RoslynIssueFlow> flows;

  @SerializedName("QuickFixes")
  private List<RoslynIssueQuickFix> quickFixes;

  public String getRuleId() {
    return ruleId;
  }

  public RoslynIssueLocation getPrimaryLocation() {
    return primaryLocation;
  }

  public List<RoslynIssueFlow> getFlows() {
    return flows;
  }

  public List<RoslynIssueQuickFix> getQuickFixes() {
    return quickFixes;
  }
}
