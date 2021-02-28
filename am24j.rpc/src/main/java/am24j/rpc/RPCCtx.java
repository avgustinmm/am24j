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
package am24j.rpc;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

public abstract class RPCCtx {
  
  public static RPCCtx NULL = new RPCCtx() {};

  private static final ThreadLocal<RPCCtx> CTX = new ThreadLocal<>();
  
  public Optional<RPCCtx> current() {
    return Optional.ofNullable(CTX.get());
  }

  public void runlAs(final Runnable runnable) {
    callAs(v -> {
      runnable.run();
      return null;
    }, null);
  }
  
  public <T> T getlAs(final Supplier<T> supplier) {
    return callAs(v -> supplier.get(), null);
  }
  
  public <T, A> T callAs(final Function<A, T> function, final A arg) {
    final RPCCtx current = CTX.get();
    if (current == this) {
      return function.apply(arg);
    } else {
      CTX.set(this);
      try {
        return function.apply(arg);
      } finally {
        if (current == null) {
          CTX.remove();
        } else {
          CTX.set(current);
        }
      }
    }
  }
}
