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
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Qualifier;

/**
 * Utilities
 *
 * @author avgustinmm
 */
class Utils {

  public static Named named(final String name) {
    return named0(name == null ? "" : name);
  }
  private static Named named0(final String name) {
    return new Named() {

      @Override
      public Class<? extends Annotation> annotationType() {
        return Named.class;
      }

      @Override
      public String value() {
        return name;
      }

      @Override
      public int hashCode() {
        return (127 * "value".hashCode()) ^ Objects.hashCode(value());
      }

      @Override
      public boolean equals(final Object o) {
        if (o instanceof Named) {
          final Named named = (Named)o;
          return Objects.equals(named.value(), value());
        }
        return false;
      }

      @Override
      public String toString() {
        return "@" + Utils.class.getName() + "$Named(value=\"" + value() + "\")";
      }
    };
  }

  public static Type parameterizedType(final Type raw, final Type[] actual, final Type owner) {
    return new ParameterizedType() {

      @Override
      public Type getRawType() {
        return raw;
      }

      @Override
      public Type getOwnerType() {
        return owner;
      }

      @Override
      public Type[] getActualTypeArguments() {
        return actual;
      }
    };
  }

  @SuppressWarnings("unchecked")
  static Class<?> clazz(final Type type) {
    if (type instanceof Class) {
      return (Class<Object>)type;
    } else if (type instanceof ParameterizedType) {
      return (Class<Object>)((ParameterizedType)type).getRawType();
    } else {
      throw InjectException.of("Can't reoslve class of " + type.getTypeName() + ")");
    }
  }

  static List<Annotation> qualifiers(final Annotation[] annotations) {
    final List<Annotation> qualifiers = new ArrayList<>();
    for (final Annotation annptation : annotations) {
      if (annptation.annotationType().getAnnotation(Qualifier.class) != null) {
        qualifiers.add(annptation);
      }
    }
    return qualifiers;
  }

  static Type providerType(final Class<?> clazz) {
    final Type[] interfaces = clazz.getGenericInterfaces();
    if (interfaces != null) {
      for (final Type type : interfaces) {
        if (type instanceof ParameterizedType) {
          final ParameterizedType paramType = (ParameterizedType)type;
          if (paramType.getRawType() == Provider.class) {
            return paramType.getActualTypeArguments()[0]; // Provider<X>
          }
        }
      }
    }
    return null;
  }

  static List<Type> types(final Class<?> clazz) {
    final List<Type> types = new ArrayList<>();
    types.add(clazz);
    final Type[] interfaces = clazz.getGenericInterfaces();
    if (interfaces != null) {
      for (final Type type : interfaces) {
        types.add(type);
      }
    }
    return types;
  }

  static boolean isOptional(final Type type) {
    return clazz(type) == Optional.class;
  }

  static Type optionalType(final Type type) {
    if (type instanceof ParameterizedType) {
      return ((ParameterizedType)type).getActualTypeArguments()[0];
    } else {
      return Object.class;
    }
  }
}
