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

import org.sonar.api.config.Configuration;
import org.sonarsource.api.sonarlint.SonarLintSide;

@SonarLintSide(lifespan = SonarLintSide.INSTANCE)
public class InstanceConfigurationProvider {
  private final boolean shouldUseCsharpEnterprise;
  private final boolean shouldUseVbEnterprise;

  public InstanceConfigurationProvider(Configuration configuration) {
    shouldUseCsharpEnterprise = configuration.get(SqvsRoslynPluginPropertyDefinitions.getShouldUseCsharpEnterprise()).map(Boolean::parseBoolean).orElse(false);
    shouldUseVbEnterprise = configuration.get(SqvsRoslynPluginPropertyDefinitions.getShouldUseVbEnterprise()).map(Boolean::parseBoolean).orElse(false);
  }

  public Boolean getShouldUseCsharpEnterprise() {
    return shouldUseCsharpEnterprise;
  }

  public Boolean getShouldUseVbEnterprise() {
    return shouldUseVbEnterprise;
  }
}
