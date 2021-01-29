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
package am24j.vertx;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;

import am24j.rt.Ctx;
import io.vertx.core.Context;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Verticle;
import io.vertx.core.Vertx;

/**
 * @author avgustinmm
 */
public abstract class Component<T extends Verticle> implements AutoCloseable {

  private static final Logger LOG = Ctx.logger("Component");
  
  private final Vertx vertx;
  private final String deploymentID;
  
  protected Component(final T verticle, final DeploymentOptions options, final Vertx vertx) {
    LOG.info("Deploying {} (options: {}, vertx: {})", verticle, options.toJson().encodePrettily(), vertx);
    this.vertx = vertx;
    final CompletableFuture<String> future = new CompletableFuture<>();
    vertx.deployVerticle(verticle, options).onComplete(ar -> {
      if (ar.succeeded()) {
        future.complete(ar.result());
      } else {
        future.completeExceptionally(ar.cause());
      }
    });
    deploymentID = future.join();
  }
  
  @Override
  public void close() {
    // expected in Shutdown Hook
    final CompletableFuture<Void> future = new CompletableFuture<>();
    vertx.undeploy(deploymentID).onComplete(ar -> {
      if (ar.succeeded()) {
        future.complete(ar.result());
      } else {
        future.completeExceptionally(ar.cause());
      }
    });
    if (Context.isOnVertxThread()) {
      try {
        future.get(1, TimeUnit.SECONDS);
      } catch (final InterruptedException e) {
        LOG.error("Failed to close!", e);
        Thread.interrupted();
      } catch (final ExecutionException | TimeoutException e) {
        LOG.error("Failed to close!", e);
      }
    } else {
      future.join();
    }
  }
}
