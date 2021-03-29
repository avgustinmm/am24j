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
package am24j.rpc.spring;

import java.io.IOException;
import java.util.Optional;

import javax.inject.Named;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import am24j.commons.Ctx;
import am24j.commons.RExc;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.json.JsonObject;

@Configuration
public class GRPCConfig {

  @Named("grpc_server.json")
  @Bean
  public DeploymentOptions grpcServerOptions() {
    return deploymenOptions("grpc_server.json");
  }

  @Named("grpc_client.json")
  @Bean
  public DeploymentOptions grpClientOptions() {
    return deploymenOptions("grpc_client.json");
  }

  private Optional<String> resource(final String name) {
    try {
      return Optional.ofNullable(Ctx.substitutedResource(name));
    } catch (final IOException e) {
      throw RExc.toRuntime(e);
    }
  }

  private DeploymentOptions deploymenOptions(final String name) {
    return resource(name).map(JsonObject::new).map(DeploymentOptions::new).orElse(new DeploymentOptions());
  }
}
