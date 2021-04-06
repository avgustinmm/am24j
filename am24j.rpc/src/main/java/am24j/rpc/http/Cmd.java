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

import javax.inject.Inject;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.shell.command.CommandBuilder;
import io.vertx.ext.shell.command.CommandRegistry;

public class Cmd {

  @Inject
  public Cmd(final Server server, final Vertx vertx) {
    final CommandBuilder cb = CommandBuilder.command("http_ls");
    cb.processHandler(process -> {
      try {
        server.info().forEach((path, message) -> {
          process.write(path);
          process.write("\n");

          process.write("    Request: ");
          process.write(new JsonObject(message.getRequest().toString()).encodePrettily().replace("\n", "\n    "));
          process.write(" \n");
          process.write("    Response: ");
          process.write(new JsonObject(message.getResponse().toString()).encodePrettily().replace("\n", "\n    "));
          process.write(" \n");
          process.write("    Error: ");
          process.write(new JsonArray(message.getErrors().toString()).encodePrettily().replace("\n", "\n    "));
          process.write(" \n");
        });
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
