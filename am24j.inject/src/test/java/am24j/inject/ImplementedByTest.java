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

import javax.inject.Inject;

import org.junit.Assert;
import org.junit.Test;

import am24j.inject.Injector.Key;

/**
 * @author avgustinmm
 */
public class ImplementedByTest {

  @am24j.inject.annotation.ImplementedBy(StandardImpl.class)
  public static interface Standard {}
  public static class StandardImpl implements Standard {
    @Inject
    public StandardImpl(final String str) {}
  }
  @Test
  public void testStandard() {
    final Injector injector = Injector.newInstance().add(Resolvers.implementedBy()).bind(Key.of(String.class), "str");
    Assert.assertEquals("Of expectged type", StandardImpl.class, injector.getInstance(Key.of(Standard.class)).getClass());
  }

  @Retention(RetentionPolicy.RUNTIME)
  @interface CustomAnn {
    public Class<?> value();
  }
  @CustomAnn(CustomImpl.class)
  public static interface Custom {}
  public static class CustomImpl implements Custom {}
  @Test
  public void testCustom() {
    final Injector injector = Injector.newInstance().add(Resolvers.implementedBy(CustomAnn.class));
    Assert.assertEquals("Of expectged type", CustomImpl.class, injector.getInstance(Key.of(Custom.class)).getClass());
  }
}
