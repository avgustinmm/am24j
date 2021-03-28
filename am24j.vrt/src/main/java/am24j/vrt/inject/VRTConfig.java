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
package am24j.vrt.inject;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import org.slf4j.Logger;

import am24j.commons.Ctx;
import am24j.commons.Types;
import am24j.config.Config;
import am24j.inject.InjectException;
import am24j.inject.Injector;
import am24j.inject.spi.Resolver;
import io.vertx.core.json.JsonObject;

/**
 * Adds {@link Injector}'s {@link VRTConfig} that resolves named (having @ {@link Named} annotation)
 * {@link JsonObject} and objects having single {@link JsonObject} argument constructors.
 * Implicitly would instantiate config
 *
 * @author avgustinmm
 */
public class VRTConfig {

  private static final Logger LOG = Ctx.logger("Config");

  @SuppressWarnings("rawtypes")
  @Inject
  public VRTConfig(final Config config, final Injector injector) {
    injector.add((key, point, inj) -> {
      if (key.type() instanceof Class) {
        final Class clazz = (Class)key.type();
        try  {
          final String name = key.qualifer().filter(q -> q instanceof Named).map(q -> ((Named)q).value()).orElse(null);
          if (name != null) {
            // try property
            if (clazz == boolean.class || clazz == Boolean.class) {
              return () -> Ctx.boolProp(name);
            } else if (clazz == int.class || clazz == Integer.class) {
              return () -> Ctx.intProp(name);
            } else if (clazz == long.class || clazz == Long.class) {
              return () -> Ctx.longProp(name);
            } else if (clazz == float.class || clazz == Float.class) {
              return () -> Ctx.floatProp(name);
            } else if (clazz == double.class || clazz == Double.class) {
              return () -> Ctx.doubleProp(name);
            } else if (clazz == String.class) {
              return () -> Ctx.prop(name);
            }

            // try config file - support JsonObject and object with JsonObject constructor
            final byte[] configResource = config.resource(name);
            if (configResource != null) {
              final JsonObject json = new JsonObject(new String(configResource, StandardCharsets.UTF_8));

              if (clazz == JsonObject.class) {
                return () -> json;
              } else if (clazz == Map.class) {
                return () -> json.getMap();
              } else if (Annotation.class.isAssignableFrom(clazz)) {
                return () -> Proxy.newProxyInstance(clazz.getClassLoader(), new Class<?>[] {clazz}, (proxy, method, args) -> {
                  final Object value = json.getValue(method.getName());
                  return value == null ? method.getDefaultValue() : Types.toType(value, method.getReturnType());
                });
              } else {
                try {
                  @SuppressWarnings("unchecked")
                  final Constructor<Object> constr =  clazz.getDeclaredConstructor(JsonObject.class);
                  try { // if possible
                    constr.setAccessible(true);
                  } catch (final Throwable t) {}
                  final Object obj = constr.newInstance(json);
                  return () -> obj;
                } catch (final NoSuchMethodException e) {
                  LOG.debug(".. no Json opbject const");
                }
              }
            }
          }
        } catch (final InvocationTargetException e) {
          throw InjectException.of(e.getCause());
        } catch (final Exception e) {
          throw InjectException.of(e);
        }
      }
      throw Resolver.NOT_FOUND;
    });
  }
}
