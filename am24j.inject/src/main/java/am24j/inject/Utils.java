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

import javax.inject.Provider;
import javax.inject.Qualifier;

/**
 * Utilities
 *
 * @author avgustinmm
 */
class Utils {

  @SuppressWarnings("unchecked")
  public static Class<?> clazz(final Type type) {
    if (type instanceof Class) {
      return (Class<Object>)type;
    } else if (type instanceof ParameterizedType) {
      return (Class<Object>)((ParameterizedType)type).getRawType();
    } else {
      throw InjectException.of("Can't reoslve class of " + type.getTypeName() + ")");
    }
  }

  public static List<Annotation> qualifiers(final Annotation[] annotations) {
    final List<Annotation> qualifiers = new ArrayList<>();
    for (final Annotation annptation : annotations) {
      if (annptation.annotationType().getAnnotation(Qualifier.class) != null) {
        qualifiers.add(annptation);
      }
    }
    return qualifiers;
  }

  public static Type providerType(final Class<?> clazz) {
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

  public static List<Type> types(final Class<?> clazz) {
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
}
