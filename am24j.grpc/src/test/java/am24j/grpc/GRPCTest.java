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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.Flow.Subscription;

import org.apache.logging.log4j.Level;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import am24j.commons.Log4j2Config;
import am24j.rpc.AuthVerfier;
import am24j.rpc.Ctx;
import io.grpc.Metadata;
import io.grpc.StatusException;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

/**
 * @author avgustinmm
 */
public class GRPCTest {

  static {
    Log4j2Config.setUp(Level.INFO, Level.TRACE, "io.grpc");
  }
  
  private static Vertx sVertx;
  private static Vertx cVertx;
  
  private static Server server;
  private static Client client;
  
  private static IService service;
  
  @BeforeClass
  public static void before() {
    sVertx = Vertx.vertx();
    cVertx = Vertx.vertx();
    
    server = new Server(
      Collections.singletonList(new ServiceImpl()),
      Collections.singletonList(new AuthVerfier<Metadata>() {

        @Override
        public CompletionStage<Ctx> ctx(final Metadata auth) { // add real check
          return CompletableFuture.completedStage(Ctx.NULL);
        }
      }), 
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
    try {
      Thread.sleep(2_000);
    } catch (final InterruptedException e) {
      e.printStackTrace();
    }
  }
  
  @AfterClass
  public static void after() {
    client.close();
    cVertx.close();
    server.close();
    sVertx.close();
  }
  
  @Test
  public void testVoidCall() {
    Assert.assertEquals(service.voidCall().toCompletableFuture().join(), null);
  }
  
  @Test
  public void testGetCall() {
    Assert.assertEquals(service.getCall(3, "test").toCompletableFuture().join(), "testtesttest");
  }
  
  @Test(expected = StatusException.class)
  public void testThrowComplete() throws Throwable {
    try {
      service.throwExc(true).toCompletableFuture().join();
    } catch (final CompletionException e) {
      throw e.getCause();
    }
  }

  @Test(expected = StatusException.class)
  public void testThrowSync() throws Throwable {
    try {
      service.throwExc(true).toCompletableFuture().join();
    } catch (final CompletionException e) {
      throw e.getCause();
    }
  }
  
  @Test
  public void testStream() {
    final List<Integer> expected = Arrays.asList(new Integer[] {-1, 0, 1, 2, 3, 4, 5, -2});
    final List<Integer> received = Collections.synchronizedList(new ArrayList<>());
    final CompletableFuture<Void> finished = new CompletableFuture<>();
    
    service.stream(6, new Subscriber<String>() {
      
      private int round = 0;
      private Subscription subscription;
      
      @Override
      public void onSubscribe(final Subscription subscription) {
        this.subscription = subscription;
        received.add(-1);
        round++;
        subscription.request(3);
      }
      
      @Override
      public void onNext(final String item) {
        received.add(Integer.parseInt(item));
        if (round == 1) {
          round++;
          subscription.request(2);
        } else if (round == 2) {
          round++;
          subscription.request(1);
        } else {
          subscription.request(101); // unexoected!!!
        }
      }
      
      @Override
      public void onError(final Throwable throwable) {
        received.add(-2);
        finished.completeExceptionally(throwable);
      }
      
      @Override
      public void onComplete() {
        received.add(-2);
        finished.complete(null);
      }
    });
    
    finished.join();
    
    Assert.assertEquals(expected, received);
  }
}
