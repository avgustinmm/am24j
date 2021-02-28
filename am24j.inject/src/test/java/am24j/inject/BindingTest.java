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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Singleton;

import org.junit.Assert;
import org.junit.Test;

import am24j.inject.Injector.Key;

/**
 * Test bindings
 *
 * @author avgustinmm
 */
public class BindingTest {

  @Test
  public void getInstanceObj() {
    final Injector injector = Injector.newInstance();
    injector.bind(Key.of(Object.class), Object.class);
    final Object obj = injector.getInstance(Key.of(Object.class));
    Assert.assertTrue("Expected type", obj instanceof Object);
  }

  @Test
  public void getInstanceEmptyConstr() {
    final Injector injector = Injector.newInstance();
    injector.bind(Key.of(ArrayList.class), ArrayList.class);
    final ArrayList<?> obj = injector.getInstance(Key.of(ArrayList.class));
    Assert.assertTrue("Expected type", obj instanceof ArrayList);
  }

  @Test
  public void bindInstance() {
    final Injector injector = Injector.newInstance();
    final Object obj = new Object();
    injector.bind(Key.of(Object.class), obj);
    Assert.assertTrue("Equals first", obj == injector.getInstance(Key.of(Object.class)));
    Assert.assertTrue("Equals secind", obj == injector.getInstance(Key.of(Object.class)));

    // rebind
    final Object obj2 = new Object();
    injector.bind(Key.of(Object.class), obj2);
    Assert.assertTrue("Equals first (rebind)", obj2 == injector.getInstance(Key.of(Object.class)));
    Assert.assertTrue("Equals secind (rebind)", obj2 == injector.getInstance(Key.of(Object.class)));
  }

  @Test
  public void bindClass() {
    final Injector injector = Injector.newInstance();
    injector.bind(Key.of(Object.class), ArrayList.class);
    final List<Object> l = injector.getInstance(Key.of(Object.class));
    Assert.assertTrue("ArrayList first", l instanceof ArrayList);
    final List<Object> l2 = injector.getInstance(Key.of(Object.class));
    Assert.assertTrue("ArrayList second", l2 instanceof ArrayList);
    Assert.assertFalse("Not equal", l == l2);

    // rebind
    injector.bind(Key.of(Object.class), HashSet.class);
    final Set<Object> s = injector.getInstance(Key.of(Object.class));
    Assert.assertTrue("ArrayList first (rebind)", s instanceof HashSet);
    final Set<Object> s2 = injector.getInstance(Key.of(Object.class));
    Assert.assertTrue("ArrayList second (rebind)", s2 instanceof HashSet);
    Assert.assertFalse("Not equal", s == s2);
  }

  @Test
  public void bindProvider() {
    final Injector injector = Injector.newInstance();
    final Object obj = new Object();
    injector.bind(Key.of(Object.class), () -> obj);
    Assert.assertTrue("Equals first", obj == injector.getInstance(Key.of(Object.class)));
    Assert.assertTrue("Equals secind", obj == injector.getInstance(Key.of(Object.class)));

    // rebind
    final Object obj2 = new Object();
    injector.bind(Key.of(Object.class), () -> obj2);
    Assert.assertTrue("Equals first (rebind)", obj2 == injector.getInstance(Key.of(Object.class)));
    Assert.assertTrue("Equals secind (rebind)", obj2 == injector.getInstance(Key.of(Object.class)));

    // rebind, diff obj
    injector.bind(Key.of(Object.class), () -> new Object());
    final Object obj3 = injector.getInstance(Key.of(Object.class));
    final Object obj4 = injector.getInstance(Key.of(Object.class));
    Assert.assertFalse("Not Equals first (rebind)", obj2 == obj3);
    Assert.assertFalse("Not Equals second (rebind)", obj2 == obj4);
    Assert.assertFalse("Not Equals third (rebind)", obj3 == obj4);
  }

  @Test
  public void multiObject() {
    final Injector injector = Injector.newInstance();
    injector.bind(Key.of(Object.class), Object.class);
    final Object o = injector.getInstance(Key.of(Object.class));
    final Object o2 = injector.getInstance(Key.of(Object.class));
    Assert.assertFalse("Not equal", o == o2);
  }

  @Singleton
  public static class SingletonObj {}
  @Test
  public void singletonObject() {
    final Injector injector = Injector.newInstance();
    injector.bind(Key.of(Object.class), SingletonObj.class);
    final SingletonObj o = injector.getInstance(Key.of(Object.class));
    final SingletonObj o2 = injector.getInstance(Key.of(Object.class));
    Assert.assertTrue("Not equal", o == o2);
    final SingletonObj o3 = injector.getInstance(Key.of(Object.class));
    Assert.assertTrue("Not equal", o == o3);
  }
}
