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
package am24j.vertx;

import am24j.jcl.JarClassLoader;
import am24j.launch.Launcher;
import io.vertx.core.Vertx;
import io.vertx.ext.shell.command.CommandBuilder;
import io.vertx.ext.shell.command.CommandRegistry;

/**
 * @author avgustinmm
 */
public class ShellLauncher {

  public static void start(final Vertx vertx) {
    final CommandBuilder cb = CommandBuilder.command("launcher");
    cb.processHandler(process -> {
      final JarClassLoader jarClassLoader = Launcher.jarClassLoader();
      try {
        if (jarClassLoader == null) {
          process.write("No jarClassLoader avaialble!\n");
        } else {
          process.write("--------------- USAGE ----------------\n");
          process.write(jarClassLoader.toString());
          process.write("\n");
        }
      } finally {
        process.end();
      }
    });
    CommandRegistry.getShared(vertx).registerCommand(cb.build(vertx));
  }
}
