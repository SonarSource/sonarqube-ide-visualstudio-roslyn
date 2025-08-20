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
import java.util.stream.StreamSupport;
import org.sonar.api.batch.fs.FilePredicate;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.batch.sensor.issue.NewIssue;
import org.sonar.api.batch.sensor.issue.NewIssueLocation;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonarsource.sonarlint.visualstudio.roslyn.http.HttpAnalysisRequestHandler;
import org.sonarsource.sonarlint.visualstudio.roslyn.protocol.Diagnostic;
import org.sonarsource.sonarlint.visualstudio.roslyn.protocol.DiagnosticLocation;
import org.sonarsource.sonarlint.visualstudio.roslyn.protocol.Fix;
import org.sonarsource.sonarlint.visualstudio.roslyn.protocol.QuickFix;
import org.sonarsource.sonarlint.visualstudio.roslyn.protocol.QuickFixEdit;

public class SqvsRoslynSensor implements Sensor {

  private static final Logger LOG = Loggers.get(SqvsRoslynSensor.class);
  private final HttpAnalysisRequestHandler httpRequestHandler;

  public SqvsRoslynSensor(HttpAnalysisRequestHandler httpRequestHandler) {
    this.httpRequestHandler = httpRequestHandler;
  }

  private static void handle(SensorContext context, Diagnostic diag) {
    var ruleKey = RuleKey.of(CSharpLanguage.REPOSITORY_KEY, diag.getId());
    if (context.activeRules().find(ruleKey) != null) {
      var diagFilePath = Paths.get(diag.getFilename());
      var diagInputFile = findInputFile(context, diagFilePath);
      if (diagInputFile != null) {
        var newIssue = context.newIssue();
        newIssue
          .forRule(ruleKey)
          .at(createLocation(newIssue, diag, diagInputFile));
        handleSecondaryLocations(context, diag, newIssue);
        handleQuickFixes(context, diag, newIssue);
        newIssue.save();
      }
    }
  }

  private static void handleQuickFixes(SensorContext context, Diagnostic diag, NewIssue newIssue) {
    var quickFixes = diag.getQuickFixes();
    if (quickFixes != null && quickFixes.length > 0) {
      newIssue.setQuickFixAvailable(true);
      for (var quickFix : quickFixes) {
        handleQuickFix(context, quickFix, newIssue);
      }
    }
  }

  static void handleQuickFix(SensorContext context, QuickFix quickFix, NewIssue newIssue) {
    var newQuickFix = newIssue.newQuickFix();
    newQuickFix.message(quickFix.getMessage());
    for (Fix fix : quickFix.getFixes()) {
      var fixInputFile = findInputFile(context, Paths.get(fix.getFilename()));
      if (fixInputFile != null) {
        var newInputFileEdit = newQuickFix.newInputFileEdit()
          .on(fixInputFile);
        for (QuickFixEdit edit : fix.getEdits()) {
          var newTextEdit = newInputFileEdit.newTextEdit()
            .at(fixInputFile.newRange(edit.getStartLine(), edit.getStartColumn() - 1, edit.getEndLine(), edit.getEndColumn() - 1))
            .withNewText(edit.getNewText());
          newInputFileEdit.addTextEdit(newTextEdit);
        }
        newQuickFix.addInputFileEdit(newInputFileEdit);
      }
    }
    newIssue.addQuickFix(newQuickFix);
  }

  private static void handleSecondaryLocations(SensorContext context, Diagnostic diag, NewIssue newIssue) {
    var additionalLocations = diag.getAdditionalLocations();
    if (additionalLocations != null) {
      for (var additionalLocation : additionalLocations) {
        var additionalFilePath = Paths.get(additionalLocation.getFilename());
        var additionalFilePathInputFile = findInputFile(context, additionalFilePath);
        if (additionalFilePathInputFile != null) {
          newIssue.addLocation(createLocation(newIssue, additionalLocation, additionalFilePathInputFile));
        }
      }
    }
  }

  private static InputFile findInputFile(SensorContext context, Path filePath) {
    return context.fileSystem().inputFile(context.fileSystem().predicates().is(filePath.toFile()));
  }

  private static NewIssueLocation createLocation(NewIssue newIssue, DiagnosticLocation location, InputFile inputFile) {
    return newIssue.newLocation()
      .on(inputFile)
      .at(inputFile.newRange(location.getLine(), location.getColumn() - 1, location.getEndLine(), location.getEndColumn() - 1))
      .message(location.getText());
  }

  @Override
  public void describe(SensorDescriptor descriptor) {
    descriptor
      .name("SQVS-Roslyn")
      .onlyOnLanguages(CSharpLanguage.LANGUAGE_KEY, VbNetLanguage.LANGUAGE_KEY)
      .createIssuesForRuleRepositories(CSharpLanguage.REPOSITORY_KEY, VbNetLanguage.REPOSITORY_KEY);
  }

  @Override
  public void execute(SensorContext context) {
    FilePredicate predicate = context.fileSystem().predicates().hasLanguage(CSharpLanguage.LANGUAGE_KEY);
    if (!context.fileSystem().hasFiles(predicate)) {
      return;
    }
    analyze(context, predicate);
  }

  private void analyze(SensorContext context, FilePredicate predicate) {
    var inputFiles = StreamSupport.stream(
      context.fileSystem().inputFiles(predicate).spliterator(), false)
      .map(InputFile::absolutePath).toList();
    var activeRules = context.activeRules().findByRepository(CSharpLanguage.REPOSITORY_KEY);
    httpRequestHandler.analyze(inputFiles, activeRules);
    // TODO by https://sonarsource.atlassian.net/browse/SLVS-2426 send analysis results to SlCore
    // handle(context, diagnostic);
  }

}
