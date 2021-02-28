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

import java.util.Objects;

/**
 * Tuple containing two objects. Supports hash code and equals - so could be used as a key in hash based structures.
 *
 * @author avgustinmm
 */
public class Tuple2<F, S> {

  private final F t1;
  private final S t2;

  public Tuple2(final F t1, final S t2) {
    this.t1 = t1;
    this.t2 = t2;
  }

  public F t1() {
    return t1;
  }

  public S t2() {
    return t2;
  }

  @Override
  public String toString() {
    return "(" + t1 + ", " + t2 + ")";
  }

  @Override
  public int hashCode() {
    return Objects.hash(t1, t2);
  }

  @Override
  public boolean equals(final Object o) {
    if (o instanceof Tuple2) {
      final Tuple2<?, ?> ot2 = (Tuple2<?, ?>)o;
      return Objects.equals(t1, ot2.t1) && Objects.equals(t2, ot2.t2);
    } else {
      return false;
    }
  }
}
