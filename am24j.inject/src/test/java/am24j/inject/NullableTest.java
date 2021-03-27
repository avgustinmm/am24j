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

import java.util.Optional;

import javax.inject.Inject;

import org.junit.Assert;
import org.junit.Test;

import am24j.inject.Injector.Key;

public class NullableTest {

  @Test
  public void nullableWithNull() {
    Assert.assertNotNull(Injector.newInstance().getInstance(Key.of(Null.class)));
  }

  @Test
  public void nullableWithNonNull() {
    Assert.assertNotNull(Injector.newInstance().bind(Key.of(Arg.class), new Arg(" ")).getInstance(Key.of(Null.class)));
  }

  @Test(expected = InjectException.class)
  public void nonNullableWithNull() {
    Assert.assertNotNull(Injector.newInstance().getInstance(Key.of(NonNull.class)));
  }

  @Test
  public void nonNullableWithNonNull() {
    Assert.assertNotNull(Injector.newInstance().bind(Key.of(Arg.class), new Arg(" ")).getInstance(Key.of(NonNull.class)));
  }

  public static class Null {

    @Inject
    public Null(final Optional<Arg> arg) {}
  }

  public static class NonNull {

    @Inject
    public NonNull(final Arg arg) {}
  }

  public static class Arg {

    public Arg(final String str) {}
  }
}
