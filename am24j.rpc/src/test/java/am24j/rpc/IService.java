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

import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow.Subscriber;

import am24j.bean.Bean;

/**
 * Test service
 *
 * @author avgustinmm
 */
@Service(name = "test", version = "0.0")
public interface IService {

  public CompletionStage<Void> voidCall();

  public CompletionStage<String> getCall(final int i, final String str);

  public CompletionStage<String> throwExc(final boolean completeExceptionally);

  public void stream(final int i, final Subscriber<String> subscriber);

  public CompletionStage<B> getCallB(final int i, final B b);

  public void streamB(final int i, final Subscriber<B> subscriber);

  public CompletionStage<BOfB> getCallBOfB(final int i, final BOfB bOfB);

  public void streamBOfB(final int i, final Subscriber<BOfB> subscriber);

  public static class B extends Bean<B> {

    private int i;
    private String str;

    public B() {}

    public int i() {
      return i;
    }

    public B i(final int i) {
      this.i = i;
      return this;
    }

    public String str() {
      return str;
    }

    public B str(final String str) {
      this.str = str;
      return this;
    }

    public B x(final int i) {
      final StringBuffer sb = new StringBuffer();
      for (int j = i(); j-- > 0; sb.append(str()));
      return new B().i(i * i()).str(sb.toString());
    }

    public static B of(final int i) {
      return new B().i(i).str(String.valueOf(i));
    }
  }

  public static class BOfB extends Bean<B> {

    private int i;
    private B b;

    public BOfB() {}

    public int i() {
      return i;
    }

    public BOfB i(final int i) {
      this.i = i;
      return this;
    }

    public B b() {
      return b;
    }

    public BOfB b(final B b) {
      this.b = b;
      return this;
    }

    public BOfB x(final int i) {
      return new BOfB().i(i * i()).b(b().x(i));
    }

    public static BOfB of(final int i) {
      return new BOfB().i(i).b(new B().i(i).str(String.valueOf(i)));
    }
  }
}
