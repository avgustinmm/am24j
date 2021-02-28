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

/**
 * Provides converting to a {@link Throwable} to a {@link RuntimeException} in order to be used where
 * there is need to be thrown a runtime exception - e.g. lambdas or completion stage's stages.
 *
 * @author avgustinmm
 */
public class RExc {

  private RExc() {}

  public static RuntimeException toRuntime(final Throwable t) {
    return t instanceof RuntimeException ? (RuntimeException)t : new RuntimeException(t);
  }
}
