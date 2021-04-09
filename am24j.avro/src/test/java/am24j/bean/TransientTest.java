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
package am24j.bean;

import java.beans.Transient;

import org.junit.Assert;
import org.junit.Test;

public class TransientTest {

  @Test
  public void test() {
    final Struct<B> struct = Struct.forClass(B.class);
    Assert.assertEquals("Property count", 1, struct.properties().length);
    Assert.assertEquals("Property name", "i", struct.properties()[0].name());
  }

  public static class B {

    private int i;
    private int t1;
    private int t2;

    public int i() {
      return i;
    }

    public B i(final int i) {
      this.i = i;
      return this;
    }

    @Transient
    public int t1() {
      return t1;
    }

    public B t1(final int t1) {
      this.t1 = t1;
      return this;
    }

    public int t2() {
      return t2;
    }

    @Transient
    public B t2(final int t2) {
      this.t2 = t2;
      return this;
    }
  }
}
