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

import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import org.apache.logging.log4j.Level;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import am24j.commons.Log4j2Config;
import am24j.rpc.Auth;
import am24j.rpc.AuthVerfier;
import am24j.rpc.BaseTest;
import am24j.rpc.IService;
import am24j.rpc.ServiceImpl;
import io.grpc.Metadata;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

/**
 * @author avgustinmm
 */
public class GRPCTest extends BaseTest {

  static {
    Log4j2Config.setUp(Level.INFO, Level.TRACE, "am24j.rcp.grpc");
  }

  private static Vertx sVertx;
  private static Vertx cVertx;

  private static Server server;
  private static Client client;

  @BeforeClass
  public static void before() {
    sVertx = Vertx.vertx();
    cVertx = Vertx.vertx();

    server = new Server(
      Collections.singletonList(new ServiceImpl()),
      Collections.singletonList(new TestAuthVerfier()),
      new DeploymentOptions()
        .setConfig(
          new JsonObject()
            .put(ServerVerticle.HOST, "localhost")
            .put(ServerVerticle.PORT,  1000)),
      sVertx);
    client = new Client(
      new DeploymentOptions()
        .setConfig(
          new JsonObject()
            .put(ClientVerticle.HOST, "localhost")
            .put(ClientVerticle.PORT,  1000)),
        cVertx);
    service = client.service(() -> "user:pass", IService.class);
  }

  @AfterClass
  public static void after() {
    client.close();
    cVertx.close();
    server.close();
    sVertx.close();
  }

  public static class TestAuthVerfier implements AuthVerfier<Metadata> {

    @Override
    public CompletionStage<Auth> verify(final Metadata auth) { // add real check
      return CompletableFuture.completedStage(Auth.ANONYMOUS);
    }
  }
}
