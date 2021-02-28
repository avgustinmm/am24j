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
package am24j.inject;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.inject.Provider;

import am24j.inject.Injector.Key;
import am24j.inject.Injector.Point;
import am24j.inject.annotation.Provides;
import am24j.inject.spi.Interceptor;

/**
 * Provides implementation of some useful interceptors
 *
 * @author avgustinmm
 */
public class Interceptors {

  private Interceptors() {}

  public static Interceptor providesBinder() {
    return providesBinder(Provides.class);
  }

  public static Interceptor providesBinder(final Class<? extends Annotation> providesAnnotqation) {
    return new ProvidesBinder(providesAnnotqation);
  }

  public static AutoCloseableHandler autoCloseableHandler() {
    return new AutoCloseableHandler();
  }

  public static class ProvidesBinder implements Interceptor  {

    private final Class<? extends Annotation> providesAnnotqation;

    protected ProvidesBinder(final Class<? extends Annotation> providesAnnotqation) {
      this.providesAnnotqation = providesAnnotqation;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Object handle(final Key key, final Optional<Point> point, final Object obj, final Injector injector) {
      if (obj != null) {
        // handles custom Provides annotation
        final Class<?> clazz = obj.getClass();

        if (clazz.getAnnotation(providesAnnotqation) != null) {
          final Type providerType = Utils.providerType(clazz);
          if (providerType != null) {
            bindProvider(
              (Provider<Object>)obj,
              Collections.singletonList(providerType),
              Utils.qualifiers(clazz.getAnnotations()),
              injector);
          }  // else warning
        }

        for (final Field field : clazz.getDeclaredFields()) {
          if (field.getAnnotation(providesAnnotqation) != null) {
            bindProvider(
              provider(obj, field),
              Collections.singletonList(
                field.getType() == Provider.class ?
                  ((ParameterizedType)field.getGenericType()).getActualTypeArguments()[0] :
                  field.getGenericType()),
              Utils.qualifiers(field.getAnnotations()),
              injector);
          }
        }

        for (final Method method : clazz.getDeclaredMethods()) {
          if (method.getAnnotation(providesAnnotqation) != null) {
            bindProvider(
              provider(obj, method),
              Collections.singletonList(
                method.getReturnType() == Provider.class ?
                  ((ParameterizedType)method.getGenericReturnType()).getActualTypeArguments()[0] :
                  method.getGenericReturnType()),
              Utils.qualifiers(method.getAnnotations()),
              injector);
          }
        }
      }

      return obj;
    }

    @SuppressWarnings("unchecked")
    private static Provider<Object> provider(final Object obj, final Field field) {
      try { // if possible
        field.setAccessible(true);
      } catch (final Throwable t) {}
      try {
        final Object value = field.get(obj);
        if (Provider.class.isAssignableFrom(field.getType())) {
          return (Provider<Object>)value;
        } else {
          return () -> obj;
        }
      } catch (final IllegalAccessException e) {
        throw InjectException.of(e);
      }
    }

    @SuppressWarnings("unchecked")
    private static Provider<Object> provider(final Object obj, final Method method) {
      try { // if possible
        method.setAccessible(true);
      } catch (final Throwable t) {}
      if (Provider.class.isAssignableFrom(method.getReturnType())) {
        try {
          return (Provider<Object>)method.invoke(obj);
        } catch (final InvocationTargetException e) {
          throw InjectException.of(e.getCause() == null ? e : e.getCause());
        } catch (final IllegalAccessException e) {
          throw InjectException.of(e);
        }
      } else {
        return new Provider<Object>() {
          @Override
          public Object get() {
            try {
              return method.invoke(obj);
            } catch (final InvocationTargetException e) {
              throw InjectException.of(e.getCause() == null ? e : e.getCause());
            } catch (final IllegalAccessException e) {
              throw InjectException.of(e);
            }
          }
        };
      }
    }

    private static void bindProvider(
        final Provider<Object> provider,
        final List<Type> types, final List<Annotation> qualifiers,
        final Injector injector) {
      final List<Key> bindKeys = new ArrayList<>();
      for (final Type type : types) {
        if (qualifiers.isEmpty()) {
          bindKeys.add(Key.of(type));
        } else {
          qualifiers.forEach(qualifier -> bindKeys.add(Key.of(type, qualifier)));
        }
      }

      bindKeys.forEach(key -> injector.bind(key, provider));
    }
  }

  // handle auto closeable
  public static class AutoCloseableHandler implements AutoCloseable, Interceptor {

    private final List<AutoCloseable> closeHandlers = Collections.synchronizedList(new ArrayList<>());

    protected AutoCloseableHandler() {}

    @Override
    public Object handle(final Key key, final Optional<Point> point, final Object obj, final Injector injector) {
      if (obj != null) {
        if (obj instanceof AutoCloseable) {
          if (!closeHandlers.contains(obj)) {
            closeHandlers.add((AutoCloseable)obj);
          }
        }
      }

      return obj;
    }

    @Override
    public void close() throws Exception {
      Exception error = null;
      for (int i = closeHandlers.size(); i-- > 0;) {
        try {
          closeHandlers.get(i).close();;
        } catch (final Exception e) {
          if (error == null) {
            error = e;
          } else {
            error.addSuppressed(e);
          }
        }
      }
    }

    @Override
    public String toString() {
      return "AutoCloseHandler: " + closeHandlers;
    }
  }
}