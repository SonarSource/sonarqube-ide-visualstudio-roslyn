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
import java.util.Arrays;
import java.util.List;
import javax.annotation.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.TextRange;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.batch.rule.internal.ActiveRulesBuilder;
import org.sonar.api.batch.rule.internal.NewActiveRule;
import org.sonar.api.batch.sensor.internal.DefaultSensorDescriptor;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.batch.sensor.issue.Issue;
import org.sonar.api.batch.sensor.issue.IssueLocation;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.testfixtures.log.LogTesterJUnit5;
import org.sonarsource.sonarlint.visualstudio.roslyn.http.HttpAnalysisRequestHandler;
import org.sonarsource.sonarlint.visualstudio.roslyn.protocol.RoslynIssue;
import org.sonarsource.sonarlint.visualstudio.roslyn.protocol.RoslynIssueFlow;
import org.sonarsource.sonarlint.visualstudio.roslyn.protocol.RoslynIssueLocation;
import org.sonarsource.sonarlint.visualstudio.roslyn.protocol.RoslynIssueTextRange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class SqvsRoslynSensorTests {
  private final NewActiveRule csActiveRule = new NewActiveRule.Builder().setRuleKey(RuleKey.of(CSharpLanguage.REPOSITORY_KEY, "S123")).build();
  private final NewActiveRule vbActiveRule = new NewActiveRule.Builder().setRuleKey(RuleKey.of(VbNetLanguage.REPOSITORY_KEY, "S456")).build();
  @RegisterExtension
  LogTesterJUnit5 logTester = new LogTesterJUnit5();
  private HttpAnalysisRequestHandler analysisRequestHandler;
  private InstanceConfigurationProvider instanceConfigurationProvider;
  private SensorContextTester sensorContext;
  private SqvsRoslynSensor underTest;
  private Path baseDir;
  private InputFile csFile;
  private InputFile csFile2;
  private InputFile vbFile;
  private InputFile vbFile2;
  private RoslynIssue csharpIssue;
  private RoslynIssue vbIssue;

  @BeforeEach
  void prepare(@TempDir Path tmp) throws Exception {
    analysisRequestHandler = mock(HttpAnalysisRequestHandler.class);
    instanceConfigurationProvider = mock(InstanceConfigurationProvider.class);
    baseDir = tmp.toRealPath();
    sensorContext = SensorContextTester.create(baseDir);
    underTest = new SqvsRoslynSensor(analysisRequestHandler, instanceConfigurationProvider);
    csFile = createInputFile("foo.cs", "var a=1;");
    csFile2 = createInputFile("foo2.cs", "var b=2;");
    vbFile = createInputFile("boo.vb", "Dim a As Integer = 1");
    vbFile2 = createInputFile("boo2.vb", "Dim b As Integer = 2");
    csharpIssue = mockRoslynIssue("S123", CSharpLanguage.REPOSITORY_KEY, csFile.filename());
    vbIssue = mockRoslynIssue("S456", VbNetLanguage.REPOSITORY_KEY, vbFile.filename());
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

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void analyzeCsFile_callsHttpRequestWithCorrectParameters(boolean expectedShouldUseCsharpEnterprise) throws Exception {
    var fileName = "Foo.cs";
    mockInputFile(sensorContext, fileName, "Console.WriteLine(\"Hello World!\");");
    sensorContext.setActiveRules(new ActiveRulesBuilder()
      .addRule(new NewActiveRule.Builder().setRuleKey(RuleKey.of("foo", "bar")).build())
      .addRule(new NewActiveRule.Builder().setRuleKey(RuleKey.of(CSharpLanguage.REPOSITORY_KEY, "S123")).build())
      .build());
    mockSettings(expectedShouldUseCsharpEnterprise, false);

    underTest.execute(sensorContext);

    verify(analysisRequestHandler).analyze(argThat(fileNames -> fileNames.stream().anyMatch(file -> file.contains(fileName))),
      argThat(activeRules -> activeRules.size() == 1 && activeRules.stream().findFirst().get().ruleKey().rule().equals("S123")),
      argThat(x -> x.isShouldUseCsharpEnterprise() == expectedShouldUseCsharpEnterprise && !x.isShouldUseVbEnterprise()));
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void analyzeVbNetFile_callsHttpRequestWithCorrectParameters(boolean expectedShouldUseVbEnterprise) throws Exception {
    var fileName = "Foo.vb";
    mockInputFile(sensorContext, fileName, "Console.WriteLine(\"Hello World!\");");
    sensorContext.setActiveRules(new ActiveRulesBuilder()
      .addRule(new NewActiveRule.Builder().setRuleKey(RuleKey.of("foo", "bar")).build())
      .addRule(new NewActiveRule.Builder().setRuleKey(RuleKey.of(VbNetLanguage.REPOSITORY_KEY, "S456")).build())
      .build());
    mockSettings(false, expectedShouldUseVbEnterprise);

    underTest.execute(sensorContext);

    verify(analysisRequestHandler).analyze(argThat(fileNames -> fileNames.stream().anyMatch(file -> file.contains(fileName))),
      argThat(activeRules -> activeRules.size() == 1 && activeRules.stream().findFirst().get().ruleKey().rule().equals("S456")),
      argThat(x -> x.isShouldUseVbEnterprise() == expectedShouldUseVbEnterprise && !x.isShouldUseCsharpEnterprise()));
  }

  @Test
  void analyzeCsAndVbNetFiles_callsHttpRequestWithCorrectParameters() throws Exception {
    mockInputFiles(sensorContext, "foo.cs", "boo.vb");
    sensorContext.setActiveRules(new ActiveRulesBuilder()
      .addRule(new NewActiveRule.Builder().setRuleKey(RuleKey.of("foo", "bar")).build())
      .addRule(new NewActiveRule.Builder().setRuleKey(RuleKey.of(CSharpLanguage.REPOSITORY_KEY, "S123")).build())
      .addRule(new NewActiveRule.Builder().setRuleKey(RuleKey.of(VbNetLanguage.REPOSITORY_KEY, "S456")).build())
      .build());
    mockSettings(true, true);

    underTest.execute(sensorContext);

    verify(analysisRequestHandler).analyze(argThat(fileNames -> fileNames.size() == 2 &&
      fileNames.stream().anyMatch(file -> file.contains("foo.cs") || file.contains("boo.vb"))),
      argThat(activeRules -> activeRules.size() == 2 &&
        activeRules.stream().anyMatch(rule -> rule.ruleKey().rule().equals("S123") || rule.ruleKey().rule().contains("S456"))),
      argThat(x -> x.isShouldUseCsharpEnterprise() && x.isShouldUseVbEnterprise()));
  }

  @Test
  void analyzeCs_reportIssueForActiveRules() {
    sensorContext.fileSystem().add(csFile);
    sensorContext.setActiveRules(new ActiveRulesBuilder().addRule(csActiveRule).build());
    when(analysisRequestHandler.analyze(
      argThat(x -> x.stream().anyMatch(file -> file.contains(csFile.filename()))),
      argThat(x -> x.stream().anyMatch(cs -> cs.ruleKey().rule().contains(csActiveRule.ruleKey().rule()))),
      argThat(x -> !x.isShouldUseCsharpEnterprise() && !x.isShouldUseVbEnterprise())))
        .thenReturn(List.of(csharpIssue));

    underTest.execute(sensorContext);

    verifyExpectedRoslynIssue(csharpIssue);
  }

  @Test
  void analyzeVb_reportIssueForActiveRules() {
    sensorContext.fileSystem().add(vbFile);
    sensorContext.setActiveRules(new ActiveRulesBuilder()
      .addRule(vbActiveRule)
      .build());
    when(analysisRequestHandler.analyze(
      argThat(x -> x.stream().anyMatch(file -> file.contains(vbFile.filename()))),
      argThat(x -> x.stream().anyMatch(cs -> cs.ruleKey().rule().contains(vbActiveRule.ruleKey().rule()))),
      argThat(x -> !x.isShouldUseCsharpEnterprise() && !x.isShouldUseVbEnterprise())))
        .thenReturn(List.of(vbIssue));

    underTest.execute(sensorContext);

    verifyExpectedRoslynIssue(vbIssue);
  }

  @Test
  void analyzeCs_handleSecondaryLocations() {
    sensorContext.fileSystem().add(csFile);
    sensorContext.fileSystem().add(csFile2);
    sensorContext.setActiveRules(new ActiveRulesBuilder().addRule(csActiveRule).build());
    var csIssueWithSecondaryLocations = mockRoslynIssueWithSecondaryLocations(csActiveRule.ruleKey().rule(), CSharpLanguage.REPOSITORY_KEY, csFile.filename(), csFile2.filename());
    when(analysisRequestHandler.analyze(
      argThat(x -> x.stream().anyMatch(file -> file.contains(csFile.filename()))),
      argThat(x -> x.stream().anyMatch(cs -> cs.ruleKey().rule().contains(csActiveRule.ruleKey().rule()))),
      argThat(x -> !x.isShouldUseCsharpEnterprise() && !x.isShouldUseVbEnterprise())))
        .thenReturn(List.of(csIssueWithSecondaryLocations));

    underTest.execute(sensorContext);

    verifyExpectedRoslynIssue(csIssueWithSecondaryLocations);
  }

  @Test
  void analyzeVb_handleSecondaryLocations() {
    sensorContext.fileSystem().add(vbFile);
    sensorContext.fileSystem().add(vbFile2);
    sensorContext.setActiveRules(new ActiveRulesBuilder().addRule(vbActiveRule).build());
    var vbIssueWithSecondaryLocations = mockRoslynIssueWithSecondaryLocations(vbActiveRule.ruleKey().rule(), VbNetLanguage.REPOSITORY_KEY, vbFile.filename(), vbFile2.filename());
    when(analysisRequestHandler.analyze(
      argThat(x -> x.stream().anyMatch(file -> file.contains(vbFile.filename()))),
      argThat(x -> x.stream().anyMatch(cs -> cs.ruleKey().rule().contains(vbActiveRule.ruleKey().rule()))),
      argThat(x -> !x.isShouldUseCsharpEnterprise() && !x.isShouldUseVbEnterprise())))
        .thenReturn(List.of(vbIssueWithSecondaryLocations));

    underTest.execute(sensorContext);

    verifyExpectedRoslynIssue(vbIssueWithSecondaryLocations);
  }

  private void mockInputFiles(SensorContextTester sensorContextTester, String... fileNames) throws IOException {
    for (var fileName : fileNames) {
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
      .initMetadata(content)
      .setCharset(StandardCharsets.UTF_8)
      .build();
  }

  private RoslynIssue mockRoslynIssue(String ruleKey, String repository, String filePath) {
    var roslynIssue = mock(RoslynIssue.class);
    when(roslynIssue.getRuleId()).thenReturn(repository + ":" + ruleKey);
    var primaryLocation = mockIssueLocation(filePath);
    when(roslynIssue.getPrimaryLocation()).thenReturn(primaryLocation);
    mockTextRange(primaryLocation, 1, 1, 1, 5);

    return roslynIssue;
  }

  private void mockTextRange(RoslynIssueLocation issueLocation, int startLine, int endLine, int starLineOffset, int endLineOffset) {
    var textRange = mock(RoslynIssueTextRange.class);
    when(textRange.getStartLine()).thenReturn(startLine);
    when(textRange.getStartLineOffset()).thenReturn(starLineOffset);
    when(textRange.getEndLine()).thenReturn(endLine);
    when(textRange.getEndLineOffset()).thenReturn(endLineOffset);
    when(issueLocation.getTextRange()).thenReturn(textRange);
  }

  private RoslynIssueLocation mockIssueLocation(String filePath) {
    var location = mock(RoslynIssueLocation.class);
    when(location.getFilePath()).thenReturn(filePath);
    when(location.getMessage()).thenReturn("Don't do this");
    return location;
  }

  private RoslynIssueFlow mockIssueFlow(RoslynIssueLocation location) {
    var flow = mock(RoslynIssueFlow.class);
    when(flow.getLocations()).thenReturn(List.of(location));

    return flow;
  }

  private RoslynIssue mockRoslynIssueWithSecondaryLocations(String ruleKey, String repository, String filePath, String filePath2) {
    var roslynIssue = mockRoslynIssue(ruleKey, repository, filePath);

    var secondaryLocation = mockIssueLocation(filePath);
    mockTextRange(secondaryLocation, 1, 1, 2, 4);

    var secondaryLocationOnAnotherFile = mockIssueLocation(filePath2);
    mockTextRange(secondaryLocationOnAnotherFile, 1, 1, 3, 6);

    var flows = List.of(mockIssueFlow(secondaryLocation), mockIssueFlow(secondaryLocationOnAnotherFile));
    when(roslynIssue.getFlows()).thenReturn(flows);

    return roslynIssue;
  }

  private void verifyExpectedRoslynIssue(RoslynIssue... roslynIssues) {
    var actualIssues = sensorContext.allIssues();
    assertThat(roslynIssues).hasSize(actualIssues.size());
    for (var actualIssue : actualIssues) {
      var expectedIssue = Arrays.stream(roslynIssues).filter(x -> x.getRuleId().equals(actualIssue.ruleKey().toString())).findFirst().orElse(null);
      assertThat(expectedIssue).isNotNull();
      verifyExpectedLocation(expectedIssue.getPrimaryLocation(), actualIssue.primaryLocation());
      verifyExpectedSecondaryLocations(expectedIssue, actualIssue);
    }
  }

  private void verifyExpectedSecondaryLocations(RoslynIssue expectedIssue, Issue actualIssue) {
    assertThat(expectedIssue.getFlows()).hasSize(actualIssue.flows().size());
    var actualLocations = actualIssue.flows().stream().flatMap(x -> x.locations().stream()).toList();
    var expectedLocations = expectedIssue.getFlows().stream().flatMap(x -> x.getLocations().stream()).toList();
    for (var location : actualLocations) {
      var expectedLocation = expectedLocations.stream().filter(x -> x.getFilePath().equals(location.inputComponent().toString())).findFirst().orElse(null);
      verifyExpectedLocation(expectedLocation, location);
    }
  }

  private void verifyExpectedLocation(@Nullable RoslynIssueLocation expectedIssueLocation, IssueLocation actualIssueLocation) {
    assertThat(expectedIssueLocation).isNotNull();
    assertThat(actualIssueLocation.inputComponent().toString()).hasToString(expectedIssueLocation.getFilePath());
    assertThat(actualIssueLocation.message()).isEqualTo(expectedIssueLocation.getMessage());
    verifyExpectedTextRange(actualIssueLocation.textRange(), expectedIssueLocation.getTextRange());
  }

  private void verifyExpectedTextRange(TextRange actualTextRange, RoslynIssueTextRange expectedTextRange) {
    assertThat(actualTextRange.start().line()).isEqualTo(expectedTextRange.getStartLine());
    assertThat(actualTextRange.start().lineOffset()).isEqualTo(expectedTextRange.getStartLineOffset());
    assertThat(actualTextRange.end().line()).isEqualTo(expectedTextRange.getEndLine());
    assertThat(actualTextRange.end().lineOffset()).isEqualTo(expectedTextRange.getEndLineOffset());
  }

  private void mockSettings(boolean shouldUseCsharpEnterprise, boolean shouldUseVbnetEnterprise) {
    when(instanceConfigurationProvider.getShouldUseCsharpEnterprise()).thenReturn(shouldUseCsharpEnterprise);
    when(instanceConfigurationProvider.getShouldUseVbEnterprise()).thenReturn(shouldUseVbnetEnterprise);
  }
}
