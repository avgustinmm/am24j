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

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.Flow.Subscription;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

import javax.inject.Inject;
import javax.inject.Named;

import org.slf4j.Logger;

import am24j.commons.Ctx;
import am24j.rpc.avro.Proto;
import am24j.rpc.avro.RPCException;
import io.grpc.CallCredentials;
import io.grpc.CallOptions;
import io.grpc.ClientCall;
import io.grpc.ClientCall.Listener;
import io.grpc.ManagedChannel;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.MethodDescriptor.MethodType;
import io.grpc.Status;
import io.grpc.StatusException;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Vertx;

/**
 * GRPC client
 *
 * @author avgustinmm
 */
public class Client implements AutoCloseable {

  private static final Logger LOG = Ctx.logger("rpc.grpc.client");

  private final Vertx vertx;

  private final Future<String> deployment;
  private List<ClientVerticle> clientVerticles = Collections.synchronizedList(new ArrayList<>());

  @Inject
  public Client(@Named("grpc_client.json") final DeploymentOptions options, final Vertx vertx) {
    LOG.info("Start (options: {})", options.toJson());
    this.vertx = vertx;
    deployment = vertx.deployVerticle(() -> {
      final ClientVerticle clientVerticle = new ClientVerticle();
      clientVerticles.add(clientVerticle);
      return clientVerticle;
    }, options);
  }

  @Override
  public void close() {
    LOG.info("Close");
    deployment.map(deploymentID -> {
      vertx.undeploy(deploymentID);
      return null;
    });
  }

  @SuppressWarnings("unchecked")
  public <T> T service(final Supplier<String> credentialSupplier, final Class<T> clazz, final Class<?>... others) {
    final Class<?>[] interfaces = new Class<?>[1 + others.length];
    System.arraycopy(others, 0, interfaces, 0, others.length);
    interfaces[others.length] = clazz;
    return (T)Proxy.newProxyInstance(clazz.getClassLoader(), interfaces, new InvocationHandler() {

      @Override
      public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
        final CallOptions callOptions;
        final String credential = credentialSupplier == null ? null : credentialSupplier.get();
        if (credential == null) {
          callOptions = CallOptions.DEFAULT;
        } else {
          final CallCredentials credentials = new CallCredentials() {

            @Override
            public void applyRequestMetadata(
                final RequestInfo requestInfo,
                final Executor appExecutor,
                final MetadataApplier applier) {
              final Metadata meta = new Metadata();
              meta.put(Common.WWW_AUTHENTICATE, credential);
              applier.apply(meta);
            }

            @Override
            public void thisUsesUnstableApi() {}
          };
          callOptions = CallOptions.DEFAULT.withCallCredentials(credentials);
        }

        final MethodDescriptor<Object[], Object> methodDescriptor = Common.methodDescriptor(method, Proto.protocol(method.getDeclaringClass()));
        final ClientCall<Object[], Object> call = channel().newCall(methodDescriptor, callOptions);
        final Object result;
        if (methodDescriptor.getType() == MethodType.UNARY) {
          final CompletableFuture<Object> future = new CompletableFuture<>();
          call.start(new Unaryistener(call, future), new Metadata());
          result = future;
          call.sendMessage(args);
        } else {
          final Object[] realArgs = new Object[args.length - 1];
          System.arraycopy(args, 0, realArgs, 0, realArgs.length);
          call.start(new StreamListener(call, (Subscriber<Object>)args[realArgs.length]), new Metadata());
          result = null;
          call.sendMessage(realArgs);
        }
        call.halfClose();
        return result;
      }
    });
  }

  private ManagedChannel channel() {
    return clientVerticles.get(ThreadLocalRandom.current().nextInt(clientVerticles.size())).channel();
  }

  private static final class Unaryistener extends Listener<Object> {

    private final ClientCall<Object[], Object> call;
    private final CompletableFuture<Object> future;

    private Unaryistener(final ClientCall<Object[], Object> call, final CompletableFuture<Object> future) {
      this.call = call;
      this.future = future;
    }

    @Override
    public void onHeaders(final Metadata headers) {
      call.request(1);
    }

    @Override
    public void onMessage(final Object message) {
      if (message instanceof RPCException) {
        future.completeExceptionally(((RPCException)message).toRPC());
      } else {
        future.complete(message);
      }
    }

    @Override
    public void onClose(final Status status, final Metadata trailers) {
      if (status.isOk()) {
        if (!future.isDone()) {
          future.completeExceptionally(new IOException("Closed withoyt result!"));
        }
      } else {
        if (future.isDone()) {
          // TODO - just log
        } else {
          future.completeExceptionally(new StatusException(status, trailers));
        }
      }
    }
  }

  private final class StreamListener extends Listener<Object> {

    private final ClientCall<Object[], Object> call;
    private final Subscriber<Object> subscriber;

    private StreamListener(final ClientCall<Object[], Object> call, final Subscriber<Object> subscriber) {
      this.call = call;
      this.subscriber = subscriber;
    }

    @Override
    public void onHeaders(final Metadata headers) {
      subscriber.onSubscribe(new Subscription() {

        @Override
        public void request(final long n) {
          call.request(1);
        }

        @Override
        public void cancel() {
          call.cancel("Close", null);
        }
      });
    }

    @Override
    public void onReady() {
    }

    @Override
    public void onMessage(final Object message) {
      if (message instanceof RPCException) {
        subscriber.onNext(((RPCException)message).toRPC());
      } else {
        subscriber.onNext(message);
      }
    }

    @Override
    public void onClose(final Status status, final Metadata trailers) {
      if (status.isOk()) {
        subscriber.onComplete();
      } else {
        subscriber.onError(new StatusException(status, trailers));
      }
    }
  }
}
