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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Assert;
import org.junit.Test;

import am24j.commons.Builder;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.grpc.VertxServerBuilder;

public class JsonConfigTest {

  private static final int IDLE_TIMEOUT = 1242345;
  private static final int HEADER_TABLES_SIZE = 1233;

  @Test
  public void testSSL() throws InterruptedException {
    final Vertx vertx = Vertx.vertx();
    try {
      final JsonObject json = new JsonObject();
      json.put(
        "useSsl", // test xyz(Handler<to be injected>) -> useSSL(Handler<HttpServerOptions>)
        new JsonObject()
          .put("idleTimeout", IDLE_TIMEOUT) // test setXuz inject -> setIdleTimeout
          .put(
            "initialSettings",
            new JsonObject()
             .put("headerTableSize", HEADER_TABLES_SIZE)) // test getXuz -> getIdleTimeoutUnit and inject
          );

      final VertxServerBuilder builder = VertxServerBuilder.forAddress(vertx, "host",  1999);
      Builder.inject(json, builder);

      final CountDownLatch latch = new CountDownLatch(1);
      final AtomicReference<AssertionError> error = new AtomicReference<>();
      builder.useSsl(hso -> {
        try {
          Assert.assertEquals("Idle timeout", IDLE_TIMEOUT, hso.getIdleTimeout());
          Assert.assertEquals("Header table size", HEADER_TABLES_SIZE, hso.getInitialSettings().getHeaderTableSize());
        } catch (final AssertionError e) {
          error.set(e);
        }
        latch.countDown();
      });
      latch.await();
      if (error.get() != null) {
        throw error.get();
      }
    } finally {
      vertx.close();
    }
  }
}
