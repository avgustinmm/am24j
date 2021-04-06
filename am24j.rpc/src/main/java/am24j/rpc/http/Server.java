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
package am24j.rpc.http;

import java.io.ByteArrayInputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.Flow.Subscription;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.avro.Protocol;
import org.apache.avro.Protocol.Message;
import org.slf4j.Logger;

import am24j.commons.ASync;
import am24j.commons.Ctx;
import am24j.commons.Reflect;
import am24j.commons.Types;
import am24j.commons.Utils;
import am24j.rpc.AuthVerfier;
import am24j.rpc.RPCException;
import am24j.rpc.Remote;
import am24j.rpc.Service;
import am24j.rpc.avro.Proto;
import am24j.vertx.VertxUtils;
import am24j.vertx.http.Http;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;

/**
 * HTTP RPC Server
 *
 * @author avgustinmm
 */
@Singleton
public class Server implements Http.HttpHandler {

  public static final String HTTP_RPC_ROOT = Ctx.prop("rpc.http.root", "/rpc");

  private static final Logger LOG = Ctx.logger("rpc.http.server");

  private final List<AuthVerfier<HttpServerRequest>> authVerfiers;
  private final Map<String, MethodHandler> methodsMap;

  private final Vertx vertx;

  @Inject
  public Server(
      @Remote final List<Object> services,
      final List<AuthVerfier<HttpServerRequest>> authVerfiers,
      final Vertx vertx) {
    LOG.info("Start (services: {})", services);
    this.authVerfiers = authVerfiers;
    this.vertx = vertx;
    methodsMap =
      services.stream()
        .flatMap(this::methodDescriptors)
        .collect(Collectors.toMap(MethodHandler::path, Function.identity()));
    if (LOG.isInfoEnabled()) {
      methodsMap.values().forEach(md -> LOG.info("    > {}", md.path()));
    }
  }

  @Override
  public String path() {
    return HTTP_RPC_ROOT + "/*";
  }

  @Override
  public void handle(final HttpServerRequest request) {
    final Executor vExecutor = VertxUtils.ctxExecutor(vertx);
    request.pause(); // otherwise ? mark as read ?
    ASync
      .sequentiallyGetSkipErrors(
        Utils.map(
          authVerfiers.iterator(),
          authVerifier -> authVerifier.verify(request)))
      .whenCompleteAsync((auth, error) -> {
        if (error == null) {
          final MethodHandler handler = methodsMap.get(request.path());
          if (handler == null) {
            LOG.debug("Method not found: {}!", request.uri());
            respond(request.response(), 404, true, new JsonObject().put("error", "Not found: " + request.path() + "!").encodePrettily());
          } else {
            if (auth == null) {
              handler.handle(request, vExecutor);
            } else {
              auth.runAs(() -> handler.handle(request, vExecutor));
            }
          }
        } else {
          if (request.getHeader("Authorization") == null) {
            request.response().putHeader("WWW-Authenticate:", authVerfiers.get(0).toString());
            respond(request.response(), 401, true, "");
          } else {
            respond(request.response(), 403, true, new JsonObject().put("error", error.toString()).encodePrettily());
          }
        }
      }, vExecutor);
  }

  public Map<String, Message> info() {
    final Map<String, Message> map = new HashMap<>();
    methodsMap.forEach((k, v) -> {
      map.put(k, v.aMessage);
    });
    return map;
  }

  private Stream<MethodHandler> methodDescriptors(final Object service) {
    return Arrays.stream(service.getClass().getInterfaces()) // only directly declared interfaces
      .filter(iClass -> Objects.nonNull(iClass.getAnnotation(Service.class)))
      .flatMap(iClass -> methodDescriptors(iClass, service));
  }

  private Future<Void> respond(final HttpServerResponse response, final int status, final boolean json, final String content)  {
    LOG.debug("Response content: {}", content);
    return response.setStatusCode(status)
      .putHeader("content-tyoe", json ? "application/json" : "avro/binary")
      .putHeader("content-length", String.valueOf(content.length()))
      .write(content).map(v -> {
        response.end();
        return null;
      });
  }

  private Stream<MethodHandler> methodDescriptors(final Class<?> iClass, final Object service) {
    final Protocol aProto = Proto.protocol(iClass);
    return Arrays.stream(iClass.getMethods()) // all methods - not only declared
      .collect(Collectors.toMap(Reflect::methodSig, Function.identity()))
      .values()
      .stream()
      .map(method -> new MethodHandler(method, service, aProto));
  }

  private class MethodHandler {

    private final Method method;
    private final Object service;
    private final Message aMessage;

    private final String path;
    private final boolean stream;

    private MethodHandler(final Method method, final Object service, final Protocol aProto) {
      this.method = method;
      this.service = service;
      aMessage = aProto.getMessages().get(Proto.methodName(method));

      path = HTTP_RPC_ROOT + '/' + aProto.getName() + '/'  + aMessage.getName();
      stream = Proto.isStream(method);
    }

    private Future<Void> handle(final HttpServerRequest request, final Executor vExecutor) {
      final boolean json = request.method() == HttpMethod.GET || !"avro/binary".equals(request.getHeader("content-type"));
      return handle0(request, json, vExecutor).recover(t -> {
        final String uuid = RPCException.uuid();
        LOG.error("[{}] Call failed!", uuid, t);
        am24j.rpc.avro.RPCException rpcExc = new am24j.rpc.avro.RPCException().setUUID(uuid).setMessage(t.getMessage()).setType(t.getClass().getName());
        final String jsonTesp = stream(rpcExc, json);
        return respond(request.response(), 500, json, jsonTesp);
      });
    }
    private Future<Void> handle0(final HttpServerRequest request, final boolean json, final Executor vExecutor) {
      try {
        if (stream) {
          return parse(request, json, stream).compose(args -> stream(args, request.response(), json, vExecutor));
        } else {
          return parse(request, json, stream).compose(args -> call(args, request, json, vExecutor));
        }
      } catch (final Throwable t) {
        return Future.failedFuture(t);
      }
    }

    private String path() {
      return path;
    }

    @SuppressWarnings("unchecked")
    private Future<Void> call(final Object[] args, final HttpServerRequest request, final boolean json, final Executor vExecutor) {
      final Promise<Void> promise = Promise.promise();
      try {
        LOG.info("[{}] Call {}, args: {}", path(), method, args);
        ((CompletionStage<Object>)method.invoke(service, args)).whenCompleteAsync((resp, error) -> {
          try {
            if (error == null) {
              final String jsonTesp = stream(resp, json);
              respond(request.response(), jsonTesp.length() == 0 ? 204 : 200, json, jsonTesp).onComplete(promise);
            } else {
              final String uuid = RPCException.uuid();
              final String jsonTesp = stream(new am24j.rpc.avro.RPCException().setUUID(uuid).setMessage(error.getMessage()).setType(error.getClass().getName()), json);
              respond(request.response(), 500, json, jsonTesp).onComplete(promise);
            }
          } catch (final Throwable t) {
            promise.fail(t);
          }
        }, vExecutor);
      } catch (final Throwable t) {
        promise.fail(t instanceof InvocationTargetException && t.getCause() != null ? t.getCause() : t);
      }
      return promise.future();
    }

    private Future<Void> stream(final Object[] args, final HttpServerResponse response, final boolean json, final Executor vExecutor) {
      final Promise<Void> promise = Promise.promise();
      response.setChunked(true);
      try {
        final Subscriber<Object> subscriber = new Subscriber<>() {

          private Subscription subscription;

          @Override
          public void onSubscribe(final Subscription subscription) {
            vExecutor.execute(() -> {
              this.subscription = subscription;
              promise.complete();
              subscription.request(1);
            });
          }

          @Override
          public void onNext(final Object item) {
            vExecutor.execute(() -> {
              response.write(stream(item, json));
              subscription.request(1); // no backpressure
            });
          }

          @Override
          public void onError(final Throwable throwable) {
            vExecutor.execute(() -> {
              response.write(stream(throwable, json));
            });
          }

          @Override
          public void onComplete() {
            vExecutor.execute(() -> response.end());
          }
        };
        try {
          final Object[] realArgs = new Object[args.length + 1];
          System.arraycopy(args, 0, realArgs, 0, args.length);
          realArgs[args.length] = subscriber;
          method.invoke(service, realArgs);
        } catch (final InvocationTargetException e) {
          promise.fail(e.getCause() == null ? e : e.getCause());
        } catch (final Throwable t) {
          promise.fail(t);
        }
      } catch (final Throwable t) {
        promise.fail(t);
      }
      return promise.future();
    }

    private Future<Object[]> parse(final HttpServerRequest request, final boolean json, final boolean stream) {
      if (request.method() == HttpMethod.GET) {
        final Object[] params = new Object[method.getParameterCount() + (stream ? -1 : 0)];
        for (int i = params.length; i-- > 0;) {
          String value = request.getParam("arg_" + i);
          if (value == null) {
            value = request.getParam(String.valueOf(i));
          }
          if (value != null) {
//            final Schema.Field field = aMessage.getRequest().getField("arg_" + i);
            params[i] = Types.toType(value, method.getParameterTypes()[i]);
          }
        }
        return Future.succeededFuture(params);
      }

      final String contentLength = request.getHeader("content-length");
      LOG.debug("Received: {}", contentLength);
      request.resume();
      return request.body().map(body -> {
        LOG.debug("Request body: {}", body);
        return body;
      }).map(body -> Proto.decodeReq(aMessage.getRequest(), new ByteArrayInputStream(body.getBytes()), json));
    }

    private String stream(final Object resp, final boolean json) {
      return new String(Proto.encodeResp(aMessage.getResponse(), aMessage.getErrors(), resp, json), StandardCharsets.UTF_8);
    }
  }
}
