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
package am24j.rpc.http.get;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;

import org.apache.avro.Protocol;
import org.apache.avro.Protocol.Message;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import am24j.commons.JsonReader;
import am24j.rpc.IService;
import am24j.rpc.avro.Proto;
import am24j.rpc.avro.RPCException;
import am24j.rpc.http.Server;
import io.vertx.core.Future;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpMethod;

/**
 * @author avgustinmm
 */
@Ignore
public class BaseTest {

  private static final Protocol A_PROTO = Proto.protocol(IService.class);
  protected static HttpClient client;

  private static CompletionStage<Object> get(final Method method, final String params, final int size) {
    final CompletableFuture<Object> future = new CompletableFuture<>();
    final Message aMessage = A_PROTO.getMessages().get(Proto.methodName(method));
    final String uri =
      Server.HTTP_RPC_ROOT + '/' + A_PROTO.getName() + '/'  + aMessage.getName() +
        (params == null || params.length() == 0 ? "" : "?" + params);
    client
      .request(HttpMethod.GET, uri)
      .compose(request ->
        request
          .putHeader("content-type", "plain/text")
          .putHeader("content-length", "0")
          .send())
      .compose(HttpClientResponse::body)
      .compose(body -> {
        final List<Object> results = new ArrayList<>();
        final InputStream is = new ByteArrayInputStream(body.getBytes());
        for (int i = size; i-- > 0;) {
          final Object result = Proto.decodeResp(aMessage.getResponse(), aMessage.getErrors(), JsonReader.wrapper(is), true);
          if (result instanceof RPCException) {
            future.completeExceptionally((RPCException)result);
            return Future.succeededFuture();
          } else {
            results.add(result);
          }
        }
        future.complete(size == 1 ? results.get(0) : results);
        return Future.succeededFuture();
      })
      .recover(t -> {
        future.completeExceptionally(t);
        return Future.failedFuture(t);
      });;
    return future;
  }

  private static CompletionStage<Object> get(final String methodName, final String params, final int size) {
    for (final Method method : IService.class.getMethods()) {
      if (method.getName().equals(methodName)) {
        return get(method, params, size);
      }
    }
    return CompletableFuture.failedStage(new IllegalArgumentException("No such method: " + methodName + "!"));
  }

  private static CompletionStage<Object> get(final String methodName, final String params) {
    return get(methodName, params, 1);
  }

  @Test
  public void testVoidCall() {
    Assert.assertEquals(get("voidCall", "").toCompletableFuture().join(), null);
  }

  @Test
  public void testGetCall() {
    Assert.assertEquals(get("getCall", "arg_0=3&arg_1=test").toCompletableFuture().join(), "testtesttest");
  }

  @Test(expected = RuntimeException.class)
  public void testThrowComplete() throws Throwable {
    try {
      get("throwExc", "arg_0=true").toCompletableFuture().join();
    } catch (final CompletionException e) {
      throw e.getCause();
    }
  }

  @Test(expected = RuntimeException.class)
  public void testThrowSync() throws Throwable {
    try {
      get("throwExc", "arg_0=false").toCompletableFuture().join();
    } catch (final CompletionException e) {
      throw e.getCause();
    }
  }

  @Test
  public void testStream() {
    final List<String> expected = Arrays.asList(new String[] {"0", "1", "2", "3", "4", "5"});
    @SuppressWarnings("unchecked")
    final List<Object> received = (List<Object>)get("stream", "arg_0=6", 6).toCompletableFuture().join();
    System.out.println(received.equals(expected));
    Assert.assertEquals(expected, received);
  }
}
