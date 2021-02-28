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
package am24j.rpc.grpc;

import java.util.List;

import javax.inject.Inject;

import io.grpc.ServerServiceDefinition;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.grpc.VertxServer;
import io.vertx.grpc.VertxServerBuilder;

/**
 * GRPC Server Verticle
 *
 * @author avgustinmm
 */
public class ServerVerticle extends AbstractVerticle {

  public static final String HOST = "host";
  public static final String PORT = "port";

  private final List<ServerServiceDefinition> services;
  private VertxServer server;

  @Inject
  public ServerVerticle(final List<ServerServiceDefinition> services) {
    this.services = services;
  }

  @Override
  public void start(final Promise<Void> startPromise) throws Exception {
    final JsonObject config = context.config();
    VertxServerBuilder builder = VertxServerBuilder
        .forAddress(vertx, config.getString(HOST), config.getInteger(PORT));
    for (final ServerServiceDefinition service : services) {
      builder.addService(service);
    }
    server = builder.build();
    server.start(startPromise);
  }

  @Override
  public void stop(final Promise<Void> stopPromise) throws Exception {
    server.shutdown(stopPromise);
  }
}
