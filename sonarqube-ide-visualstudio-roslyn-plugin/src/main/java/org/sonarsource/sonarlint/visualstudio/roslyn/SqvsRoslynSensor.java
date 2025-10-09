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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
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
import org.sonarsource.sonarlint.visualstudio.roslyn.protocol.RoslynIssue;
import org.sonarsource.sonarlint.visualstudio.roslyn.protocol.RoslynIssueLocation;
import org.sonarsource.sonarlint.visualstudio.roslyn.protocol.RoslynIssueQuickFix;

public class SqvsRoslynSensor implements Sensor {

  private static final Logger LOG = Loggers.get(SqvsRoslynSensor.class);
  private final InstanceConfigurationProvider instanceConfigurationProvider;
  private final AnalysisPropertiesProvider analysisPropertiesProvider;
  private final RemoteAnalysisService remoteAnalysisService;

  public SqvsRoslynSensor(
    InstanceConfigurationProvider instanceConfigurationProvider,
    AnalysisPropertiesProvider analysisPropertiesProvider,
    RemoteAnalysisService remoteAnalysisService) {
    this.instanceConfigurationProvider = instanceConfigurationProvider;
    this.analysisPropertiesProvider = analysisPropertiesProvider;
    this.remoteAnalysisService = remoteAnalysisService;
  }

  private static void handle(SensorContext context, RoslynIssue roslynIssue) throws URISyntaxException {
    var parts = roslynIssue.getRuleId().split(":");
    var ruleKey = RuleKey.of(parts[0], parts[1]);
    if (context.activeRules().find(ruleKey) != null) {
      var diagFilePath = roslynIssue.getPrimaryLocation().getFileUri();
      var diagInputFile = findInputFile(context, diagFilePath);
      if (diagInputFile != null) {
        var newIssue = context.newIssue();
        newIssue
          .forRule(ruleKey)
          .at(createLocation(newIssue, roslynIssue.getPrimaryLocation(), diagInputFile));
        handleSecondaryLocations(context, roslynIssue, newIssue);
        handleQuickFixes(roslynIssue, newIssue);
        newIssue.save();
      }
    }
  }

  private static void handleQuickFixes(RoslynIssue roslynIssue, NewIssue newIssue) {
    for (RoslynIssueQuickFix quickFix : roslynIssue.getQuickFixes()) {
      var newQuickFix = newIssue.newQuickFix();
      // quickfixes are lazily evaluated on the client (VS) side,
      // here we only pass the value that is then mapped back to the quickfix object by the client
      newQuickFix.message(quickFix.getValue());
      newIssue.addQuickFix(newQuickFix);
    }
  }

  private static void handleSecondaryLocations(SensorContext context, RoslynIssue diag, NewIssue newIssue) throws URISyntaxException {
    var flows = diag.getFlows();
    for (var flow : flows) {
      for (var flowLocation : flow.getLocations()) {
        var filePath = flowLocation.getFileUri();
        var inputFile = findInputFile(context, filePath);
        if (inputFile != null) {
          newIssue.addLocation(createLocation(newIssue, flowLocation, inputFile));
        }
      }
    }
  }

  private static InputFile findInputFile(SensorContext context, URI filePath) {
    return context.fileSystem().inputFile(context.fileSystem().predicates().hasURI(filePath));
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

  private static FilePredicate getIsRoslynLanguagesOrRazorFilesPredicate(SensorContext context) {
    return context.fileSystem().predicates().or(
      context.fileSystem().predicates().hasLanguages(CSharpLanguage.LANGUAGE_KEY, VbNetLanguage.LANGUAGE_KEY),
      // if it's HTML language, make sure to only analyze the razor files
      context.fileSystem().predicates().hasExtension(CSharpLanguage.RAZOR_EXTENSION),
      context.fileSystem().predicates().hasExtension(VbNetLanguage.RAZOR_EXTENSION));
  }

  private void analyze(SensorContext context, FilePredicate predicate) {
    var inputFiles = getFileUris(context, predicate);
    var activeRules = getActiveRules(context);
    var analysisProperties = analysisPropertiesProvider.getAnalysisProperties();
    var analyzerInfo = getAnalyzerInfo();
    var roslynIssues = remoteAnalysisService.analyze(inputFiles, activeRules, analysisProperties, analyzerInfo);
    for (var roslynIssue : roslynIssues) {
      try {
        handle(context, roslynIssue);
      } catch (Exception exception) {
        LOG.error(String.format("Issue %s can not be saved due to ", roslynIssue.getRuleId()), exception.fillInStackTrace());
      }
    }

  }

  private List<URI> getFileUris(SensorContext context, FilePredicate predicate) {
    return StreamSupport.stream(
      context.fileSystem().inputFiles(predicate).spliterator(), false)
      .map(InputFile::uri)
      .toList();
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
