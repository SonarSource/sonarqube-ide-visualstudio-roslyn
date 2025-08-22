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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;
import org.sonar.api.batch.fs.InputComponent;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.TextRange;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.batch.rule.Severity;
import org.sonar.api.batch.rule.internal.ActiveRulesBuilder;
import org.sonar.api.batch.rule.internal.NewActiveRule;
import org.sonar.api.batch.sensor.internal.DefaultSensorDescriptor;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.batch.sensor.issue.NewIssue;
import org.sonar.api.batch.sensor.issue.NewIssueLocation;
import org.sonar.api.batch.sensor.issue.NewMessageFormatting;
import org.sonar.api.batch.sensor.issue.fix.NewInputFileEdit;
import org.sonar.api.batch.sensor.issue.fix.NewQuickFix;
import org.sonar.api.batch.sensor.issue.fix.NewTextEdit;
import org.sonar.api.issue.impact.SoftwareQuality;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.testfixtures.log.LogTesterJUnit5;
import org.sonarsource.sonarlint.visualstudio.roslyn.http.HttpAnalysisRequestHandler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class SqvsRoslynSensorTests {
  @RegisterExtension
  LogTesterJUnit5 logTester = new LogTesterJUnit5();
  private HttpAnalysisRequestHandler analysisRequestHandler;
  private SensorContextTester sensorContext;
  private SqvsRoslynSensor underTest;
  private Path baseDir;

  @BeforeEach
  void prepare(@TempDir Path tmp) throws Exception {
    analysisRequestHandler = mock(HttpAnalysisRequestHandler.class);
    baseDir = tmp.toRealPath();
    sensorContext = SensorContextTester.create(baseDir);
    underTest = new SqvsRoslynSensor(analysisRequestHandler);
  }

  @Test
  void describe() {
    DefaultSensorDescriptor descriptor = new DefaultSensorDescriptor();

    underTest.describe(descriptor);

    assertThat(descriptor.name()).isEqualTo("SQVS-Roslyn");
    assertThat(descriptor.languages()).contains(CSharpLanguage.LANGUAGE_KEY, VbNetLanguage.LANGUAGE_KEY);
    assertThat(descriptor.ruleRepositories()).contains(CSharpLanguage.REPOSITORY_KEY, VbNetLanguage.REPOSITORY_KEY);
  }

  @Test
  void noopIfNoFiles() {
    underTest.execute(sensorContext);

    verifyNoInteractions(analysisRequestHandler);
  }

  @Test
  void analyzeCsFile_callsHttpRequestWithCorrectParameters() throws Exception {
    var fileName = "Foo.cs";
    mockInputFile(sensorContext, fileName, "Console.WriteLine(\"Hello World!\");");
    sensorContext.setActiveRules(new ActiveRulesBuilder()
      .addRule(new NewActiveRule.Builder().setRuleKey(RuleKey.of("foo", "bar")).build())
      .addRule(new NewActiveRule.Builder().setRuleKey(RuleKey.of(CSharpLanguage.REPOSITORY_KEY, "S123")).build())
      .build());

    underTest.execute(sensorContext);

    verify(analysisRequestHandler).analyze(argThat(fileNames -> fileNames.stream().anyMatch(file -> file.contains(fileName))),
      argThat(activeRules -> activeRules.size() == 1 && activeRules.stream().findFirst().get().ruleKey().rule().equals("S123")));
  }

  @Test
  void analyzeVbNetFile_callsHttpRequestWithCorrectParameters() throws Exception {
    var fileName = "Foo.vb";
    mockInputFile(sensorContext, fileName, "Console.WriteLine(\"Hello World!\");");
    sensorContext.setActiveRules(new ActiveRulesBuilder()
      .addRule(new NewActiveRule.Builder().setRuleKey(RuleKey.of("foo", "bar")).build())
      .addRule(new NewActiveRule.Builder().setRuleKey(RuleKey.of(VbNetLanguage.REPOSITORY_KEY, "S456")).build())
      .build());

    underTest.execute(sensorContext);

    verify(analysisRequestHandler).analyze(argThat(fileNames -> fileNames.stream().anyMatch(file -> file.contains(fileName))),
      argThat(activeRules -> activeRules.size() == 1 && activeRules.stream().findFirst().get().ruleKey().rule().equals("S456")));
  }

  @Test
  void analyzeCsAndVbNetFiles_callsHttpRequestWithCorrectParameters() throws Exception {
    mockInputFiles(sensorContext, "foo.cs", "boo.vb");
    sensorContext.setActiveRules(new ActiveRulesBuilder()
      .addRule(new NewActiveRule.Builder().setRuleKey(RuleKey.of("foo", "bar")).build())
      .addRule(new NewActiveRule.Builder().setRuleKey(RuleKey.of(CSharpLanguage.REPOSITORY_KEY, "S123")).build())
      .addRule(new NewActiveRule.Builder().setRuleKey(RuleKey.of(VbNetLanguage.REPOSITORY_KEY, "S456")).build())
      .build());

    underTest.execute(sensorContext);

    verify(analysisRequestHandler).analyze(argThat(fileNames -> fileNames.size() == 2 &&
        fileNames.stream().anyMatch(file -> file.contains("foo.cs") || file.contains("boo.vb"))),
      argThat(activeRules -> activeRules.size() == 2 &&
        activeRules.stream().anyMatch(rule -> rule.ruleKey().rule().equals("S123") || rule.ruleKey().rule().contains("S456"))));
  }

  private void mockInputFiles(SensorContextTester sensorContextTester, String... fileNames) throws IOException {
    for (var fileName: fileNames)
    {
      mockInputFile(sensorContextTester, fileName, "some content");
    }
  }

  private void mockInputFile(SensorContextTester sensorContextTester, String fileName, String content) throws IOException {
    var file = createInputFile(fileName, content);
    sensorContextTester.fileSystem().add(file);
  }

  private InputFile createInputFile(String fileName, String content) throws IOException {
    Path filePath = baseDir.resolve(fileName);
    Files.write(filePath, content.getBytes(StandardCharsets.UTF_8));
    var languageKey = fileName.endsWith(".vb") ? VbNetLanguage.LANGUAGE_KEY : CSharpLanguage.LANGUAGE_KEY;

    return TestInputFileBuilder.create("", filePath.toString())
      .setModuleBaseDir(baseDir)
      .setLanguage(languageKey)
      .setCharset(StandardCharsets.UTF_8)
      .build();
  }

  private static class MockSonarLintIssue implements NewIssue {
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
      return new MockIssueLocation();
    }

    @Override
    public void save() {
      // no op
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

    private static class MockSonarLintQuickFix implements NewQuickFix {
      private final List<MockSonarLintInputFileEdit> inputFileEdits = new ArrayList<>();
      private String message;

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

      private static class MockSonarLintInputFileEdit implements NewInputFileEdit {
        private final List<MockSonarLintTextEdit> textEdits = new ArrayList<>();
        private InputFile inputFile;

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

        private static class MockSonarLintTextEdit implements NewTextEdit {
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

  private static class MockIssueLocation implements NewIssueLocation {

    @Override
    public NewIssueLocation on(InputComponent inputComponent) {
      return this;
    }

    @Override
    public NewIssueLocation at(TextRange textRange) {
      return this;
    }

    @Override
    public NewIssueLocation message(String s) {
      return this;
    }

    @Override
    public NewIssueLocation message(String message, List<NewMessageFormatting> newMessageFormatting) {
      return this;
    }

    @Override
    public NewMessageFormatting newMessageFormatting() {
      throw new UnsupportedOperationException();
    }
  }

}
