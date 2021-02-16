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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;

import org.junit.Test;

public class AsyncTest {

  @Test
  public void seq() {
    final List<Supplier<CompletionStage<Void>>> all = new ArrayList<>();
    for (int i = 0; i < 10_000; i++) {
      all.add(() -> CompletableFuture.completedFuture(null));
    }
    ASync.<Void>sequentially(Utils.map(all.iterator(), Supplier::get)).toCompletableFuture().join();
  }
}
