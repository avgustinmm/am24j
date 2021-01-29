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

import javax.inject.Inject;

import org.junit.Assert;
import org.junit.Test;

import am24j.inject.Injector.Key;

/**
 * @author avgustinmm
 */
public class CyclicDepsTest {
  
  public static class A1 {   
    @Inject
    public A1(final A2I direct) {
      System.out.println("<init> A1()");
    }
  }
  
  @am24j.inject.annotation.ImplementedBy(A2.class)
  public static interface A2I {}
  
  public static class A2 implements A2I{     
    @Inject
    public A2(final A1 direct) {
      System.out.println("<init> A2");
    }
  }
  
  @Test(expected = InjectException.class)
  public void test() throws Exception {
    final Injector injector = 
        Injector.newInstance()
          .add(Resolvers.implementedBy()) 
          .add(Interceptors.providesBinder());
    try {
      injector.getInstance(Key.of(A1.class));
    } catch (final InjectException e) {
      Assert.assertTrue("Expected message / exception", e.getMessage().contains("Cyclic dependencies!"));
      throw e;
    }
  }
}
