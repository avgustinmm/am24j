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
package am24j.inject.spi;

import java.util.Optional;

import javax.inject.Provider;

import am24j.inject.InjectException;
import am24j.inject.Injector;
import am24j.inject.Injector.Key;
import am24j.inject.Injector.Point;

/**
 * Resolves object instance based on key and injection point
 *
 * @author avgustinmm
 */
public interface Resolver {

  public static final InjectException NOT_FOUND = InjectException.of("Not found!");

  public Provider<Object> get(final Key key, final Optional<Point> point, final Injector injector);
}
