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

import java.time.Duration;
import java.util.Base64;

/**
 * Types conversions
 *
 * @author avgustinmm
 */
public class Types {

  /**
   * Convert string to a type
   *
   * @param <T> required type
   *
   * @param str string
   * @param type class of the required type
   * @return converted value
   */
  @SuppressWarnings("unchecked")
  public static <T> T toType(final String str, final Class<T> type) {
    if (type == boolean.class || type == Boolean.class) {
      return (T)(Boolean)Boolean.parseBoolean(str);
    } else if (type == byte.class || type == Byte.class) {
      return (T)(Byte)Byte.parseByte(str);
    } else if (type == short.class || type == Short.class) {
      return (T)(Short)Short.parseShort(str);
    } else if (type == char.class || type == Character.class) {
      return (T)(Character)str.charAt(0);
    } else if (type == int.class || type == Integer.class) {
      return (T)(Integer)Integer.parseInt(str);
    } else if (type == float.class || type == Float.class) {
      return (T)(Float)Float.parseFloat(str);
    } else if (type == long.class || type == Long.class) {
      return (T)(Long)Long.parseLong(str);
    } else if (type == double.class || type == Double.class) {
      return (T)(Double)Double.parseDouble(str);
    } else if (type == byte[].class) {
      return (T)Base64.getDecoder().decode(str);
    } else if (type == Duration.class) {
      return (T)Duration.parse(str);
    } else if (type == String.class) {
      return (T)str;
    }
    throw new UnsupportedOperationException("Can't convert to " + type + "!");
  }

  /**
   * Converts a json simple value to require type
   *
   * @param <T> required type
   *
   * @param jsonValue json value
   * @param type class of the required type
   * @return converted value
   */
  @SuppressWarnings("unchecked")
  public static <T> T toType(final Object jsonValue, final Class<T> type) {
    if (jsonValue == null) {
      return null;
    } else if (type.isAssignableFrom(jsonValue.getClass())) {
      return (T)jsonValue;
    }

    final String str = String.valueOf(jsonValue);
    return toType(str, type);
  }
}
