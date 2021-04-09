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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.Flow.Subscription;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import am24j.rpc.IService.B;
import am24j.rpc.IService.BOfB;

/**
 * @author avgustinmm
 */
@Ignore
public class BaseTest {

  protected static IService service;

  @Test
  public void testVoidCall() {
    Assert.assertEquals(service.voidCall().toCompletableFuture().join(), null);
  }

  @Test
  public void testGetCall() {
    Assert.assertEquals(service.getCall(3, "test").toCompletableFuture().join(), "testtesttest");
  }

  @Test(expected = RuntimeException.class)
  public void testThrowComplete() throws Throwable {
    try {
      service.throwExc(true).toCompletableFuture().join();
    } catch (final CompletionException e) {
      throw e.getCause();
    }
  }

  @Test(expected = RuntimeException.class)
  public void testThrowSync() throws Throwable {
    try {
      service.throwExc(false).toCompletableFuture().join();
    } catch (final CompletionException e) {
      throw e.getCause();
    }
  }

  @Test
  public void testStream() {
    final List<Object> expected = Arrays.asList(new Object[] {-1, "0", "1", "2", "3", "4", "5", -2});
    final List<Object> received = Collections.synchronizedList(new ArrayList<>());

    final CompletableFuture<Void> finished = new CompletableFuture<>();
    service.stream(6, new StreamHandler<String>(received, finished));
    finished.join();

    Assert.assertEquals(expected, received);
  }

  @Test
  public void testGetCallB() {
    final B b = new B().i(4).str("test");
    Assert.assertEquals(service.getCallB(3, b).toCompletableFuture().join(), b.x(3));
  }

  @Test
  public void testStreamB() {
    final List<Object> expected = Arrays.asList(new Object[] {-1, B.of(0), B.of(1), B.of(2), B.of(3), B.of(4), B.of(5), -2});
    final List<Object> received = Collections.synchronizedList(new ArrayList<>());

    final CompletableFuture<Void> finished = new CompletableFuture<>();
    service.streamB(6, new StreamHandler<B>(received, finished));
    finished.join();

    Assert.assertEquals(expected, received);
  }

  @Test
  public void testGetCallBOfB() {
    final B b = new B().i(4).str("test");
    final BOfB bOfB = new BOfB().i(5).b(b);
    Assert.assertEquals(service.getCallBOfB(3, bOfB).toCompletableFuture().join(), bOfB.x(3));
  }

  @Test
  public void testStreamBOfB() {
    final List<Object> expected = Arrays.asList(new Object[] {-1, BOfB.of(0), BOfB.of(1), BOfB.of(2), BOfB.of(3), BOfB.of(4), BOfB.of(5), -2});
    final List<Object> received = Collections.synchronizedList(new ArrayList<>());

    final CompletableFuture<Void> finished = new CompletableFuture<>();
    service.streamBOfB(6, new StreamHandler<BOfB>(received, finished));
    finished.join();

    Assert.assertEquals(expected, received);
  }

  private static class StreamHandler<T> implements Subscriber<T> {

    private final List<Object> received;
    private final CompletableFuture<Void> finished;

    private volatile int round;
    private volatile Subscription subscription;

    private StreamHandler(final List<Object> received, final CompletableFuture<Void> finished) {
      this.received = received;
      this.finished = finished;
    }

    @Override
    public void onSubscribe(final Subscription subscription) {
      this.subscription = subscription;

      received.add(-1);

      round++;
      subscription.request(3);
    }

    @Override
    public void onNext(final T item) {
      received.add(item);
      if (round == 1) {
        round++;
        subscription.request(2);
      } else if (round == 2) {
        round++;
        subscription.request(1);
      } else {
        subscription.request(101); // unexoected!!!
      }
    }

    @Override
    public void onError(final Throwable throwable) {
      received.add(-2);
      finished.completeExceptionally(throwable);
    }

    @Override
    public void onComplete() {
      received.add(-2);
      finished.complete(null);
    }
  }
}
