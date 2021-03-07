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

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import am24j.example.hellowold.DirectHttp;
import am24j.example.hellowold.JaxRS;
import am24j.example.hellowold.RPCImpl;
import am24j.inject.Starter;
import am24j.rpc.Auth;
import am24j.rpc.AuthVerfier;
import am24j.rpc.grpc.Common;
import am24j.rt.config.Config;
import am24j.vertx.HZCluster;
import am24j.vertx.Shell;
import am24j.vertx.VertxInstance;
import am24j.vertx.http.Http;
import am24j.vertx.http.RestEasy;
import io.grpc.Metadata;

/**
 * Bootrstrap a server with Vertx base runtime, http & gRPC server with Hello World direct / JaxRS and RPC services
 *
 * @author avgustinmm
 */
public class Server {

  public static void main(final String[] args) throws Exception {
    Starter.start(new Class<?>[] {
      Config.class,

      am24j.rt.hz.HZInstance.class,
      HZCluster.class,

      VertxInstance.class,

      Shell.class,

      JaxRS.class,
      DirectHttp.class,
      RPCImpl.class,

      RestEasy.class,
      Http.class,

      GRPCAuth.class,

      am24j.rpc.grpc.Server.class,
      am24j.rpc.http.Server.class
    });
    System.out.println("Could get Hello World via browser:");
    System.out.println("  http://localhost/direct/hello[?name=<name>]");
    System.out.println("  http://localhost/jaxrs/hello[?name=<name>]");
    System.out.println("Or, start gRPC call to Hello World RPC with starting in cmd:");
    System.out.println("  java -jar example-app.jar am24j.example.Client");
  }

  public static class GRPCAuth implements AuthVerfier<Metadata> {

    @Override
    public CompletionStage<Auth> verify(final Metadata auth) {
      if ("user:pass".equals(auth.get(Common.WWW_AUTHENTICATE))) {
        return CompletableFuture.completedStage(Auth.ANONYMOUS);
      } else {
        return CompletableFuture.failedFuture(new RuntimeException("Unauthorixed!"));
      }
    }

  }
}
