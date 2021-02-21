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

import java.util.concurrent.Executor;

import io.vertx.core.Context;
import io.vertx.core.Vertx;

public class VertxUtils {
  
  public static Executor ctxExecutor(final Vertx vertx) {
    return new Executor() {

      private final Context context = vertx.getOrCreateContext();;
      
      @Override
      public void execute(final Runnable command) {
        context.runOnContext(v -> command.run());
      }
    };
  }
}
