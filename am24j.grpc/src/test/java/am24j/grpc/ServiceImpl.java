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
package am24j.grpc;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.Flow.Subscription;

import am24j.rpc.Remote;

import java.util.concurrent.Semaphore;

/**
 * TODO - extends test and use-cases:
 * <ul>
 *   <li>checking authentication / context (invoke calls, sub</li>
 *   <li>bean arguments / returns completion statge types</li>
 *   <li>array arguments / returns completion statge types</li>
 *   <li>returning CompletableFuture (should be supported along with CompletionStage)</li>
 *   <li>sync and async processing in server</li>
 *   <li>checking that response is in the same thread</li>
 *   <li>...</li>
 * </ul>
 * 
 * @author avgustinmm
 */
@Remote
public class ServiceImpl implements IService {

  @Override
  public CompletionStage<Void> voidCall() {
    return CompletableFuture.completedStage(null);
  }

  @Override
  public CompletionStage<String> getCall(final int i, final String str) {
    final StringBuffer sb = new StringBuffer();
    for (int j = i; j-- > 0; sb.append(str));
    return CompletableFuture.completedStage(sb.toString());
  }

  @Override
  public CompletionStage<String> throwExc(final boolean completeExceptionally) {
    if (completeExceptionally) {
      return CompletableFuture.failedFuture(new RuntimeException());
    } else {
      throw new RuntimeException();
    }
  }

  @Override
  public void stream(final int i, final Subscriber<String> subscriber) {
    final Semaphore semaphore = new Semaphore(i);
    subscriber.onSubscribe(new Subscription() {
      
      @Override
      public void request(final long n) {
        for (int j = 0; j < n; j++) {
          if (semaphore.tryAcquire()) {
            subscriber.onNext(String.valueOf(i - semaphore.availablePermits() - 1));
          } else {
            subscriber.onComplete();
          }
        }
      }
      
      @Override
      public void cancel() {
        
      }
    });
  }
}
