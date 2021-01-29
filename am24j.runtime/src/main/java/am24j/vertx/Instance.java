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

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import am24j.inject.annotation.Provides;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.spi.cluster.ClusterManager;

/**
 * @author avgustinmm
 */
@Singleton
@Provides
public class Instance implements Provider<Vertx>, AutoCloseable {

  private final Vertx vertx;
  
  @Inject
  public Instance(@Named("vertx.json") final VertxOptions options, final ClusterManager clusterManeger) {
    this.vertx = 
      Optional.ofNullable(clusterManeger)
        .map(cm -> {
          final CompletableFuture<Vertx> future = new CompletableFuture<>();
          Vertx.clusteredVertx(options.setClusterManager(cm), handler(future));
          return future;
        })
        .orElse(CompletableFuture.completedFuture(Vertx.vertx())) 
        .toCompletableFuture()
        .join();
  }
  
  @Override
  public Vertx get() {
    return vertx;
  }

  @Override
  public void close() {
    final CompletableFuture<Void> future = new CompletableFuture<>();
    vertx.close(handler(future));
    future.join();
  }
  
  private static <T> Handler<AsyncResult<T>> handler(final CompletableFuture<T> future) {
    return ar -> {
      if (ar.succeeded() ) {
        future.complete(ar.result());
      } else {
        future.completeExceptionally(ar.cause());
      }
    };
  }
}
