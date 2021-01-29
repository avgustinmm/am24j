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

import java.lang.reflect.InvocationTargetException;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;

import am24j.rt.Ctx;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.ext.shell.ShellVerticle;

/**
 * @author avgustinmm
 */
@Singleton
public class Shell extends Component<ShellVerticle> {

  private static final Logger LOG = Ctx.logger("Shell");
  
  @Inject
  public Shell(final ShellVerticle verticle, @Named("shell.json") final DeploymentOptions options, final Vertx vertx) {
    super(verticle, options, vertx);
    try {
      Class.forName(Shell.class.getPackage().getName() + ".ShellLauncher").getMethod("start", Vertx.class).invoke(null, vertx);
    } catch (final ClassNotFoundException e) {
      LOG.debug("No ShellLauncher class."); 
    } catch (final IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException  | SecurityException e) {
      LOG.error("Failed to register ShellLauncher!", e);
    }
  }
}
