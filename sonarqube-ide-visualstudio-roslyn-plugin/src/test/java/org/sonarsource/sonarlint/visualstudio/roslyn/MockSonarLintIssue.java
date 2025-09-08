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

import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.TextRange;
import org.sonar.api.batch.rule.Severity;
import org.sonar.api.batch.sensor.issue.NewIssue;
import org.sonar.api.batch.sensor.issue.NewIssueLocation;
import org.sonar.api.batch.sensor.issue.fix.NewInputFileEdit;
import org.sonar.api.batch.sensor.issue.fix.NewQuickFix;
import org.sonar.api.batch.sensor.issue.fix.NewTextEdit;
import org.sonar.api.batch.sensor.issue.internal.DefaultIssueLocation;
import org.sonar.api.issue.impact.SoftwareQuality;
import org.sonar.api.rule.RuleKey;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

// copied from https://github.com/SonarSource/sonarlint-omnisharp/blob/3c584bb30ff7bece45e7399699befa30c58b3605/omnisharp-plugin/src/test/java/org/sonarsource/sonarlint/omnisharp/OmnisharpSensorTests.java#L562
public class MockSonarLintIssue implements NewIssue {

    private final List<MockSonarLintQuickFix> quickFixes = new ArrayList<>();

    @Override
    public NewIssue forRule(RuleKey ruleKey) {
        return this;
    }

    @Override
    public NewIssue gap(@Nullable Double aDouble) {
        return this;
    }

    @Override
    public NewIssue overrideSeverity(@Nullable Severity severity) {
        return this;
    }

    @Override
    public NewIssue overrideImpact(SoftwareQuality softwareQuality, org.sonar.api.issue.impact.Severity severity) {
        return null;
    }

    @Override
    public NewIssue at(NewIssueLocation newIssueLocation) {
        return this;
    }

    @Override
    public NewIssue addLocation(NewIssueLocation newIssueLocation) {
        return this;
    }

    @Override
    public NewIssue setQuickFixAvailable(boolean b) {
        return this;
    }

    @Override
    public NewIssue addFlow(Iterable<NewIssueLocation> iterable) {
        return this;
    }

    @Override
    public NewIssue addFlow(Iterable<NewIssueLocation> iterable, NewIssue.FlowType flowType, @Nullable String s) {
        return this;
    }

    @Override
    public NewIssueLocation newLocation() {
        return new DefaultIssueLocation();
    }

    @Override
    public void save() {
        // do nothing
    }

    @Override
    public NewIssue setRuleDescriptionContextKey(@Nullable String s) {
        return this;
    }

    @Override
    public NewIssue setCodeVariants(@Nullable Iterable<String> iterable) {
        return null;
    }

    @Override
    public MockSonarLintQuickFix newQuickFix() {
        return new MockSonarLintQuickFix();
    }

    @Override
    public NewIssue addQuickFix(org.sonar.api.batch.sensor.issue.fix.NewQuickFix newQuickFix) {
        quickFixes.add((MockSonarLintQuickFix) newQuickFix);
        return this;
    }

    public List<MockSonarLintQuickFix> getQuickFixes() {
        return quickFixes;
    }


    public static class MockSonarLintQuickFix implements NewQuickFix {
        private String message;
        private final List<MockSonarLintInputFileEdit> inputFileEdits = new ArrayList<>();

        @Override
        public NewQuickFix message(String message) {
            this.message = message;
            return this;
        }

        @Override
        public NewInputFileEdit newInputFileEdit() {
            return new MockSonarLintInputFileEdit();
        }

        @Override
        public NewQuickFix addInputFileEdit(NewInputFileEdit newInputFileEdit) {
            inputFileEdits.add((MockSonarLintInputFileEdit) newInputFileEdit);
            return this;
        }

        public String getMessage() {
            return message;
        }

        public List<MockSonarLintInputFileEdit> getInputFileEdits() {
            return inputFileEdits;
        }

        public static class MockSonarLintInputFileEdit implements NewInputFileEdit {
            private InputFile inputFile;
            private final List<MockSonarLintTextEdit> textEdits = new ArrayList<>();

            @Override
            public NewInputFileEdit on(InputFile inputFile) {
                this.inputFile = inputFile;
                return this;
            }

            @Override
            public NewTextEdit newTextEdit() {
                return new MockSonarLintTextEdit();
            }

            @Override
            public NewInputFileEdit addTextEdit(NewTextEdit newTextEdit) {
                textEdits.add((MockSonarLintTextEdit) newTextEdit);
                return this;
            }

            public InputFile getInputFile() {
                return inputFile;
            }

            public List<MockSonarLintTextEdit> getTextEdits() {
                return textEdits;
            }

            public static class MockSonarLintTextEdit implements NewTextEdit {
                private TextRange textRange;
                private String newText;

                @Override
                public NewTextEdit at(TextRange textRange) {
                    this.textRange = textRange;
                    return this;
                }

                @Override
                public NewTextEdit withNewText(String newText) {
                    this.newText = newText;
                    return this;
                }

                public TextRange getTextRange() {
                    return textRange;
                }

                public String getNewText() {
                    return newText;
                }
            }
        }
    }
}
