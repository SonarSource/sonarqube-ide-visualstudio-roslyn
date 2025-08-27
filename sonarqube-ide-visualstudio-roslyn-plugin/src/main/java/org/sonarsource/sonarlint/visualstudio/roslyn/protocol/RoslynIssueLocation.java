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

public class RoslynIssueLocation {
  @SerializedName("FilePath")
  private String filePath;

  @SerializedName("Message")
  private String message;

  @SerializedName("TextRange")
  private RoslynIssueTextRange textRange;

  public String getFilePath() {
    return filePath;
  }

  public String getMessage() {
    return message;
  }

  public RoslynIssueTextRange getTextRange() {
    return textRange;
  }

}
