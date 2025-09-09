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
                    "FilePath": "test/file/path.cs",
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
                                "FilePath": "flow/file/path.cs",
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
        assertThat(roslynIssue.getPrimaryLocation().getFilePath()).isEqualTo("test/file/path.cs");
        assertThat(roslynIssue.getPrimaryLocation().getMessage()).isEqualTo("Test message");
        assertThat(roslynIssue.getPrimaryLocation().getTextRange().getStartLine()).isEqualTo(10);
        assertThat(roslynIssue.getPrimaryLocation().getTextRange().getEndLine()).isEqualTo(15);
        assertThat(roslynIssue.getPrimaryLocation().getTextRange().getStartLineOffset()).isEqualTo(5);
        assertThat(roslynIssue.getPrimaryLocation().getTextRange().getEndLineOffset()).isEqualTo(20);
        
        assertThat(roslynIssue.getFlows()).hasSize(1);
        assertThat(roslynIssue.getFlows().get(0).getLocations()).hasSize(1);
        assertThat(roslynIssue.getFlows().get(0).getLocations().get(0).getFilePath()).isEqualTo("flow/file/path.cs");
        
        assertThat(roslynIssue.getQuickFixes()).hasSize(1);
        assertThat(roslynIssue.getQuickFixes().get(0).getValue()).isEqualTo("Quick fix value");
    }
}