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

import io.grpc.ManagedChannel;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.grpc.VertxChannelBuilder;

/**
 * GRPC Client veritcl
 *
 * @author avgustinmm
 */
public class ClientVerticle extends AbstractVerticle {

  public static final String HOST = "host";
  public static final String PORT = "port";

  private ManagedChannel channel;

  @Override
  public void start(final Promise<Void> startPromise) throws Exception {
    final JsonObject config = context.config();
    channel = VertxChannelBuilder
      .forAddress(vertx, config.getString(HOST), config.getInteger(PORT))
      .usePlaintext()
      .build();
    startPromise.complete();
  }

  @Override
  public void stop(final Promise<Void> stopPromise) throws Exception {
    channel.shutdown();
    stopPromise.complete();
  }

  ManagedChannel channel() {
    return channel;
  }
}
