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
package am24j.commons;

import java.util.regex.Pattern;

import org.junit.Assert;
import org.junit.Test;

public class PatternsTest {

  @Test
  public void testDot() {
    final Pattern p1CI = Patterns.forWildCard("x.y", false);
    System.out.println("-> " + p1CI);
    Assert.assertFalse(p1CI.matcher("xzy").matches());
    Assert.assertFalse(p1CI.matcher("xzY").matches());
    Assert.assertTrue(p1CI.matcher("x.y").matches());
    Assert.assertFalse(p1CI.matcher("x.Y").matches());
    final Pattern p1CS = Patterns.forWildCard("x.y", true);
    Assert.assertTrue(p1CS.matcher("x.Y").matches());

    final Pattern p2CS = Patterns.forWildCard("x.y.z", true);
    Assert.assertTrue(p2CS.matcher("x.Y.z").matches());
  }

  @Test
  public void testWild() {
    final Pattern p1CI = Patterns.forWildCard("x*y", false);
    System.out.println("-> " + p1CI);
    Assert.assertTrue(p1CI.matcher("xfdfdfdfy").matches());
    Assert.assertFalse(p1CI.matcher("xhhhdhdhdY").matches());
    final Pattern p1CS = Patterns.forWildCard("x*y", true);
    Assert.assertTrue(p1CS.matcher("xffdddY").matches());

    final Pattern p2CS = Patterns.forWildCard("x*y*z", true);
    Assert.assertTrue(p2CS.matcher("xffdddYmmmmmz").matches());  }
}
