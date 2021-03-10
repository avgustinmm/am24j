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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.Flow.Subscription;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.avro.Protocol;
import org.slf4j.Logger;

import am24j.commons.ASync;
import am24j.commons.Ctx;
import am24j.commons.Reflect;
import am24j.commons.Utils;
import am24j.rpc.Auth;
import am24j.rpc.AuthVerfier;
import am24j.rpc.RPCException;
import am24j.rpc.Remote;
import am24j.rpc.Service;
import am24j.rpc.avro.Proto;
import am24j.vertx.VertxUtils;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.MethodDescriptor.MethodType;
import io.grpc.ServerCall;
import io.grpc.ServerCall.Listener;
import io.grpc.ServerCallHandler;
import io.grpc.ServerMethodDefinition;
import io.grpc.ServerServiceDefinition;
import io.grpc.Status;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Vertx;

/**
 * GRCPC Server
 *
 * @author avgustinmm
 */
public class Server implements AutoCloseable {

  private static final Logger LOG = Ctx.logger("rpc.grpc.server");

  private final List<AuthVerfier<Metadata>> authVerfiers;
  private final Vertx vertx;

  private final Future<String> deployment;

  @Inject
  public Server(
      @Remote final List<Object> services,
      final List<AuthVerfier<Metadata>> authVerfiers,
      @Named("grpc_server.json") final DeploymentOptions options,
      final Vertx vertx) {
    LOG.info("Start (options: {}, servicesL {})", options.toJson(), services);
    this.authVerfiers = authVerfiers;
    this.vertx = vertx;
    final List<ServerServiceDefinition> ssdList =
      services.stream()
        .flatMap(this::serviceDefinitions)
        .collect(Collectors.toList());
    if (LOG.isInfoEnabled()) {
      ssdList.forEach(ssd -> {
        LOG.info("  Start sertvice: {}", ssd.getServiceDescriptor().getName());
        ssd.getServiceDescriptor().getMethods().forEach(md -> {
          LOG.info("    > {}", md.getFullMethodName());
        });
      });
    }
    deployment = vertx.deployVerticle(() -> new ServerVerticle(ssdList), options);
  }

  @Override
  public void close() {
    LOG.info("Close");
    deployment.map(deploymentID -> {
      vertx.undeploy(deploymentID);
      return null;
    });
  }

  private Stream<ServerServiceDefinition> serviceDefinitions(final Object service) {
    return Arrays.stream(service.getClass().getInterfaces()) // only directly declared interfaces
      .filter(iClass -> Objects.nonNull(iClass.getAnnotation(Service.class)))
      .map(iClass -> serviceDefinition(iClass, service));
  }

  private ServerServiceDefinition serviceDefinition(final Class<?> iClass, final Object service) {
    final Protocol aProto = Proto.protocol(iClass);

    final ServerServiceDefinition.Builder builder = ServerServiceDefinition.builder(aProto.getName());
    Arrays.stream(iClass.getMethods()) // all methods - not only declared
      .collect(Collectors.toMap(Reflect::methodSig, Function.identity()))
      .values()
      .forEach(method -> {
        final MethodDescriptor<Object[], Object> md = Common.methodDescriptor(method, aProto);
        builder.addMethod(
          ServerMethodDefinition.create(
            md,
            new ServerCallHandler<Object[], Object>() {

              @Override
              public Listener<Object[]> startCall(final ServerCall<Object[], Object> call, final Metadata headers) {
                if (md.getType() == MethodType.UNARY) {
                  return new UnaryListener(call, headers, method, service);
                } else {
                  return new ServerStreamListener(call, headers, method, service);
                }
              }
            }));
      });
    return builder.build();
  }

  private final class UnaryListener extends BaseListener {

    private UnaryListener(
        final ServerCall<Object[], Object> call, final Metadata headers,
        final Method method, final Object service) {
      super(call, headers, method, service);
    }

    @Override
    protected void invoke(final Object[] args) {
      try {
        final Object result = method.invoke(service, args);
        ((CompletionStage<?>)result).whenCompleteAsync((r, t) -> {
          if (t == null) {
            call.sendMessage(r);
          } else {
            error(t);
          }
        }, vExecutor);
      } catch (final InvocationTargetException e) {
        error(e.getCause() == null ? e : e.getCause());
      } catch (final Throwable t) {
        error(t);
      }
    }
  }

  private final class ServerStreamListener extends BaseListener {

    private final CompletableFuture<Subscription> subscriptionFuture = new CompletableFuture<>(); // TODO via vertx could we set and use it in single thread
    private final Subscriber<Object> subscriber = new Subscriber<>() {

      @Override
      public void onSubscribe(final Subscription subscription) {
        vExecutor.execute(() -> subscriptionFuture.complete(subscription));
      }

      @Override
      public void onNext(final Object item) {
        vExecutor.execute(() -> {
          call.sendMessage(item);
          if (call.isReady()) {
            onReady();
          }
        });
      }

      @Override
      public void onError(final Throwable throwable) {
        vExecutor.execute(() -> {
          if (!subscriptionFuture.isDone()) {
            subscriptionFuture.completeExceptionally(throwable);
          }
          error(throwable);
        });
      }

      @Override
      public void onComplete() {
        vExecutor.execute(() -> call.close(Status.OK, new Metadata()));
      }
    };

    private ServerStreamListener(
        final ServerCall<Object[], Object> call, final Metadata headers,
        final Method method, final Object service) {
      super(call, headers, method, service);
    }

    @Override
    public void onCancel() {

    }

    @Override
    public void onReady() {
      subscriptionFuture.thenAcceptAsync(subscription -> subscription.request(1), vExecutor);
    }

    @Override
    protected void invoke(final Object[] args) {
      try {
        final Object[] realArgs = new Object[args.length + 1];
        System.arraycopy(args, 0, realArgs, 0, args.length);
        realArgs[args.length] = subscriber;
        method.invoke(service, realArgs);
      } catch (final InvocationTargetException e) {
        error(e.getCause() == null ? e : e.getCause());
      } catch (final Throwable t) {
        error(t);
      }
    }
  }

  private abstract class BaseListener extends Listener<Object[]> {

    protected final ServerCall<Object[], Object> call;
    protected final Method method;
    protected final Object service;

    protected final Executor vExecutor;

    private final CompletionStage<Auth> authFuture;
    private final CompletableFuture<Void> ready = new CompletableFuture<>(); // when halfClosed is received, then can send message

    private BaseListener(
        final ServerCall<Object[], Object> call, final Metadata headers,
        final Method method, final Object service) {
      this.call = call;
      this.method = method;
      this.service = service;

      vExecutor = VertxUtils.ctxExecutor(vertx);

      authFuture = ASync
        .sequentiallyGetSkipErrors(
          Utils.map(
            authVerfiers.iterator(),
            authVerifier -> authVerifier.verify(headers)))
        .whenCompleteAsync((auth, error) -> {
          if (error == null && auth != null) {
            call.request(1); // data, otherwise neither message non half is received
          } else {
            call.close(Status.UNAUTHENTICATED, new Metadata());
          }
        }, vExecutor)
        .thenCombine(ready, (auth, v) -> auth);
    }

    @Override
    public void onMessage(final Object[] args) {
      authFuture.thenAccept(ctx -> ctx.runAs(() -> invoke(args)));
    }

    @Override
    public void onHalfClose() {
      call.sendHeaders(new Metadata());
      ready.complete(null);
    }

    protected abstract void invoke(final Object[] args);

    protected void error(final Throwable t) {
      final String uuid = RPCException.uuid();
      LOG.error("[{}] Call failed!", uuid, t);
      call.sendMessage(new am24j.rpc.avro.RPCException().setUUID(uuid).setMessage(t.getMessage()).setType(t.getClass().getName()));
    }
  }
}