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

import javax.inject.Named;

import org.junit.Assert;
import org.junit.Test;

@Named("dfsdfda")
public class NamedTest {

  @Named("dffsdfsda")
  @Test
  public void testGenerated() {
    final String[] names = {
      "sdfsfdf",
      "dgdsa34w3",
      "sfs3dcq",
      "sgfw3wefd"
    };
    final Named[] named = new Named[names.length];
    for (int i = named.length; i-- > 0; named[i] = Utils.named(names[i]));

    for (int i = named.length; i-- > 0;) {
      for (int j = named.length; j-- > 0;) {
        if (i == j) {
          Assert.assertEquals("hashCode", named[i].hashCode(), named[j].hashCode());
          Assert.assertTrue("equaks", named[i].equals(named[j]));
        } else {
          Assert.assertNotEquals("hashCode", named[i].hashCode(), named[j].hashCode());
          Assert.assertFalse("equaks", named[i].equals(named[j]));
        }
      }
    }
  }

  @Named("ewvd c9ed")
  @Test
  public void testGenVsStd() throws NoSuchMethodException, SecurityException {
    final Named[] std = {
      NamedTest.class.getAnnotation(Named.class),
      NamedTest.class.getMethod("testGenerated").getAnnotation(Named.class),
      NamedTest.class.getMethod("testGenVsStd").getAnnotation(Named.class)
    };

    final Named[] gen = {
      Utils.named("dfsdfda"),
      Utils.named("dffsdfsda"),
      Utils.named("ewvd c9ed")
    };

    for (int i = std.length; i-- > 0;) {
      for (int j = gen.length; j-- > 0;) {
        if (i == j) {
          Assert.assertEquals("hashCode", std[i].hashCode(), gen[j].hashCode());
          Assert.assertTrue("equaks", std[i].equals(gen[j]));
          Assert.assertTrue("equaks", gen[i].equals(std[j]));
        } else {
          Assert.assertNotEquals("hashCode", std[i].hashCode(), gen[j].hashCode());
          Assert.assertFalse("equaks", std[i].equals(gen[j]));
          Assert.assertFalse("equaks", gen[i].equals(std[j]));
        }
      }
    }
  }
}
