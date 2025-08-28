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

import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.sonar.api.config.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class InstanceConfigurationProviderTests {

  private Configuration configuration;

  @BeforeEach
  void prepare() {
    configuration = mock(Configuration.class);
  }

  @ParameterizedTest
  @MethodSource("expectedConfigurationProperties")
  void propertiesDefined_initializesPropertyAsExpected(boolean expectedShouldUseCsharpEnterprise, boolean expectedShouldUseVbEnterprise) {
    mockConfigurationProperty(SqvsRoslynPluginPropertyDefinitions.getShouldUseCsharpEnterprise(), expectedShouldUseCsharpEnterprise);
    mockConfigurationProperty(SqvsRoslynPluginPropertyDefinitions.getShouldUseVbEnterprise(), expectedShouldUseVbEnterprise);

    var underTest = new InstanceConfigurationProvider(configuration);

    assertThat(underTest.getShouldUseCsharpEnterprise()).isEqualTo(expectedShouldUseCsharpEnterprise);
    assertThat(underTest.getShouldUseVbEnterprise()).isEqualTo(expectedShouldUseVbEnterprise);
  }

  @Test
  void propertiesNotDefined_initializesFalse() {
    var underTest = new InstanceConfigurationProvider(configuration);

    assertThat(underTest.getShouldUseCsharpEnterprise()).isFalse();
    assertThat(underTest.getShouldUseVbEnterprise()).isFalse();
  }

  private void mockConfigurationProperty(String name, boolean value) {
    var optionalValue = Optional.of(String.valueOf(value));
    when(configuration.get(name)).thenReturn(optionalValue);
  }

  static Stream<Arguments> expectedConfigurationProperties() {
    return Stream.of(
      Arguments.of(true, true),
      Arguments.of(false, false),
      Arguments.of(false, true),
      Arguments.of(true, false));
  }

}
