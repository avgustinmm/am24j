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

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Qualifier;
import javax.inject.Singleton;

import org.junit.Assert;
import org.junit.Test;

import am24j.inject.Injector.Key;
import am24j.inject.annotation.Provides;

/**
 * @author avgustinmm
 */
public class MultiTest {

  @Test
  public void classOnly() {
    final Injector injector = Injector.newInstance().add(Resolvers.multi(true));
    injector.bind(Key.of(String.class), "x");
    injector.bind(Key.of(String.class), "y");
    injector.bind(Key.of(String.class), "z");
    injector.bind(Key.of(String.class, TestUtils.namedX()), "t"); // not collected

    final List<String> list = injector.getInstance(Key.of(TestUtils.listStr(null)));
    Assert.assertEquals("Size", 3, list.size());
    Collections.sort(list);
    Assert.assertEquals("Element 1", "x", list.get(0));
    Assert.assertEquals("Element 2", "y", list.get(1));
    Assert.assertEquals("Element 3", "z", list.get(2));

    Assert.assertEquals("Type", ArrayList.class, list.getClass());
  }

  @Test
  public void qualified() {
    final Injector injector = Injector.newInstance().add(Resolvers.multi(true));
    injector.bind(Key.of(String.class, TestUtils.namedX()), "x");
    injector.bind(Key.of(String.class, TestUtils.namedX()), "y");
    injector.bind(Key.of(String.class, TestUtils.namedX()), "z");
    injector.bind(Key.of(String.class), "t"); // not collected

    final List<String> list = injector.getInstance(Key.of(TestUtils.listStr(null), TestUtils.namedX()));
    Assert.assertEquals("Size", 3, list.size());
    Collections.sort(list);
    Assert.assertEquals("Element 1", "x", list.get(0));
    Assert.assertEquals("Element 2", "y", list.get(1));
    Assert.assertEquals("Element 3", "z", list.get(2));

    Assert.assertEquals("Type", ArrayList.class, list.getClass());
  }

  @Test
  public void set() {
    final Injector injector = Injector.newInstance().add(Resolvers.multi(true));
    injector.bind(Key.of(String.class), "x");
    injector.bind(Key.of(String.class), "y");
    injector.bind(Key.of(String.class), "z");
    injector.bind(Key.of(String.class, TestUtils.namedX()), "t"); // not collected

    final Set<String> set = injector.getInstance(Key.of(TestUtils.setStr(null)));
    Assert.assertEquals("Size", 3, set.size());
    final List<String> list = new ArrayList<>(set);
    Collections.sort(list);
    Assert.assertEquals("Element 1", "x", list.get(0));
    Assert.assertEquals("Element 2", "y", list.get(1));
    Assert.assertEquals("Element 3", "z", list.get(2));

    Assert.assertEquals("Type", HashSet.class, set.getClass());
  }

  @Test
  public void extendedExplicit() {
    final Injector injector = Injector.newInstance().add(Resolvers.multi(true));
    final Object obj = new Object();
    injector.bind(Key.of(Object.class), obj);
    injector.bind(Key.of(String.class), "x");
    injector.bind(Key.of(String.class), "y");
    injector.bind(Key.of(String.class), "z");

    final List<Object> list = injector.getInstance(Key.of(TestUtils.listObj(null)));
    Assert.assertEquals("Size", 4, list.size());
    Assert.assertTrue("Element 1", list.contains("x"));
    Assert.assertTrue("Element 2", list.contains("y"));
    Assert.assertTrue("Element 3", list.contains("z"));
    Assert.assertTrue("Obj", list.contains(obj));

    Assert.assertEquals("Type", ArrayList.class, list.getClass());
  }

  @Test
  public void strictExplicit() {
    final Injector injector = Injector.newInstance().add(Resolvers.multi(false));
    final Object obj = new Object();
    injector.bind(Key.of(Object.class), obj);
    injector.bind(Key.of(String.class), "x");
    injector.bind(Key.of(String.class), "y");
    injector.bind(Key.of(String.class), "z");

    final List<Object> list = injector.getInstance(Key.of(TestUtils.listObj(null)));
    Assert.assertEquals("Size", 1, list.size());
    Assert.assertTrue("Obj", list.contains(obj));

    Assert.assertEquals("Type", ArrayList.class, list.getClass());
  }

  @Test
  public void extendedImplicit() {
    final Injector injector = Injector.newInstance().add(Resolvers.multi(true));
    injector.getInstance(Key.of(Object.class));
    injector.bind(Key.of(String.class), "x");
    injector.bind(Key.of(String.class), "y");
    injector.bind(Key.of(String.class), "z");

    final List<Object> list = injector.getInstance(Key.of(TestUtils.listObj(null)));
    Assert.assertEquals("Size", 4, list.size());
    Assert.assertTrue("Element 1", list.contains("x"));
    Assert.assertTrue("Element 2", list.contains("y"));
    Assert.assertTrue("Element 3", list.contains("z"));
    boolean has = false;
    for (final Object o : list) {
      if (o.getClass() == Object.class) {
        has = true;
      }
    }
    Assert.assertTrue("Obj", has);

    Assert.assertEquals("Type", ArrayList.class, list.getClass());
  }

  @Test
  public void strictImplicit() {
    final Injector injector = Injector.newInstance().add(Resolvers.multi(false));
    injector.getInstance(Key.of(Object.class));
    injector.bind(Key.of(String.class), "x");
    injector.bind(Key.of(String.class), "y");
    injector.bind(Key.of(String.class), "z");

    final List<Object> list = injector.getInstance(Key.of(TestUtils.listObj(null)));
    Assert.assertEquals("Size", 1, list.size());
    boolean has = false;
    for (final Object o : list) {
      if (o.getClass() == Object.class) {
        has = true;
      }
    }
    Assert.assertTrue("Obj", has);

    Assert.assertEquals("Type", ArrayList.class, list.getClass());
  }

  @Retention(RetentionPolicy.RUNTIME)
  @Qualifier
  public static @interface Collect {}

  @Collect @Singleton
  public static class CollectComp1 implements AutoCloseable {
    @Override
    public void close() {}
    @Override
    public String toString() {
      return "CollectComp1 (" + hashCode() + ")";
    }
  }
  @Collect @Singleton
  public static class CollectComp2 implements AutoCloseable {
    @Override
    public void close() {}
    @Override
    public String toString() {
      return "CollectComp2 (" + hashCode() + ")";
    }
  }
  @Collect
  public static class CollectComp3 implements Provider<AutoCloseable> {
    private final AutoCloseable ac = () -> {};
    @Override
    public AutoCloseable get() {
      return ac;
    }
  }
  @Collect
  public static class CollectComp4 {
    private final AutoCloseable ac1 = () -> {};
    private final AutoCloseable ac2 = () -> {};
    @Provides @Collect
    public AutoCloseable get() {
      return ac1;
    }
    @Provides @Collect
    public Provider<AutoCloseable> getP() {
      return () -> ac2;
    }
  }

  public static class Collector {

    private final List<AutoCloseable> list;
    @Inject
    public Collector(@Collect final List<AutoCloseable> list) {
      this.list = list;
    }
  }

  @Test
  public void complex() {
    final Injector injector = Injector.newInstance().add(Resolvers.multi(true)).add(Interceptors.providesBinder());
    injector.getInstance(Key.of(CollectComp1.class));
    injector.getInstance(Key.of(CollectComp2.class));
    injector.getInstance(Key.of(CollectComp3.class));
    injector.getInstance(Key.of(CollectComp4.class));
    final Collector coll1 = injector.getInstance(Key.of(Collector.class));
    final Collector coll2 = injector.getInstance(Key.of(Collector.class));
    Assert.assertFalse("Different", coll1 == coll2);
    Assert.assertEquals("Lists", 5, coll2.list.size());
    Assert.assertEquals("Lists", coll1.list, coll2.list);
  }
}
