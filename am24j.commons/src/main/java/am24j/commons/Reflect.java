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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Comparator;
import java.util.stream.Stream;

/**
 * @author avgustinmm
 */
public class Reflect {

  public static final Comparator<Method> METHOD_COMPARATOR = new Comparator<Method>() {

    @Override
    public int compare(final Method m1, final Method m2) {
      return Reflect.methodSig(m1).compareTo(Reflect.methodSig(m2));
    }
  };

  private Reflect() {}
  
  @SuppressWarnings("unchecked")
  public static <T> T call(final Object obj, final Method method, final Object... args) {
    if (!method.canAccess(obj)) {
      method.setAccessible(true);
    }
    try {
      return (T)method.invoke(obj, args);
    } catch (final InvocationTargetException e) {
      final Throwable cause = e.getCause() == null ? e : e.getCause();
      throw RE.toRuntime(cause);
    } catch (final Exception e) {
      throw RE.toRuntime(e);
    }
  }
  
  public static Method method(final Class<?> clazz, final String name, final Class<?>... parameterTypes) {
    try {
      return clazz.getDeclaredMethod(name, parameterTypes);
    } catch (final Exception e) {
      throw RE.toRuntime(e);
    }
  }
  
  public static String methodSig(final Method method) {
    final StringBuilder sb = new StringBuilder(method.getName());
    sb.append('(');
    final Class<?>[] paramTypes = method.getParameterTypes();
    if (paramTypes.length != 0) {
      sb.append(paramTypes[0].getName());
      for (int i = 1; i < paramTypes.length; i++) {
        sb.append(',').append(paramTypes[i].getName());
      }
    }
    sb.append(')'); // no return type - it might differ when extended
    return sb.toString();
  }
  
  public static Stream<Method> findByName(final Class<?> clazz, final String methodName) {
    return Arrays.stream(clazz.getDeclaredMethods()).filter(method -> methodName.equals(method.getName()));
  }

  public static Class<?> toClass(final Type type) {
    if (type instanceof Class) {
      return (Class<?>)type;
    } else if (type instanceof ParameterizedType) {
      final Type rawType = ((ParameterizedType)type).getRawType();
      if (rawType instanceof Class) {
        return (Class<?>)rawType;
      }
    }
    throw new UnsupportedOperationException("Can't resolve type " + type.getTypeName() + " to class!");
  }
}
