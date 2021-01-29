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
package am24j.rt.hz;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import org.slf4j.Logger;

import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;

import am24j.inject.annotation.Provides;
import am24j.rt.Ctx;
import io.vertx.core.json.JsonObject;

/**
 * @author avgustinmm
 */
@Provides
public class Instance implements Provider<HazelcastInstance>, AutoCloseable {

  private static final Logger LOG = Ctx.logger("Hazelcast");
  
  private final HazelcastInstance hazelcast;
  
  @Inject
  public Instance(@Named("hazelcast.json") final JsonObject clusterConfig) throws IOException {
    hazelcast = hazelcast(clusterConfig);
  }

  @Override
  public HazelcastInstance get() {
    return hazelcast;
  }

  @Override
  public void close() {
    hazelcast.shutdown();
  }
  
  private HazelcastInstance hazelcast(final JsonObject clusterConfig) throws IOException {
    System.setProperty("hazelcast.logging.type", "slf4j"); // set logger
    
    final Config config = new Config();
    
    LOG.info("Apply cluster json config");
    apply(clusterConfig, config);

    LOG.info("Creating cluster (config: {}) ...", config);
    return Hazelcast.newHazelcastInstance(config);
  }
  
  // create config object from json following mapping -> x -> setX()
  private static void apply(final JsonObject json, final Object target) {
    json.forEach(e -> {
      try {
        final Object value = e.getValue(); 
        if (value instanceof JsonObject) {
          final String key = "get" + e.getKey().toLowerCase();
          for (final Method method : target.getClass().getMethods()) {
            if (key.equals(method.getName().toLowerCase()) && method.getParameterCount() == 0) {
              apply((JsonObject)e.getValue(), method.invoke(target));
              break;
            }
          }
        } else {
          final String key = "set" + e.getKey().toLowerCase();
          for (final Method method : target.getClass().getMethods()) {
            if (key.equals(method.getName().toLowerCase()) && method.getParameterCount() == 1) {
              method.invoke(target, am24j.rt.config.Config.toType(e.getValue(), method.getParameterTypes()[0]));
              break;
            }
          }
        }
      } catch (final Throwable t) {
        throw toRuntime(fromInvocation(t));
      }
    });
  }
  
  private static RuntimeException toRuntime(final Throwable t) {
    throw t instanceof RuntimeException ? (RuntimeException)t : new RuntimeException(t);
  }

  private static Throwable fromInvocation(final Throwable t) {
    if (t instanceof InvocationTargetException) {
      if (t.getCause() != null) {
        return t.getCause();
      }
    }
    return t;
  }  
}