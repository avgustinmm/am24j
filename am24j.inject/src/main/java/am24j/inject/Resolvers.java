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
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.inject.Provider;

import am24j.inject.Injector.BindListener;
import am24j.inject.Injector.Key;
import am24j.inject.Injector.Point;
import am24j.inject.spi.Resolver;

/**
 * Implements some resolvers
 *
 * @author avgustinmm
 */
public class Resolvers {

  private Resolvers() {}

  public static Resolver multi() {
    return multi(true);
  }

  // extended takes in account inheritance - i.e adds in collection all that extends the kty type
  public static Resolver multi(final boolean extended) {
    return new Multi(extended);
  }

  public static Resolver implementedBy() {
    return implementedBy(am24j.inject.annotation.ImplementedBy.class);
  }

  public static Resolver implementedBy(final Class<? extends Annotation> implementedByClass) {
    return new ImplementedBy(implementedByClass);
  }

  /**
   * Provides collection of objects retrieving them from all bound(!) prividers matching the key.
   */
  public static class Multi implements Resolver, BindListener {

    private final boolean extended;
    private final Map<Key, List<Provider<Object>>> candidates = new HashMap<>();
    private final Map<Key, Collection<Object>> collections = new HashMap<>();

    protected Multi(final boolean extended) {
      this.extended = extended;
    }

    @Override
    public Provider<Object> get(final Key key, final Optional<Point> point, final Injector injector) {
      if (Collection.class.isAssignableFrom(Utils.clazz(key.type()))) {
        if (key.type() instanceof Class) {
          // resolves collection types
          if (((Class<?>)key.type()).isAssignableFrom(ArrayList.class)) {
            return () -> new ArrayList<>();
          } else if (((Class<?>)key.type()).isAssignableFrom(HashSet.class)) {
            return () -> new HashSet<>();
          }
        } else {
          return () -> collections.computeIfAbsent(key, k -> {
            final Collection<Object> collection = injector.getInstance(Key.of(Utils.clazz(key.type())));
            candidates.forEach((candidateKey, providers) -> {
              if (match(candidateKey, key)) {
                providers.forEach(provider -> {
                  final Object obj = provider.get();
                  if (!collection.contains(obj)) {
                    collection.add(obj);
                  }
                });
              }
            });
            return collection;
          });
        }
      }

      throw NOT_FOUND;
    }

    @Override
    public void bound(final Key key, final Provider<Object> provider, final Injector injector) {
      final List<Provider<Object>> providers = candidates.computeIfAbsent(key, k -> new ArrayList<>());
      if (!providers.contains(provider)) {
        providers.add(provider);
      }
      collections.forEach((k, c) -> {
        if (match(key, k)) {
          final Object obj = provider.get();
          if (!c.contains(obj)) {
            c.add(obj);
          }
        }
      });
    }

    @Override
    public String toString() {
      return "Multi(" + hashCode() + ")";
    }

    protected boolean match(final Key key, final Key collectionKey) {
      // check qualifiers
      if (!qualifiersMAtch(key, collectionKey)) {
        return false;
      }

      if (collectionKey.type() instanceof ParameterizedType) { // parameterized collection
        final Type requiredType = ((ParameterizedType)collectionKey.type()).getActualTypeArguments().length == 1 ? ((ParameterizedType)collectionKey.type()).getActualTypeArguments()[0] : Object.class;
        final Type providerType = Utils.providerType(Utils.clazz(key.type()));
        if (providerType == null) {
          return extended ? isAssignableFrom(requiredType, key.type()) : requiredType == key.type();
        } else {
          return extended ? isAssignableFrom(requiredType, providerType) : requiredType == providerType;
        }
      } else {
        return false;
      }
    }

    private boolean qualifiersMAtch(final Key key, final Key collectionKey) {
      if (collectionKey.qualifer().isPresent()) {
        final Annotation qualifier = collectionKey.qualifer().get();
        // check has same qualifier
        if (!key.qualifer().filter(q -> q.equals(qualifier)).map(q -> true).orElse(false)) {
          // check if the class itself bears qualifier - implicit ..
          for (final Annotation q : Utils.qualifiers(Utils.clazz(key.type()).getAnnotations())) {
            if (q.equals(qualifier)) {
              return true;
            }
          }
          return false;
        }
      } else if (key.qualifer().isPresent()) { // check no qualifier
        return false;
      }
      return true;
    }

    private boolean isAssignableFrom(final Type requiredType, final Type type) {
      if (requiredType instanceof Class) {
        if (type instanceof Class) {
          return ((Class<?>)requiredType).isAssignableFrom(((Class<?>)type));
        }
      } else if (requiredType instanceof ParameterizedType) {
        if (type instanceof ParameterizedType) {
          final ParameterizedType requiredPType = (ParameterizedType)requiredType;
          final ParameterizedType pType = (ParameterizedType)type;
          if (isAssignableFrom(requiredPType.getRawType(), pType.getRawType())) {
            final Type[] reqActual = requiredPType.getActualTypeArguments();
            final Type[] typeActual = pType.getActualTypeArguments();
            if (reqActual.length == typeActual.length) {
              for (int i = reqActual.length; i-- > 0;) {
                if (!isAssignableFrom(reqActual[i], typeActual[i])) {
                  return false;
                }
              }
              return true;
            }
          }
        } else if (type instanceof Class) {
          final Type superType = ((Class<?>)type).getGenericSuperclass();
          if (isAssignableFrom(requiredType, superType)) {
            return true;
          }
          for (final Type superInterface : ((Class<?>)type).getGenericInterfaces()) {
            if (isAssignableFrom(requiredType, superInterface)) {
              return true;
            }
          }
        }
      }
      return false;
    }
  }

  public static class ImplementedBy implements Resolver {

    private final Class<? extends Annotation> implementedByClass;
    private final Method value;

    protected ImplementedBy(final Class<? extends Annotation> implementedByClass) {
      this.implementedByClass = implementedByClass;
      try {
        value = implementedByClass.getMethod("value");
        if (value.getReturnType() != Class.class) {
          throw InjectException.of("ImplementBy annotation class no-argument method \"value\" MUST have return type \"java.lang.Class\"!");
        }
        try { // if possible
          value.setAccessible(true);
        } catch (final Throwable t) {}
      } catch (final NoSuchMethodException e) {
        throw InjectException.of("ImplementBy annotation class MUST have no-argument method \"value\"!");
      }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Provider<Object> get(final Key key, final Optional<Point> point, final Injector injector) {
      final Class<?> clazz = Utils.clazz(key.type());
      if (clazz.isInterface()) {
        final Annotation implementedBy = clazz.getAnnotation(implementedByClass); // TODO defone own
        if (implementedBy == null) {
          throw NOT_FOUND;
        } else {
          return () -> {
            try {
              return injector.getInstance(Key.of((Class<Object>)value.invoke(implementedBy)));
            } catch (final Exception e) {
              throw InjectException.of(e);
            }
          };
        }
      }

      throw NOT_FOUND;
    }

    @Override
    public String toString() {
      return "Implement4edBy(" + hashCode() + ")";
    }
  }
}
