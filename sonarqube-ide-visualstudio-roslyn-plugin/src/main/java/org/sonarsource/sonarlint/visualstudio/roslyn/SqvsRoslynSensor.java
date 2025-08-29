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

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.StreamSupport;
import org.sonar.api.batch.fs.FilePredicate;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.rule.ActiveRule;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.batch.sensor.issue.NewIssue;
import org.sonar.api.batch.sensor.issue.NewIssueLocation;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonarsource.sonarlint.visualstudio.roslyn.http.AnalyzerInfoDto;
import org.sonarsource.sonarlint.visualstudio.roslyn.http.HttpAnalysisRequestHandler;
import org.sonarsource.sonarlint.visualstudio.roslyn.protocol.RoslynIssue;
import org.sonarsource.sonarlint.visualstudio.roslyn.protocol.RoslynIssueLocation;

public class SqvsRoslynSensor implements Sensor {

  private static final Logger LOG = Loggers.get(SqvsRoslynSensor.class);
  private final HttpAnalysisRequestHandler httpRequestHandler;
  private final InstanceConfigurationProvider instanceConfigurationProvider;

  public SqvsRoslynSensor(HttpAnalysisRequestHandler httpRequestHandler, InstanceConfigurationProvider instanceConfigurationProvider) {
    this.httpRequestHandler = httpRequestHandler;
    this.instanceConfigurationProvider = instanceConfigurationProvider;
  }

  private static void handle(SensorContext context, RoslynIssue roslynIssue) {
    var parts = roslynIssue.getRuleId().split(":");
    var ruleKey = RuleKey.of(parts[0], parts[1]);
    if (context.activeRules().find(ruleKey) != null) {
      var diagFilePath = Paths.get(roslynIssue.getPrimaryLocation().getFilePath());
      var diagInputFile = findInputFile(context, diagFilePath);
      if (diagInputFile != null) {
        var newIssue = context.newIssue();
        newIssue
          .forRule(ruleKey)
          .at(createLocation(newIssue, roslynIssue.getPrimaryLocation(), diagInputFile));
        handleSecondaryLocations(context, roslynIssue, newIssue);
        // TODO by https://sonarsource.atlassian.net/browse/SLVS-2492 handle quickfixes once they are returned from the server
        /* handleQuickFixes(context, roslynIssue, newIssue); */
        newIssue.save();
      }
    }
  }

  private static void handleSecondaryLocations(SensorContext context, RoslynIssue diag, NewIssue newIssue) {
    var flows = diag.getFlows();
    for (var flow : flows) {
      for (var flowLocation : flow.getLocations()) {
        var filePath = Paths.get(flowLocation.getFilePath());
        var inputFile = findInputFile(context, filePath);
        if (inputFile != null) {
          newIssue.addLocation(createLocation(newIssue, flowLocation, inputFile));
        }
      }
    }
  }

  private static InputFile findInputFile(SensorContext context, Path filePath) {
    return context.fileSystem().inputFile(context.fileSystem().predicates().is(filePath.toFile()));
  }

  private static NewIssueLocation createLocation(NewIssue newIssue, RoslynIssueLocation location, InputFile inputFile) {
    return newIssue.newLocation()
      .on(inputFile)
      .at(inputFile.newRange(location.getTextRange().getStartLine(), location.getTextRange().getStartLineOffset(), location.getTextRange().getEndLine(),
        location.getTextRange().getEndLineOffset()))
      .message(location.getMessage());
  }

  @Override
  public void describe(SensorDescriptor descriptor) {
    descriptor
      .name("SQVS-Roslyn")
      // the file extensions for razor files are already defined for HTML language, so they can't be associated with C#/VB.NET due to plugin limitations,
      // so we have to enable executing the sensor for HTML language
      .onlyOnLanguages(CSharpLanguage.LANGUAGE_KEY, VbNetLanguage.LANGUAGE_KEY, SqvsRoslynPluginPropertyDefinitions.HTML_LANGUAGE_KEY)
      .createIssuesForRuleRepositories(CSharpLanguage.REPOSITORY_KEY, VbNetLanguage.REPOSITORY_KEY);
  }

  @Override
  public void execute(SensorContext context) {
    var isRoslynLanguagesOrRazorFilesPredicate = getIsRoslynLanguagesOrRazorFilesPredicate(context);
    if (!context.fileSystem().hasFiles(isRoslynLanguagesOrRazorFilesPredicate)) {
      return;
    }
    analyze(context, isRoslynLanguagesOrRazorFilesPredicate);
  }

  private FilePredicate getIsRoslynLanguagesOrRazorFilesPredicate(SensorContext context) {
    return context.fileSystem().predicates().or(
      context.fileSystem().predicates().hasLanguages(CSharpLanguage.LANGUAGE_KEY, VbNetLanguage.LANGUAGE_KEY),
      // if it's HTML language, make sure to only analyze the razor files
      context.fileSystem().predicates().hasExtension(CSharpLanguage.RAZOR_EXTENSION),
      context.fileSystem().predicates().hasExtension(VbNetLanguage.RAZOR_EXTENSION));
  }

  // TODO by https://sonarsource.atlassian.net/browse/SLVS-2492 handle quickfixes once they are returned from the server
  /*
   * private static void handleQuickFixes(SensorContext context, RoslynIssue diag, NewIssue newIssue) {
   * var quickFixes = diag.getQuickFixes();
   * if (quickFixes != null && quickFixes.length > 0) {
   * newIssue.setQuickFixAvailable(true);
   * for (var quickFix : quickFixes) {
   * handleQuickFix(context, quickFix, newIssue);
   * }
   * }
   * }
   *
   * static void handleQuickFix(SensorContext context, QuickFix quickFix, NewIssue newIssue) {
   * var newQuickFix = newIssue.newQuickFix();
   * newQuickFix.message(quickFix.getMessage());
   * for (Fix fix : quickFix.getFixes()) {
   * var fixInputFile = findInputFile(context, Paths.get(fix.getFilename()));
   * if (fixInputFile != null) {
   * var newInputFileEdit = newQuickFix.newInputFileEdit()
   * .on(fixInputFile);
   * for (QuickFixEdit edit : fix.getEdits()) {
   * var newTextEdit = newInputFileEdit.newTextEdit()
   * .at(fixInputFile.newRange(edit.getStartLine(), edit.getStartColumn() - 1, edit.getEndLine(), edit.getEndColumn() - 1))
   * .withNewText(edit.getNewText());
   * newInputFileEdit.addTextEdit(newTextEdit);
   * }
   * newQuickFix.addInputFileEdit(newInputFileEdit);
   * }
   * }
   * newIssue.addQuickFix(newQuickFix);
   * }
   */

  private void analyze(SensorContext context, FilePredicate predicate) {
    var inputFiles = getFilePaths(context, predicate);
    var activeRules = getActiveRules(context);
    var analyzerInfo = getAnalyzerInfo();
    var roslynIssues = httpRequestHandler.analyze(inputFiles, activeRules, analyzerInfo);
    for (var roslynIssue : roslynIssues) {
      try {
        handle(context, roslynIssue);
      } catch (Exception exception) {
        LOG.error(String.format("Issue %s can not be saved due to ", roslynIssue.getRuleId()), exception.fillInStackTrace());
      }
    }
  }

  private Collection<String> getFilePaths(SensorContext context, FilePredicate predicate) {
    return StreamSupport.stream(
      context.fileSystem().inputFiles(predicate).spliterator(), false)
      .map(InputFile::absolutePath).toList();
  }

  private Collection<ActiveRule> getActiveRules(SensorContext context) {
    var activeRules = new ArrayList<ActiveRule>();
    activeRules.addAll(context.activeRules().findByRepository(CSharpLanguage.REPOSITORY_KEY));
    activeRules.addAll(context.activeRules().findByRepository(VbNetLanguage.REPOSITORY_KEY));
    return activeRules;
  }

  private AnalyzerInfoDto getAnalyzerInfo() {
    return new AnalyzerInfoDto(instanceConfigurationProvider.getShouldUseCsharpEnterprise(), instanceConfigurationProvider.getShouldUseVbEnterprise());
  }
}
