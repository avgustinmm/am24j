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
package am24j.vrt.spring;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.hazelcast.core.HazelcastInstance;

import am24j.commons.RExc;
import am24j.config.Config;
import am24j.hz.HZInstance;
import am24j.vertx.VertxInstance;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.json.JsonObject;

@Configuration
public class VRTConfig {

  private final Config config;

  @Inject
  public VRTConfig(final Config config) {
    this.config = config;
  }

  @Named("hazelcast.json")
  @Bean
  public JsonObject hzConfig() {
    return jsonObject("hazelcast.json");
  }

  @Named("shell.json")
  @Bean
  public DeploymentOptions shellOptions() {
    return deploymenOptions("hazelcast.json");
  }

  @Named("vertx.json")
  @Bean
  public VertxOptions options() {
    return new VertxOptions(jsonObject("vertx.json"));
  }

  @Named("http_server.json")
  @Bean
  public DeploymentOptions httpServer() {
    return deploymenOptions("http_server.json");
  }

  @Bean
  public HazelcastInstance hzInstance(final ApplicationContext ctx) {
    try {
      return ctx.getBean(HZInstance.class).get();
    } catch (final NoSuchBeanDefinitionException e) {
      return null;
    }
  }

  @Bean
  public Vertx vertx(final VertxInstance vInstance) {
    return vInstance.get();
  }

  private Optional<String> resource(final String name) {
    try {
      return Optional.ofNullable(config.resource(name)).map(resource -> new String(resource, StandardCharsets.UTF_8));
    } catch (final IOException e) {
      throw RExc.toRuntime(e);
    }
  }

  private JsonObject jsonObject(final String name) {
    return resource(name).map(JsonObject::new).orElse(new JsonObject());
  }

  private DeploymentOptions deploymenOptions(final String name) {
    return resource(name).map(JsonObject::new).map(DeploymentOptions::new).orElse(new DeploymentOptions());
  }
}
