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
package am24j.vertx.http;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Qualifier;
import javax.inject.Singleton;

import org.jboss.resteasy.plugins.server.vertx.VertxRequestHandler;
import org.jboss.resteasy.plugins.server.vertx.VertxResteasyDeployment;
import org.slf4j.Logger;

import am24j.commons.Ctx;
import am24j.vertx.http.Http.HttpHandler;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;

/**
 * Provides JAX-RS support as a {@link HttpHandler} for {@link Http}
 *
 * @author avgustinmm
 */
@Singleton
public class RestEasy implements HttpHandler, AutoCloseable {

  @Retention(RetentionPolicy.RUNTIME)
  @Qualifier
  public static @interface JAXRS {}

  private static final Logger LOG = Ctx.logger("RestEasy");

  private final VertxResteasyDeployment deployment = new VertxResteasyDeployment();
  private final Handler<HttpServerRequest> handler;

  @Inject
  public RestEasy(@JAXRS final List<Object> services, final Vertx vertx) {
    LOG.info("Start with services: {}", services);

    final ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
    // in order to load default providers (from META-INF/services ) we set thus class loader (if null, it doesn't load them, maybe loaded with system?
    Thread.currentThread().setContextClassLoader(RestEasy.class.getClassLoader());
    try {
      deployment.start();
      services.forEach(service -> deployment.getRegistry().addSingletonResource(service, "/jaxrs"));

      handler = new VertxRequestHandler(vertx, deployment);
    } finally {
      Thread.currentThread().setContextClassLoader(contextClassLoader);
    }
  }

  @Override
  public String path() {
    return Ctx.prop("vrt.http.jaxrs.root", "/jaxrs") + "/*";
  }

  @Override
  public void handle(final HttpServerRequest request) {
    handler.handle(request);
  }

  @Override
  public void close() {
    deployment.stop();
  }
}
