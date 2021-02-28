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
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.Flow.Subscription;
import java.util.function.Supplier;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.avro.Protocol;
import org.apache.avro.Protocol.Message;
import org.slf4j.Logger;

import am24j.commons.Ctx;
import am24j.rpc.avro.Proto;
import am24j.rpc.avro.RPCException;
import am24j.vertx.VertxUtils;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;

/**
 * HTTP RPC client. Single threaded.
 *
 * @author avgustinmm
 */
public class Client implements AutoCloseable {

  private static final Logger LOG = Ctx.logger("rpc.http.client");

  private static final String HTTP_RPC_ROOT = Ctx.prop("rpc.http.root", "/rpc");

  private final boolean json;
  private final HttpClient client;

  private final Vertx vertx;

  @Inject
  public Client(@Named("http_client.json") final JsonObject options, final Vertx vertx) {
    LOG.info("Start (options: {})", options);
    this.vertx = vertx;
    json = options.getBoolean("json", true);
    client = vertx.createHttpClient(new HttpClientOptions(options));
  }

  @Override
  public void close() {
    client.close();
  }

  @SuppressWarnings("unchecked")
  public <T> T service(final Supplier<String> credentialSupplier, final Class<T> clazz, final Class<?>... others) {
    final Class<?>[] interfaces = new Class<?>[1 + others.length];
    System.arraycopy(others, 0, interfaces, 0, others.length);
    interfaces[others.length] = clazz;
    return (T)Proxy.newProxyInstance(clazz.getClassLoader(), interfaces, new InvocationHandler() {

      @Override
      public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
        final Class<?> iClass = method.getDeclaringClass();
        final Protocol aProto = Proto.protocol(iClass);
        final Message aMessage = aProto.getMessages().get(Proto.methodName(method));
        final String path = HTTP_RPC_ROOT + '/' + aProto.getName() + '/' + aMessage.getName();
        final Object result;
        if (Proto.isStream(method)) {
          final Object[] realArgs = new Object[args.length - 1];
          System.arraycopy(args, 0, realArgs, 0, realArgs.length);
          final Subscriber<Object> subscriber = (Subscriber<Object>)args[args.length - 1];
          client
            .request(HttpMethod.POST, path)
            .compose(requst -> {
              final Buffer buff = Buffer.buffer(Proto.encodeReqy(aMessage.getRequest(), realArgs, json));
              return requst
                .putHeader("content-type", json ? "application/json" : "avro/binary")
                .putHeader("content-length", String.valueOf(buff.length()))
                .send(buff);
            })
            .compose(response -> {
              final StreamHandler streamHandler = new StreamHandler(aMessage, json, response, subscriber, VertxUtils.ctxExecutor(vertx));
              subscriber.onSubscribe(streamHandler);
              response.handler(streamHandler).end(ar -> {
                if (ar.succeeded()) {
                  subscriber.onComplete();
                } else {
                  subscriber.onError(ar.cause());
                }
              });
              return Future.succeededFuture();
            })
            .recover(t -> {
              LOG.error("Call failed!", t);
              subscriber.onError(t);
              return Future.failedFuture(t);
            });
          result = null;
        } else {
          final CompletableFuture<Object> future = new CompletableFuture<>();
          client
            .request(HttpMethod.POST, path)
            .compose(request -> {
              final Buffer buff = Buffer.buffer(Proto.encodeReqy(aMessage.getRequest(), args, json));
              return request
               .putHeader("content-type", json ? "application/json" : "avro/binary")
               .putHeader("content-length", String.valueOf(buff.length()))
               .send(buff);
            })
            .compose(HttpClientResponse::body)
            .compose(body -> {
              LOG.debug("Response body: {}", body);
              final Object reps = Proto.decodeResp(aMessage.getResponse(), aMessage.getErrors(), new ByteArrayInputStream(body.getBytes()), json);
              if (reps instanceof RPCException) {
                future.completeExceptionally((RPCException)reps);
              } else {
                future.complete(reps);
              }
              return Future.succeededFuture();
            })
            .recover(t -> {
              LOG.error("Call failed!", t);
              future.completeExceptionally(t);
              return Future.failedFuture(t);
            });
          result = future;
        }
        return result;
      }
    });
  }

  private static class StreamHandler extends InputStream implements Handler<Buffer>, Subscription {

    private final Message aMessage;
    private final boolean json;
    private final HttpClientResponse response;
    private final Subscriber<Object> subscriber;
    private final Executor vExecutor;

    private final List<Buffer> buffers = new LinkedList<>();
    private int bufPos;
    private int pos;

    private long requested;

    private StreamHandler(final Message aMessage, final boolean json, final HttpClientResponse response, final Subscriber<Object> subscriber, final Executor vExecutor) {
      this.aMessage = aMessage;
      this.json = json;
      this.response = response;
      this.subscriber = subscriber;
      this.vExecutor = vExecutor;
      response.pause();
    }

    @Override
    public void handle(final Buffer buff) {
      buffers.add(buff);
      if (requested <= 0) {
        response.pause();
        return; // no requested
      }
      while (!buffers.isEmpty()) {
        final int bufPos = this.bufPos;
        final int pos = this.pos;
        try {
          final Object decoded = Proto.decodeResp(aMessage.getResponse(), aMessage.getErrors(), this, json);
          if (decoded instanceof RPCException) {
            subscriber.onError(((RPCException)decoded).toRPC());
          } else {
            subscriber.onNext(decoded);
          }
          if (requested <= 0) {
            response.pause();
          }
          if (this.bufPos > 0) { // remove read
            for (int i = 0; i < this.bufPos; i++) {
              buffers.remove(0);
            }
            this.bufPos = 0;
          }
        } catch (final Exception e) {
          // incomplete record, return mark back
          this.bufPos = bufPos;
          this.pos = pos;
          return;
        }
      }
    }

    @Override
    public int read() throws IOException {
      if (bufPos < buffers.size()) {
        final Buffer buff = buffers.get(bufPos);
        if (pos == buff.length()) {
          bufPos++;
          pos = 0;
          return read();
        }
        return buff.getByte(pos++);
      } else {
        return -1;
      }
    }

    @Override
    public void request(final long n) {
      vExecutor.execute(() -> {
        if (n < 0) throw new IllegalArgumentException("Request must be non-negative! Found: " + n + "!");
        if (n > 0) {
          final long newRquested = requested + n;
          requested = newRquested > requested ? newRquested : Long.MAX_VALUE; // if less, overflow
          if (requested > 0) {
            response.resume();
          }
        }
      });
    }

    @Override
    public void cancel() {
      vExecutor.execute(response::end);
    }
  }
}
