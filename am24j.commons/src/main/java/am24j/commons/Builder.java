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
package am24j.commons;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;

/**
 * Create or inject objects from /with a json recursively if needed. Objects are created using empty constructor.
 * Injecting is made as follows. For every key finds (orderly) a method:
 * <ol>
 * <li>standard  setter, e.g. setXyz(type) and inject</li>
 * <li>optimized setter, e.g.  xyz(type) and inject</li>
 * <li>standard time setter, e.g.  setXyz(number, TimeUnit)</li>
 * <li>optimized time setter, e.g.  xyz(number, TimeUnit)</li>
 * </ol>
 * , then if value is a json object:
 * <ol>
 * <li>get object via standard getter, e.g. getXyz() and inject the output</li>
 * <li>get object via optimized getter, e.g. xyz() and inject the output</li>
 * <li>method with {@link Handler} argument that received target to inject, e.g. xyz(Handler), then inject object received in the {@link Handler#handle(Object)}</li>
 * </ol>
 * If value is not a json object but true (boolean or string true) interpret is as a flag setter - just call it.
 * If false - skip it.
 *
 * @author avgustinmm
 */
public class Builder {

  private Builder() {}

  public static <T> T create(final String json, final Class<T> type) {
    return create(new JsonObject(json), type);
  }

  public static <T> T create(final JsonObject json, final Class<T> type) {
    final Constructor<T> constr;
    try {
      constr = type.getConstructor();
    } catch (final NoSuchMethodException e) {
      throw new IllegalArgumentException("Can't instantiiate " + type.getName() + "! No empty constrruvtor!", e);
    }
    try {
      return inject(json, constr.newInstance());
    } catch (final InvocationTargetException e) {
      throw RExc.toRuntime(e.getCause() == null ? e : e.getCause());
    } catch (final Exception e) {
      throw RExc.toRuntime(e);
    }
  }

  public static <T> T inject(final String json, final T target) {
    return inject(new JsonObject(json), target);
  }

  public static <T> T inject(final JsonObject json, final T target) {
    json.forEach(e -> {
      try {
        final String key = e.getKey();
        final Object value = e.getValue();

        // 1. standard or optimized setter -> setXyz(type) / xyz(type)
        final Method setter = setter(key, target.getClass());
        if (setter != null) {
          final Object adapted =
            value instanceof JsonObject ?
              create((JsonObject)value, setter.getParameterTypes()[0]) :
              Types.toType(value, setter.getParameterTypes()[0]);
          setter.invoke(target, adapted);
          return;
        }

        // 2. standard or optimized time setter -> setXyz(<number>, TimeUnit) or xyz(<number>, TimeUnit)
        final Method timeSetter = timeSetter(key, target.getClass());
        if (timeSetter != null) {
          final Duration duration  = Types.toType(value, Duration.class);
          timeSetter.invoke(
            target,
            Types.toType(String.valueOf(duration.toMillis()), timeSetter.getParameterTypes()[0]),
            TimeUnit.MILLISECONDS);
          return;
        }

        final Method getter = getter(key, target.getClass());
        if (value instanceof JsonObject) {
          // 3. get object via standard or optimized getter to inject <to inject getXyz() / xyz()
          if (getter != null) {
            inject((JsonObject)value, getter.invoke(target));
            return;
          }

          // 4. method with handler that received target to inject
          // -> xyz(Handler<ObjectToConfigure>) and inject value passed to handler
          final Method handler = handler(key, target.getClass());
          if (handler != null) {
            handler.invoke(target, new Handler<Object>( ) {

              @Override
              public void handle(final Object target) {
                inject((JsonObject)value, target);
              }
            });
            return;
          }
        } else {
          // 3. flag setter -> xyz()
          if (getter != null) {
            if ("true".equals(String.valueOf(value))) {
              getter.invoke(target);
              return;
            } else if ("false".equals(String.valueOf(value))) {
              return;
            }
          }
        }
      } catch (final Throwable t) {
        throw RExc.toRuntime(fromInvocation(t));
      }
    });

    return target;
  }

  private static Method setter(final String key, final Class<?> type) {
    final String standadard = "set" + Character.toUpperCase(key.charAt(0)) + key.substring(1);
    final List<Method> optimized = new ArrayList<>();
    for (final Method method : type.getMethods()) {
      if (standadard.equals(method.getName()) && method.getParameterCount() == 1) {
        return method; // with priority
      } else if (key.equals(method.getName()) && method.getParameterCount() == 1 && method.getParameterTypes()[0] != Handler.class) {
        optimized.add(method);
      }
    }
    if (optimized.isEmpty()) {
      return null;
    } else if (optimized.size() == 1) {
      return optimized.get(0);
    } else {
      throw new IllegalArgumentException("Ambiguous optimized setters for " + key + ": " + optimized + "!");
    }
  }

  private static Method timeSetter(final String key, final Class<?> type) {
    final String standadard = "set" + Character.toUpperCase(key.charAt(0)) + key.substring(1);
    final List<Method> optimized = new ArrayList<>();
    for (final Method method : type.getMethods()) {
      if (standadard.equals(method.getName()) && method.getParameterCount() == 2 && method.getParameterTypes()[1] == TimeUnit.class) {
        return method; // with priority
      } else if (key.equals(method.getName()) && method.getParameterCount() == 2 && method.getParameterTypes()[1] == TimeUnit.class) {
        optimized.add(method);
      }
    }
    if (optimized.isEmpty()) {
      return null;
    } else if (optimized.size() == 1) {
      return optimized.get(0);
    } else {
      throw new IllegalArgumentException("Ambiguous optimized time setters for " + key + ": " + optimized + "!");
    }
  }

  private static Method getter(final String key, final Class<?> type) {
    final String standadard = "get" + Character.toUpperCase(key.charAt(0)) + key.substring(1);
    Method optimized = null;
    for (final Method method : type.getMethods()) {
      if (standadard.equals(method.getName()) && method.getParameterCount() == 0) {
        return method; // with priority
      } else if (key.equals(method.getName()) && method.getParameterCount() == 0) {
        optimized = method;
      }
    }
    return optimized;
  }

  private static Method handler(final String key, final Class<?> type) {
    for (final Method method : type.getMethods()) {
      if (key.equals(method.getName()) && method.getParameterCount() == 1 && method.getParameterTypes()[0] == Handler.class) {
        return method;
      }
    }
    return null;
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
