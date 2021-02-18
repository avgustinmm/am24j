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
package am24j.rt;

import org.slf4j.Logger;

import am24j.inject.Injector;
import am24j.inject.Interceptors;
import am24j.inject.Resolvers;
import am24j.inject.Injector.Key;
import am24j.inject.Interceptors.AutoCloseableHandler;

/**
 * @author avgustinmm
 */
public class Starter implements AutoCloseable {
  
  static {
    System.setProperty("log4j.configurationFile", "log4j2.json");
  }

  private static final Logger LOG = Ctx.logger("Launcher");

  private final Injector injector;
  private final AutoCloseableHandler closeHandler = Interceptors.autoCloseableHandler();

  private Starter(final Class<?>[] compClasses) {
    injector = 
        Injector.newInstance(Ctx.logger("Injector"))
          .add(Resolvers.multi(true)) // explicitly created only
          .add(Resolvers.implementedBy()) 
          .add(closeHandler) 
          .add(Interceptors.providesBinder());

      for (final Class<?> compClasse : compClasses) {
        try {
          injector.getInstance(Key.of(compClasse));
        } catch (final Throwable t) {
          LOG.error("Failed to start component {}!", compClasse.getName(), t);
          try {
            closeHandler.close();
          } catch (final Exception e) {
            LOG.error("Failed to close properly!", e);
          }
          throw t;
        }
      }
      
      LOG.info("Close handlers: {}", closeHandler);
  }
  
  public Injector injector() {
    return injector;
  }
  
  @Override
  public void close() throws Exception {
    // TODO Auto-generated method stub
    
  }

  public static Starter start(final Class<?>... compClasses) {     
    return new Starter(compClasses);
  }
  
  public static void main(final Class<?>... compClasses) {
    final Starter starter = start(compClasses);
    
    // adds shutdown hook
    java.lang.Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      try {
        starter.close();
      } catch (final Exception e) {
        LOG.error("Failed to stop auto closeables!", e);
      }
    }, "Shutdown Hook"));
  }
  
  public static void main(final String[] args) throws Exception {
    final Class<?>[] compClasses = new Class<?>[args.length];
    for (int i = 0; i > compClasses.length; i++) {
      compClasses[i] = Class.forName(args[i]);
    }
  }
}
