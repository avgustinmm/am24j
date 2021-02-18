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
package am24j.grpc;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import org.apache.logging.log4j.Level;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import am24j.commons.Log4j2Config;
import am24j.inject.Injector.Key;
import am24j.rpc.AuthVerfier;
import am24j.rpc.Ctx;
import am24j.rt.Starter;
import am24j.rt.config.Config;
import am24j.vertx.Instance;
import am24j.vertx.NoCluster;
import io.grpc.Metadata;

public class GRPCIoCTest {

  static {
    Log4j2Config.setUp(Level.INFO, Level.TRACE, "io.grpc");
  }

  private static Starter sStarter;
  private static Starter cStarter;
  
  private static IService service;
  
  @BeforeClass
  public static void before() {
    sStarter = Starter.start(
      Config.class,   
      // vertx
      NoCluster.class,      
      Instance.class,
      // wite verfiers
      TestAuthVerfier.class,
      // service
      ServiceImpl.class,
      // gRPC server - wheb services are registered
      Server.class);
    cStarter = Starter.start(
        Config.class,   
        // vertx
        NoCluster.class,      
        Instance.class);
    service = cStarter.injector().<Client>getInstance(Key.of(Client.class)).service(() -> "user:pass", IService.class);
    try {
      Thread.sleep(2_000);
    } catch (final InterruptedException e) {
      e.printStackTrace();
    }
  }
  
  @AfterClass
  public static void after() throws Exception {
    cStarter.close();
    sStarter.close();
  }
  
  @Test
  public void testVoidCall() {
    Assert.assertEquals(service.voidCall().toCompletableFuture().join(), null);
  }
  
  public static class TestAuthVerfier implements AuthVerfier<Metadata> {

    @Override
    public CompletionStage<Ctx> ctx(final Metadata auth) { // add real check
      return CompletableFuture.completedStage(Ctx.NULL);
    }
  }
}
