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

import am24j.commons.Ctx;
import am24j.example.hellowold.DirectHttp;
import am24j.example.hellowold.JaxRS;
import am24j.example.hellowold.RPCImpl;
import am24j.hz.HZInstance;
import am24j.inject.Starter;
import am24j.rpc.Auth;
import am24j.rpc.AuthVerfier;
import am24j.rpc.grpc.Cmd;
import am24j.rpc.grpc.Common;
import am24j.vertx.HZCluster;
import am24j.vertx.Shell;
import am24j.vertx.VertxInstance;
import am24j.vertx.cmd.LauncherCmd;
import am24j.vertx.http.Http;
import am24j.vertx.http.RestEasy;
import am24j.vrt.inject.VRTConfig;
import io.grpc.Metadata;
import io.vertx.core.http.HttpServerRequest;

/**
 * Bootstrap a server with Vertx base runtime, http &amp; gRPC server with Hello World direct / JaxRS and RPC services
 *
 * @author avgustinmm
 */
public class Server {

  public static void main(final String[] args) throws Exception {
    Starter.start(
      VRTConfig.class,

      HZInstance.class,
      HZCluster.class,

      VertxInstance.class,

      Shell.class,
      LauncherCmd.class,

      JaxRS.class,
      DirectHttp.class,
      RPCImpl.class,

      RestEasy.class,
      Http.class,

      GRPCAuth.class,

      BasicAuth.class,

      am24j.rpc.grpc.Server.class,
      am24j.rpc.http.Server.class,

      Cmd.class,
      am24j.rpc.http.Cmd.class);

    final String httpPort = Ctx.intProp("node.id", 0) == 0 ? "" : ":8" + Ctx.intProp("node.id", 0);
    final String rpcClientConf = Ctx.intProp("node.id", 0) == 0 ? "" :  " -Dnode.id=" + Ctx.intProp("node.id", 0);
    System.out.println("Could get Hello World HTTP service via browser:");
    System.out.println("  http://localhost" + httpPort + "/direct/hello[?name=<name>]");
    System.out.println("  http://localhost" + httpPort + "/jaxrs/hello[?name=<name>]");
    System.out.println("Or, start gRPC call to Hello World gRPC RPC call with starting in cmd:");
    System.out.println("  java" + rpcClientConf + " -jar example-app.jar am24j.example.Client");
    System.out.println("Or, start gRPC call to Hello World HTTP RPC GET call via browser!");
    System.out.println("  http://localhost" + httpPort + "/rpc/hellowprld_1.0.0/hello[?arg_0=<name>]");

    System.out.println("\nShhell is available via telnet:");
    System.out.println("  telnet localhost 230" + Ctx.intProp("node.id", 0));
    System.out.println("Or, via http:");
    System.out.println("  http://localhost:800" + Ctx.intProp("node.id", 0) + "/shell.html");
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

  public static class BasicAuth implements AuthVerfier<HttpServerRequest> {

    @Override
    public CompletionStage<Auth> verify(final HttpServerRequest request) {
      final String authorization = request.getHeader("Authorization");
      if (authorization == null) {
        return CompletableFuture.completedStage(null);
      } else {
        return CompletableFuture.failedStage(new IllegalAccessException("Error"));
      }
    }

    @Override
    public String toString() {
      return "Test Auth";
    }
  }
}
