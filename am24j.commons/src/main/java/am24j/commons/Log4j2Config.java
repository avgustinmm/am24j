/**
 * Copyright 2021 Avgustin Marinov
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package am24j.commons;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilderFactory;
import org.apache.logging.log4j.core.config.builder.impl.BuiltConfiguration;

public class Log4j2Config {

  private Log4j2Config() {}
  
  public static void setUp(String... extLogers) {
    setUp(Level.INFO, Level.DEBUG, extLogers);
  }
  
  public static void setUp(final Level rootLevel, final Level extLevel, final String... extLogers) {
    final ConfigurationBuilder<BuiltConfiguration> builder = ConfigurationBuilderFactory.newConfigurationBuilder();
    builder
      .add(
        builder
          .newAppender("stdout", "Console")
          .add(
            builder
              .newLayout("PatternLayout")
              .addAttribute("pattern", "[%d{yyyy-MMM-dd HH:mm:ss.SSS z}{UTC}][%logger{36}] %-5level %msg%n")))
      .add(
        builder
          .newRootLogger(rootLevel)
          .add(builder.newAppenderRef("stdout")));
    for (final String extLoger : extLogers) {
      builder
        .add(
          builder
            .newLogger(extLoger, extLevel)
            .add(builder.newAppenderRef("stdout"))
            .addAttribute("additivity", false));
    }
    Configurator.initialize(builder.build());
  }
}
