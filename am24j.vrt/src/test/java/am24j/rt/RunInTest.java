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
package am24j.rt;

import org.junit.Assert;
import org.junit.Test;

import am24j.commons.Ctx;
import am24j.commons.Ctx.RunAs;

/**
 * @author avgustinmm
 */
public class RunInTest {

  private static final String TEST = "test";
  private static final String CURRENT = "current";

  @Test
  public void runAsNoCurrent() {
    final RunAsExt runAsExt = new RunAsExt(TEST);

    // before
    Assert.assertNull(Ctx.ctx(RunAsExt.class).orElse(null));

    runAsExt.runAs(() -> {
      // in
      Assert.assertEquals(runAsExt.key, Ctx.ctx(RunAsExt.class).map(rae -> rae.key).orElse(null));
    });

    // after
    Assert.assertNull(Ctx.ctx(RunAsExt.class).orElse(null));
  }

  @Test
  public void runAsDiffKeysNoCurrent() {
    final RunAsExtDifKKey runAsExt = new RunAsExtDifKKey(TEST);

    // before
    Assert.assertNull(Ctx.ctx(RunAs.class).orElse(null));

    runAsExt.runAs(() -> {
      // in
      Assert.assertEquals(runAsExt.key, Ctx.ctx(RunAs.class).map(rae -> ((RunAsExtDifKKey)rae).key).orElse(null));
    });

    // after
    Assert.assertNull(Ctx.ctx(RunAs.class).orElse(null));
  }

  @Test
  public void runAsCurrent() {
    final RunAsExt runAsExtCurrent = new RunAsExt(CURRENT);
    final RunAsExt runAsExt = new RunAsExt(TEST);

    Ctx.ctx(RunAsExt.class, runAsExtCurrent);

    // before
    Assert.assertEquals(runAsExtCurrent, Ctx.ctx(RunAsExt.class).orElse(null));

    runAsExt.runAs(() -> {
      // in
      Assert.assertEquals(runAsExt.key, Ctx.ctx(RunAsExt.class).map(rae -> rae.key).orElse(null));
    });

    // after
    Assert.assertEquals(runAsExtCurrent, Ctx.ctx(RunAsExt.class).orElse(null));
  }

  @Test
  public void runAsDiffKeysCurrent() {
    final RunAsExtDifKKey runAsExtCurrent = new RunAsExtDifKKey(CURRENT);
    final RunAsExtDifKKey runAsExt = new RunAsExtDifKKey(TEST);

    Ctx.ctx(RunAs.class, runAsExtCurrent);

    // before
    Assert.assertEquals(runAsExtCurrent, Ctx.ctx(RunAs.class).orElse(null));

    runAsExt.runAs(() -> {
      // in
      Assert.assertEquals(runAsExt.key, Ctx.ctx(RunAs.class).map(rae -> ((RunAsExtDifKKey)rae).key).orElse(null));
    });

    // after
    Assert.assertEquals(runAsExtCurrent, Ctx.ctx(RunAs.class).orElse(null));
  }

  private static class RunAsExt extends RunAs {

    private String key;

    public RunAsExt(final String key) {
      super(RunAsExt.class);
      this.key = key;
    }
  }

  private static class RunAsExtDifKKey extends RunAs {

    private String key;

    public RunAsExtDifKKey(final String key) {
      super(RunAs.class);
      this.key = key;
    }
  }
}
