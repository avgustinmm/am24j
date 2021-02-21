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
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow.Subscriber;
import java.util.function.Supplier;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.avro.Protocol;
import org.apache.avro.Protocol.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import am24j.rpc.avro.Proto;
import am24j.rpc.avro.RPCException;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;

// single threads, mainly for test purposes 
public class Client implements AutoCloseable {

  // TODO get it via runtime ctx (?) (inject it ?)
  private static final Logger LOG = LoggerFactory.getLogger("am24j.rpc.http.client");
  
  private final boolean json;
  private final HttpClient client;
  
  @Inject
  public Client(@Named("http_client.json") final JsonObject options, final Vertx vertx) {
    LOG.info("Start (options: {})", options);
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
        final String path = "/rpc" + '/' + aProto.getName() + '/' + aMessage.getName();
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
              response.body().map(buff ->  {
                System.out.println(buff);
                return null;
              });
              // TODO - read stream on chunks and push them on subscriber (backpressure?)
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
}
