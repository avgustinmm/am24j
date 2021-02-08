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
package am24j.bean;

import java.beans.Transient;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Contains bean structure 
 * 
 * @author avgustinmm
 */
public class Bean<T>  {
  
  private final Method[] builder; // [2] -> 0 creates builder, 1 builds obkect
  private final Constructor<T> constructor;
  private final Property[] props;
  
  private Bean(final Type type) {
    final Class<T> clazz = clazz(type);
    try {
      builder = builder(clazz);
      constructor = builder == null ? clazz.getConstructor() : null;
      
      final List<Property> props = new ArrayList<>();
      final Class<?> setter = builder == null ? clazz : builder[0].getReturnType();
      for (final Method setterCandidate : setter.getMethods()) {
        if (setterCandidate.getParameterTypes().length == 1) {
          if (setterCandidate.getAnnotation(Transient.class) == null) {
            if (void.class.equals(setterCandidate.getReturnType()) || 
                setterCandidate.getReturnType().isAssignableFrom(setter)) {
              final Method getter = getter(setterCandidate, clazz);
              if (getter != null) {
                props.add(new Property(setterCandidate, getter));
              }
            }
          }
        }
      }
      Collections.sort(props);
      this.props = props.toArray(new Property[props.size()]);
    } catch (final Exception e) {
      throw e instanceof RuntimeException ? (RuntimeException)e : new RuntimeException(e);
    }
  }
  
  public static <T> Bean<T> forClass(final Class<T> clazz) {
    return forType(clazz);
  }
  
  private static final Map<Type, Bean<?>> BEAN_STRUCTS = new ConcurrentHashMap<>();
  @SuppressWarnings("unchecked")
  public static <T> Bean<T> forType(final Type type) {
    return (Bean<T>)BEAN_STRUCTS.computeIfAbsent(type, c -> new Bean<>(c));
  }
  
  public Property[] properties() {
    return props;
  }
  
  public Object[] values(final T obj) {
    try {
      final Object[] values = new Object[props.length];
      for (int i = 0; i < props.length; i++) {
        values[i] = props[i].get(obj);
      }
      return values;
    } catch (final Throwable t) {
      throw t instanceof RuntimeException ? (RuntimeException)t : new RuntimeException(t);
    }
  }
  
  @SuppressWarnings("unchecked")
  public T build(final Object[] values) {
    try {
      if (builder == null) {
        final T obj = constructor.newInstance();
        for (int i = 0; i < props.length; i++) {
          props[i].set(values[i], obj);
        }
        return obj;
      } else {
        final Object builder = this.builder[0].invoke(null);
        for (int i = 0; i < props.length; i++) {
          props[i].set(values[i], builder);
        }
        return (T)this.builder[1].invoke(builder);
      }
    } catch (final Throwable t) {
      throw t instanceof RuntimeException ? (RuntimeException)t : new RuntimeException(t);
    }
  }
  
  private static Method[] builder(final Class<?> clazz) throws NoSuchMethodException, SecurityException {
    final Method[] builder = builder("builder", clazz);
    if (builder != null) {
      return builder;
    }
    return builder("newBuilder", clazz);
  }

  private static Method[] builder(final String name, final Class<?> clazz) {
    try {
      final Method builder = clazz.getMethod(name);
      if (Modifier.isStatic(builder.getModifiers())) {
        final Method build = builder.getReturnType().getMethod("build");
        if (clazz.isAssignableFrom(build.getReturnType())) {
          return new Method[] {builder, build};
        }
      }
    } catch (final NoSuchMethodException e) {}
    return null;
  }
  
  private static Method getter(final Method setter, final Class<?> clazz) throws NoSuchMethodException, SecurityException {
    final String setterName = setter.getName();
    final String getterName;
    if (setterName.length() > 3 && setterName.startsWith("set") && Character.isUpperCase(setterName.charAt(3))) {
      // setXXX(<type>) / <? extends <type>> getXXX() pattern
      getterName = "get" + setterName.substring(3);
    } else {
      // <prop>(<type>), <? extends <type>> <prop>() pattern
      getterName = setterName;
    }
    try {
      final Method getterCandidate = clazz.getMethod(getterName);
      if (getterCandidate.getAnnotation(Transient.class) != null) {
        return null;
      }
      return Optional.class.equals(getterCandidate.getReturnType()) || setter.getParameterTypes()[0].isAssignableFrom(getterCandidate.getReturnType()) ? getterCandidate : null;
    } catch (final NoSuchMethodException e) {}
    return null;
  }
  
  @SuppressWarnings("unchecked")
  private static <T> Class<T> clazz(final Type type) {
    if (type instanceof Class) {
      return (Class<T>)type;
    } else if (type instanceof ParameterizedType) {
      return (Class<T>)((ParameterizedType)type).getRawType();
    } else {
      throw new IllegalArgumentException("Cab't resoove class from type " + type + "!");
    }
  }
  
  public static class Property implements Comparable<Property> {

    private final String name;
    private final Method setter;
    private final Method getter;
    
    private Property(final Method setter, final Method getter) {
      name = setter.getName().equals(getter.getName()) ? setter.getName() : Character.toLowerCase(setter.getName().charAt(3)) + setter.getName().substring(4);
      this.getter = getter;
      this.setter = setter;
    }
    
    @Override
    public int compareTo(final Property p) {
      return name.compareTo(p.name);
    }
    
    public String name() {
      return name;
    }
    
    public Type type() {
      return setter.getGenericParameterTypes()[0];
    }
    
    public boolean nullable() {
      return !clazz(type()).isPrimitive();
    }
    
    private void set(final Object value, final Object to) throws Throwable {
      try {
        setter.invoke(to, value);
      } catch (final InvocationTargetException e) {
        throw e.getCause() == null ? e : e.getCause();
      }
    }
    
    private Object get(final Object from) throws Throwable {
      try {
        final Object value = getter.invoke(from);
        return value instanceof Optional ? ((Optional<?>)value).orElse(null) : value;
      } catch (final InvocationTargetException e) {
        throw e.getCause() == null ? e : e.getCause();
      }
    }
  }
}