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

import java.util.Iterator;
import java.util.function.Function;

/**
 * Utilities for java util structures
 *
 * @author avgustinmm
 */
public class Utils {

  private Utils() {}

  public static <S, T> Iterator<T> map(final Iterator<S> i, final Function<S, T> maoFn) {
    return new Iterator<T>() {

      @Override
      public boolean hasNext() {
        return i.hasNext();
      }

      @Override
      public T next() {
        return maoFn.apply(i.next());
      }
    };
  }
}
