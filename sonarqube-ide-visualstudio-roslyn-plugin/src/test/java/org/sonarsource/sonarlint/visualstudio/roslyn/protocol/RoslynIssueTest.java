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
package org.sonarsource.sonarlint.visualstudio.roslyn.protocol;

import com.google.gson.Gson;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RoslynIssueTest {

    @Test
    void deserializationSmokeTest() {
        String json = """
            {
                "RuleId": "rule:id",
                "PrimaryLocation": {
                    "FileUri": "file:///test/file/path.cs",
                    "Message": "Test message",
                    "TextRange": {
                        "StartLine": 10,
                        "EndLine": 15,
                        "StartLineOffset": 5,
                        "EndLineOffset": 20
                    }
                },
                "Flows": [
                    {
                        "Locations": [
                            {
                                "FileUri": "file:///flow/file/path.cs",
                                "Message": "Flow message",
                                "TextRange": {
                                    "StartLine": 1,
                                    "EndLine": 2,
                                    "StartLineOffset": 3,
                                    "EndLineOffset": 4
                                }
                            }
                        ]
                    }
                ],
                "QuickFixes": [
                    {
                        "Value": "Quick fix value"
                    }
                ]
            }
            """;
        
        var roslynIssue = new Gson().fromJson(json, RoslynIssue.class);

        assertThat(roslynIssue).isNotNull();
        assertThat(roslynIssue.getRuleId()).isEqualTo("rule:id");
        assertThat(roslynIssue.getPrimaryLocation().getFileUri()).hasToString("file:///test/file/path.cs");
        assertThat(roslynIssue.getPrimaryLocation().getMessage()).isEqualTo("Test message");
        assertThat(roslynIssue.getPrimaryLocation().getTextRange().getStartLine()).isEqualTo(10);
        assertThat(roslynIssue.getPrimaryLocation().getTextRange().getEndLine()).isEqualTo(15);
        assertThat(roslynIssue.getPrimaryLocation().getTextRange().getStartLineOffset()).isEqualTo(5);
        assertThat(roslynIssue.getPrimaryLocation().getTextRange().getEndLineOffset()).isEqualTo(20);
        
        assertThat(roslynIssue.getFlows()).hasSize(1);
        assertThat(roslynIssue.getFlows().get(0).getLocations()).hasSize(1);
        assertThat(roslynIssue.getFlows().get(0).getLocations().get(0).getFileUri()).hasToString("file:///flow/file/path.cs");

        assertThat(roslynIssue.getQuickFixes()).hasSize(1);
        assertThat(roslynIssue.getQuickFixes().get(0).getValue()).isEqualTo("Quick fix value");
    }
}