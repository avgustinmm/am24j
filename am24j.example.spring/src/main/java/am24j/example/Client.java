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
package am24j.example;

import javax.inject.Inject;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FullyQualifiedAnnotationBeanNameGenerator;

import am24j.example.hellowold.RPCInterface;
import am24j.rpc.spring.GRPCConfig;
import am24j.vertx.VertxInstance;
import am24j.vrt.spring.VRTConfig;

@Configuration
public class Client {

  // Client application component, injected whit remote service
  public static final class HelloWorldPrinter {

    @Inject
    public HelloWorldPrinter(final RPCInterface service) {
      System.out.println("RPC call returned:\n  " + service.hello("World (" + System.currentTimeMillis() + ")").toCompletableFuture().join());
    }
  }

  @Bean
  public RPCInterface service(final ApplicationContext ctx) {
    return ctx.getBean(am24j.rpc.grpc.Client.class).service(() -> "user:pass", RPCInterface.class);
  }

  public static void main(final String[] args) throws Exception {
    try (final AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext()) {
      ctx.setBeanNameGenerator(FullyQualifiedAnnotationBeanNameGenerator.INSTANCE);
      ctx.register(
        VRTConfig.class,
        GRPCConfig.class,

        VertxInstance.class,
        am24j.rpc.grpc.Client.class,
        Client.class,
        HelloWorldPrinter.class);
      ctx.refresh(); // call and print on initialization of HellWorkdPrinter
    }
  }
}
