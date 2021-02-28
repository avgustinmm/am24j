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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import javax.inject.Named;
import javax.inject.Provider;

import org.junit.Assert;
import org.junit.Test;

import am24j.inject.Injector.BindListener;
import am24j.inject.Injector.Key;
import am24j.inject.annotation.Provides;

/**
 * @author avgustinmm
 */
public class ProvidesTest {

  @Provides @Named("x")
  public static class Std implements Provider<AtomicLong> {

    @Provides
    private final AtomicBoolean ab = new AtomicBoolean(true);

    @Provides @Named("x")
    private final AtomicInteger ai = new AtomicInteger(2);

    @Provides
    public Object obj() {
      return new Object();
    }

    @Provides
    public Provider<String> str() {
      return () -> null;
    }

    @Provides @Named("x")
    public String strX() {
      return "x";
    }

    @Override
    public AtomicLong get() {
      return new AtomicLong(4);
    }
  }

  @Test
  public void stanard() {
    final AtomicBoolean obj = new AtomicBoolean(false);
    final AtomicBoolean str = new AtomicBoolean(false);
    final AtomicBoolean strX = new AtomicBoolean(false);

    final AtomicBoolean al = new AtomicBoolean(false);
    final AtomicBoolean ab = new AtomicBoolean(false);
    final AtomicBoolean ai = new AtomicBoolean(false);

    final Injector injector = Injector.newInstance().add(Interceptors.providesBinder()).add(new BindL(strX, str, obj, al, ab, ai));
    injector.getInstance(Key.of(Std.class));
    Assert.assertTrue("obj", obj.get());
    Assert.assertTrue("str", str.get());
    Assert.assertTrue("strX", strX.get());

    Assert.assertTrue("classX", al.get());
    Assert.assertTrue("field", ab.get());
    Assert.assertTrue("fieldX", ai.get());

    Assert.assertEquals("Instance of obj", Object.class, injector.getInstance(Key.of(Object.class)).getClass());
    Assert.assertNotEquals("Obj not equals", injector.getInstance(Key.of(Object.class)), injector.getInstance(Key.of(Object.class)));
    Assert.assertNull("Str is null", injector.getInstance(Key.of(String.class)));
    Assert.assertEquals("x", "x", injector.getInstance(Key.of(String.class, TestUtils.namedX())));
  }

  @Retention(RetentionPolicy.RUNTIME)
  public @interface CustomAnn {}
  @CustomAnn @Named("x")
  public static class Custom  implements Provider<AtomicLong> {

    @CustomAnn
    private final AtomicBoolean ab = new AtomicBoolean(true);

    @CustomAnn @Named("x")
    private final AtomicInteger ai = new AtomicInteger(2);

    @CustomAnn
    public Object obj() {
      return new Object();
    }

    @CustomAnn
    public Provider<String> str() {
      return () -> null;
    }

    @CustomAnn @Named("x")
    public String strX() {
      return "x";
    }

    @Override
    public AtomicLong get() {
      return new AtomicLong(4);
    }
  }

  @Test
  public void custom() {
    final AtomicBoolean obj = new AtomicBoolean(false);
    final AtomicBoolean str = new AtomicBoolean(false);
    final AtomicBoolean strX = new AtomicBoolean(false);

    final AtomicBoolean al = new AtomicBoolean(false);
    final AtomicBoolean ab = new AtomicBoolean(false);
    final AtomicBoolean ai = new AtomicBoolean(false);

    final Injector injector = Injector.newInstance().add(Interceptors.providesBinder(CustomAnn.class)).add(new BindL(strX, str, obj, al, ab, ai));
    injector.getInstance(Key.of(Custom.class));
    Assert.assertTrue("obj", obj.get());
    Assert.assertTrue("str", str.get());
    Assert.assertTrue("strX", strX.get());

    Assert.assertTrue("classX", al.get());
    Assert.assertTrue("field", ab.get());
    Assert.assertTrue("fieldX", ai.get());

    Assert.assertEquals("Instance of obj", Object.class, injector.getInstance(Key.of(Object.class)).getClass());
    Assert.assertNotEquals("Obj not equals", injector.getInstance(Key.of(Object.class)), injector.getInstance(Key.of(Object.class)));
    Assert.assertNull("Str is null", injector.getInstance(Key.of(String.class)));
    Assert.assertEquals("x", "x", injector.getInstance(Key.of(String.class, TestUtils.namedX())));
  }

  private class BindL implements BindListener {

    private final AtomicBoolean strX;
    private final AtomicBoolean str;
    private final AtomicBoolean obj;

    private final AtomicBoolean al;
    private final AtomicBoolean ab;
    private final AtomicBoolean ai;

    private BindL(
        final AtomicBoolean strX, final AtomicBoolean str, final AtomicBoolean obj,
        final AtomicBoolean al, final AtomicBoolean ab, final AtomicBoolean ai) {
      this.strX = strX;
      this.str = str;
      this.obj = obj;

      this.al = al;
      this.ab = ab;
      this.ai = ai;
    }

    @Override
    public void bound(final Key key, final Provider<Object> provider, final Injector injector) {
      if (key.type() == Object.class) {
        if (!key.qualifer().isPresent()) {
          obj.set(true);
        }
      } else if (key.type() == String.class) {
        if (key.qualifer().isPresent()) {
          if (key.qualifer().get() instanceof Named) {
            if ("x".equals(((Named)key.qualifer().get()).value())) {
              strX.set(true);
            }
          }
        } else {
          str.set(true);
        }
      } else if (key.type() == AtomicLong.class) {
        if (key.qualifer().isPresent()) {
          if (key.qualifer().get() instanceof Named) {
            if ("x".equals(((Named)key.qualifer().get()).value())) {
              al.set(true);
            }
          }
        }
      }  else if (key.type() == AtomicBoolean.class) {
        if (!key.qualifer().isPresent()) {
          ab.set(true);
        }
      }  else if (key.type() == AtomicInteger.class) {
        if (key.qualifer().isPresent()) {
          if (key.qualifer().get() instanceof Named) {
            if ("x".equals(((Named)key.qualifer().get()).value())) {
              ai.set(true);
            }
          }
        }
      }
    }
  }
}
