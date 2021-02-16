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

import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public class ASync {

  private static final Timer TIMER = new Timer();
  
  public static <T> CompletionStage<T> handler(final CompletionStage<T> stage, final CompletableFuture<T> hamdler) {
    stage.whenComplete((r, e) -> {
      if (e == null) {
        hamdler.complete(r);
      } else {
        hamdler.completeExceptionally(e);
      }
    });
    return hamdler;
  }
  
  public static <T> CompletionStage<T> timeout(final CompletionStage<T> future, final long timeoutMS) {
    final CompletableFuture<T> tFuture = new CompletableFuture<>();
    final AtomicBoolean finished = new AtomicBoolean(false);
    TIMER.schedule(new TimerTask() {
      
      @Override
      public void run() {
        if (finished.compareAndSet(false, true)) {
          tFuture.completeExceptionally(new TimeoutException("Timeout (" + timeoutMS + "ms)!"));
        }
      }
    }, timeoutMS);
    return tFuture.whenComplete((r, t) -> {
      if (finished.compareAndSet(false, true)) {
        if (t == null) {
          tFuture.complete(r);
        } else {
          tFuture.completeExceptionally(t);
        }
      }
    });
  }
  
  public static CompletionStage<Void> sequentiallySkipErrors(final Iterator<CompletionStage<Void>> i) {
    return sequentially(Utils.map(i, cs -> cs.exceptionally(t -> null)));
  }
  // gets furst non error non null / fail on error
  public static CompletionStage<Void> sequentially(final Iterator<CompletionStage<Void>> i) {
    final CompletableFuture<Void> future = new CompletableFuture<>();
    next(false, i, future);
    return future;
  }
  
  public static <T> CompletionStage<T> sequentiallyGetSkipErrors(final Iterator<CompletionStage<T>> i) {
    return sequentiallyGet(Utils.map(i, cs -> cs.exceptionally(t -> null)));
  }
  // gets furst non error non null / fail on error
  public static <T> CompletionStage<T> sequentiallyGet(final Iterator<CompletionStage<T>> i) {
    final CompletableFuture<T> future = new CompletableFuture<>();
    next(true,  i, future);
    return future;
  }
  
  private static <T> void next(final boolean get, final Iterator<CompletionStage<T>> i, final CompletableFuture<T> future) {
    if (i.hasNext()) {        
      i.next().whenCompleteAsync((r, e) -> { // asyn in order to prevent StackOverflowError if many are completed in same thread
        if (e == null) {
          if (get && r != null) {
            future.complete(r);
          } else {
            next(get, i, future);
          }
        } else {
          future.completeExceptionally(e);
        }
      });
    } else {
      future.complete(null);
    }
  }
  
  public static class Lock {
    
    private CompletionStage<Void> lokk = CompletableFuture.completedFuture(null);
    
    public <T> CompletionStage<T> syncr(final Supplier<CompletionStage<T>> fn) {
      final CompletableFuture<T> future = new CompletableFuture<>();
      final AtomicBoolean in = new AtomicBoolean(true);
      synchronized (this) {
        lokk = lokk.whenCompleteAsync((v, t) -> 
          fn.get().whenComplete((r, e) -> {
            if (e == null) {
              future.complete(r);
            } else {
              future.completeExceptionally(e);
            }
          }), in.get() ? ForkJoinPool.commonPool() : Runnable::run); // TODO - does ForkJoinPool execute step out of this execution - i.e. out of synchronized)
      }
      in.set(false);
      return future;
    }
  }
//  
//  public static void main(final String[] a) {
//    final ForkJoinPool pool = ForkJoinPool.commonPool();
//    pool.execute(() -> {
//      final AtomicBoolean in = new AtomicBoolean(true);
//      pool.execute(() -> {
//        System.out.println("--> " + in);
//      });
//      in.set(false);
//    });
//  }
}
