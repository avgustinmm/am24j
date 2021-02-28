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

import java.lang.reflect.Field;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.junit.Assert;
import org.junit.Test;

import am24j.inject.Injector.Key;
import am24j.inject.Interceptors.AutoCloseableHandler;
import am24j.inject.annotation.Provides;

/**
 * Aoto closables tests
 *
 * @author avgustinmm
 */
public class AutoCloseableTest {

  @Singleton
  public static class Comp1 implements AutoCloseable {
    @Override
    public void close() {}
    @Override
    public String toString() {
      return "Comp1 (" + hashCode() + ")";
    }
  }
  @Singleton
  public static class Comp2 implements AutoCloseable {
    @Override
    public void close() {}
    @Override
    public String toString() {
      return "Comp2 (" + hashCode() + ")";
    }
  }
  @Singleton
  public static class Comp3 implements Provider<AutoCloseable> {
    private final AutoCloseable ac = () -> {};
    @Override
    public AutoCloseable get() {
      return ac;
    }
  }
  @Singleton
  public static class Comp4 {
    private final AutoCloseable ac1 = () -> {};
    private final AutoCloseable ac2 = () -> {};
    @Provides
    public AutoCloseable get() {
      return ac1;
    }
    @Provides @Named("x")
    public Provider<AutoCloseable> getP() {
      return () -> ac2;
    }
  }

  public static class Automatic implements AutoCloseable {
    @Override
    public void close() {}
  }

  public static class User {

    @Inject
    User(final Automatic a) {}
  }


  @Test
  public void test() {
    final AutoCloseableHandler ach = Interceptors.autoCloseableHandler();
    final Injector injector = Injector.newInstance().add(Resolvers.multi(true)).add(Interceptors.providesBinder()).add(ach);

    injector.getInstance(Key.of(Comp1.class));
    Assert.assertEquals("One added!", 1, size(ach));

    injector.getInstance(Key.of(Comp2.class));
    Assert.assertEquals("Second added!", 2, size(ach));

    final Comp3 comp3 = injector.getInstance(Key.of(Comp3.class));
    Assert.assertEquals("Provider don't add!", 2, size(ach));
    injector.<Comp3>getInstance(Key.of(Comp3.class)).get();
    Assert.assertEquals("Direct get - don't add", 2, size(ach));
    injector.bind(Key.of(AutoCloseable.class), new Comp3());
    final AutoCloseable ac = injector.getInstance(Key.of(AutoCloseable.class));
    Assert.assertEquals("Via provider explicit get - add", 3, size(ach));
    Assert.assertEquals("Provider used", comp3.ac, ac); // same comp - singleton

    injector.<Comp4>getInstance(Key.of(Comp4.class)).get();
    Assert.assertEquals("Direct get - don't add", 3, size(ach));
    injector.getInstance(Key.of(AutoCloseable.class));
    Assert.assertEquals("Via provider (provides) explicit get - dd", 4, size(ach));
    injector.getInstance(Key.of(Comp4.class));
    injector.<Comp4>getInstance(Key.of(Comp4.class)).getP().get();
    Assert.assertEquals("Direct get - don't add", 4, size(ach));
    injector.getInstance(Key.of(AutoCloseable.class, TestUtils.namedX()));
    Assert.assertEquals("Via provider (provides) explicit get - add", 5, size(ach));

    injector.getInstance(Key.of(Automatic.class));
    Assert.assertEquals("Automaiic constr -add ", 6, size(ach));

    injector.getInstance(Key.of(Automatic.class));
    Assert.assertEquals("Automaiic indirect -add ", 7, size(ach));
  }

  @SuppressWarnings("unchecked")
  private static int size(final AutoCloseableHandler ach) {
    try {
      final Field field = AutoCloseableHandler.class.getDeclaredField("closeHandlers");
      field.setAccessible(true);
      return ((List<AutoCloseable>)field.get(ach)).size();
    } catch (final Exception e) {
      throw InjectException.of(e);
    }
  }
}
