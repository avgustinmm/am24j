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

import java.lang.reflect.Type;
import java.util.List;
import java.util.Set;

import javax.inject.Named;

/**
 * @author avgustinmm
 */
@Named("x")
class TestUtils {

  static Named namedX() {
    return TestUtils.class.getAnnotation(Named.class);
  }

  static Type listStr(final List<String> v) {
    try {
      return TestUtils.class.getDeclaredMethod("listStr", List.class).getGenericParameterTypes()[0];
    } catch (final NoSuchMethodException | SecurityException e) {
      throw InjectException.of(e);
    }
  }

  static Type listObj(final List<Object> v) {
    try {
      return TestUtils.class.getDeclaredMethod("listObj", List.class).getGenericParameterTypes()[0];
    } catch (final NoSuchMethodException | SecurityException e) {
      throw InjectException.of(e);
    }
  }

  static Type setStr(final Set<String> v) {
    try {
      return TestUtils.class.getDeclaredMethod("setStr", Set.class).getGenericParameterTypes()[0];
    } catch (final NoSuchMethodException | SecurityException e) {
      throw InjectException.of(e);
    }
  }
}
