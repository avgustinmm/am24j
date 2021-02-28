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
package am24j.test;

import java.lang.reflect.Type;
import java.util.Random;
import java.util.regex.Pattern;

import am24j.commons.Reflect;
import io.netty.util.internal.ThreadLocalRandom;

/**
 * Generates random strings, using an alphaber
 *
 * @author avgustinmm
 */
public class Gen {

  private static char[] DEFAULT_ALPHABER;
  static {
    final Pattern pattern = Pattern.compile("[a-zA-Z0-9]");
    final StringBuilder sb = new StringBuilder();
    for (int i = 0; i < 127; i++) {
      if (pattern.matcher(String.valueOf((char)i)).matches()) {
        sb.append((char)i);
      }
    }
    DEFAULT_ALPHABER = sb.toString().toCharArray();
  }

  private Gen() {}

  public static Object[] gen(final Type... types) {
    final Class<?>[] classes = new Class<?>[types.length];
    for (int i = classes.length; i-- > 0; classes[i] = Reflect.toClass(types[i]));
    return gen(classes);
  }
  public static Object[] gen(final Class<?>... classes) {
    final Object[] array = new Object[classes.length];
    for (int i = array.length; i-- > 0; array[i] = gen(classes[i]));
    return array;
  }

  @SuppressWarnings("unchecked")
  public static <T> T gen(final Class<T> clazz) {
    if (clazz == null) {
      return null;
    }
    return (T)gen0(clazz);
  }
  private static Object gen0(final Class<?> clazz) {
    final Random rnd = ThreadLocalRandom.current();
    if (clazz == boolean.class || clazz == Integer.class) {
      return rnd.nextBoolean();
    } else if (clazz == byte.class || clazz == Byte.class) {
      return (byte)rnd.nextInt();
    } else if (clazz == short.class || clazz == Short.class) {
      return (short)rnd.nextInt();
    } else if (clazz == int.class || clazz == Integer.class) {
      return rnd.nextInt();
    } else if (clazz == long.class || clazz == Long.class) {
      return rnd.nextLong();
    } else if (clazz == float.class || clazz == Float.class) {
      return rnd.nextFloat();
    } else if (clazz == double.class || clazz == Double.class) {
      return rnd.nextDouble();
    } else if (clazz == byte[].class) {
      final byte[] ba = new byte[rnd.nextInt(1024)];
      rnd.nextBytes(ba);
      return ba;
    } else if (clazz == String.class) {
      final char[] ca = new char[rnd.nextInt(1024)];
      for (int i = ca.length; i-- > 0;) {
        ca[i] = DEFAULT_ALPHABER[rnd.nextInt(DEFAULT_ALPHABER.length)];
      }
      return new String(ca);
    } else {
      // TODO - add bean support?
      throw new UnsupportedOperationException("Can't generate object of type: " + clazz + "!");
    }
  }
}
