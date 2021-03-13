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
package am24j.avro;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;

import am24j.avro.Avro.Encoding;
import am24j.bean.StrictTest.BeanStd;
import am24j.bean.StrictTest.Buildable;
import am24j.bean.StrictTest.Buildable.Builder;
import am24j.bean.StrictTest.BuildableNewBuilder;
import am24j.bean.StrictTest.BuildableReq;

public class AvroTest {

  @Test
  public void bean() throws Throwable {
    final BeanStd bean = new BeanStd().setX(23).opt("test-str");
    bean.y(90);
    check(bean);
  }

  @Test
  public void beanOptNull() throws Throwable {
    final BeanStd bean = new BeanStd().setX(23);
    bean.y(90);
    check(bean);
  }

  @Test
  public void buildable() throws Throwable {
    final Builder builder = Buildable.builder().setX(23).opt("test-str");
    builder.y(90);
    check(builder.build());
  }

  @Test
  public void buildableOptNull() throws Throwable {
    final Builder builder = Buildable.builder().setX(23).opt(null);
    builder.y(90);
    check(builder.build());
  }

  @Test
  public void buildableNewBuilder() throws Throwable {
    final BuildableNewBuilder.Builder builder = BuildableNewBuilder.newBuilder().setX(23).opt("test-str");
    builder.y(90);
    check(builder.build());
  }

  @Test
  public void buildableNewBuilderOptNull() throws Throwable {
    final BuildableNewBuilder.Builder builder = BuildableNewBuilder.newBuilder().setX(23);
    builder.y(90);
    check(builder.build());
  }

  @Test (expected = IllegalArgumentException.class)
  public void buildableReq() throws Throwable {
    final BuildableReq.Builder builder = BuildableReq.newBuilder().setX(23);
    builder.y(90);
    builder.req(BuildableReq.newBuilder().setX(24).build());
    check(builder.build());
  }

  private static void check(final Object bean) throws IOException {
    {
      final byte[] ba = Avro.encode(bean, Encoding.Json);
      final Object decoded = Avro.decode(ba, bean.getClass(), Encoding.Json);
      Assert.assertEquals(bean, decoded);
    }

    {
      final byte[] ba = Avro.encode(bean, Encoding.Binary);
      final Object decoded = Avro.decode(ba, bean.getClass(), Encoding.Binary);
      Assert.assertEquals(bean, decoded);
    }
  }
//
//  public static void main(final String[] args) {
//    final Schema schema = Avro.forClaxx(BeanTest.Buildable.class);
//    System.out.println(schema.toString());
//    final Schema schemaReq = Avro.forClaxx(BeanTest.BuildableReq.class);
//    System.out.println(schemaReq.toString());
//    // cyclic refs - not supporetd - how to do?
////    final Schema schemaCyclicReq = Avro.forClaxx(CyclicReq1.class);
////    System.out.println(schemaCyclicReq.toString());
//  }

  public static class CyclicReq1 {

    private volatile CyclicReq3 req;

    public void req(final CyclicReq3 req) {
      this.req = req;
    }

    public CyclicReq3 req() {
      return req;
    }
  }

  public static class CyclicReq2 {

    private volatile CyclicReq1 req;

    public void req(final CyclicReq1 req) {
      this.req = req;
    }

    public CyclicReq1 req() {
      return req;
    }
  }

  public static class CyclicReq3 {

    private volatile CyclicReq2 req;

    public void req(final CyclicReq2 req) {
      this.req = req;
    }

    public CyclicReq2 req() {
      return req;
    }
  }
}
