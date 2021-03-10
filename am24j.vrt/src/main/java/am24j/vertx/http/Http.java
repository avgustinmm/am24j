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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;

import am24j.commons.Ctx;
import am24j.commons.Patterns;
import am24j.vertx.Component;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Verticle;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpServerRequest;

/**
 * Provides http server in Vertx that routes to plugable {@link HttpHandler}s
 *
 * @author avgustinmm
 */
@Singleton
public class Http extends Component<Verticle> {

  private static final Logger LOG = Ctx.logger("HTTP");

  @Inject
  public Http(final List<HttpHandler> handlers, @Named("http_server.json") final DeploymentOptions options, final Vertx vertx) {
    super(() -> new AbstractVerticle() {

      private HttpServer server;
      private Map<String, Pattern> matchers = new HashMap<>();

      @Override
      public void start(final Promise<Void> startPromise) throws Exception {
        LOG.info("Start with handlers: {}", handlers);

        server = vertx.createHttpServer(new HttpServerOptions(config()));
        server.requestHandler(request -> {
          for (final HttpHandler handler : handlers) {
            final Matcher matcher = matchers.computeIfAbsent(handler.path(), path -> Patterns.forWildCard(path, true)).matcher(request.path());
            if (matcher.matches()) {
              handler.handle(request);
              return;
            }
          }
        });
        server.listen().map(v -> (Void)null).onComplete(startPromise);
      }

      @Override
      public void stop(final Promise<Void> stopPromise) throws Exception {
        server.close(stopPromise);
      }
    }, options, vertx);
  }

  public interface HttpHandler extends Handler<HttpServerRequest> {

    public String path();
  }
}
