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

import javax.inject.Inject;

import io.grpc.MethodDescriptor;
import io.grpc.ServerMethodDefinition;
import io.grpc.ServerServiceDefinition;
import io.vertx.core.Vertx;
import io.vertx.ext.shell.command.CommandBuilder;
import io.vertx.ext.shell.command.CommandRegistry;

public class Cmd {

  @Inject
  public Cmd(final Server server, final Vertx vertx) {
    final CommandBuilder cb = CommandBuilder.command("grpc_ls");
    cb.processHandler(process -> {
      try {
        for (final ServerServiceDefinition ssd : server.ssdList()) {
          process.write(ssd.getServiceDescriptor().getName());
          process.write("\n");
          for (final ServerMethodDefinition<?, ?> smd : ssd.getMethods()) {
            final MethodDescriptor<?, ?> md = smd.getMethodDescriptor();
            process.write("  ");
            process.write(md.getFullMethodName());
            process.write(" \n");

            process.write("    ");
            process.write(md.getRequestMarshaller().toString().replace("\n", "\n    "));
            process.write(" \n");
            process.write("    ");
            process.write(md.getResponseMarshaller().toString().replace("\n", "\n    "));
            process.write(" \n");
          }
        }
      } catch (final Exception e) {
        process.write(e.toString());
        process.write("\n");
      } finally {
        process.end();
      }
    });
    CommandRegistry.getShared(vertx).registerCommand(cb.build(vertx));
  }
}
