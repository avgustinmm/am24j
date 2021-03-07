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
package am24j.example;

import am24j.example.hellowold.RPCInterface;
import am24j.inject.Injector.Key;
import am24j.inject.Starter;
import am24j.rt.config.Config;
import am24j.vertx.VertxInstance;

public class Client {

  public static void main(final String[] args) throws Exception {
    try (final Starter starter = Starter.start(new Class<?>[] {
        Config.class,
        VertxInstance.class
      })) {
      final RPCInterface service =
        starter
          .injector()
          .<am24j.rpc.grpc.Client>getInstance(Key.of(am24j.rpc.grpc.Client.class))
          .service(() -> "user:pass", RPCInterface.class);
      System.out.println("RPC call returned:\n  " + service.hello("World (" + System.currentTimeMillis() + ")").toCompletableFuture().join());
    }
  }
}
