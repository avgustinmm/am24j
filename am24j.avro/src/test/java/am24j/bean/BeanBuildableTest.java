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
package am24j.bean;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.junit.Assert;
import org.junit.Test;

import am24j.bean.BeanTest.BExtern;
import am24j.bean.BeanTest.BSerial;

public class BeanBuildableTest {

  @Test
  public void hash() {
    final Buildable b = Buildable.builder().a(14).b("dfsdf").build();
    final Buildable b2 = Buildable.builder().a(14).b("dfsdf").build();
    final Buildable b3 = Buildable.builder().a(142).b("dfsdddf").build();
    Assert.assertEquals(b.hashCode(), b2.hashCode());
    Assert.assertNotEquals(b2.hashCode(), b3.hashCode());
    Assert.assertNotEquals(b.hashCode(), b3.hashCode());
    System.out.println(b.hashCode());
    System.out.println(b2.hashCode());
    System.out.println(b3.hashCode());
  }

  @Test
  public void equals() {
    final Buildable b = Buildable.builder().a(14).b("dfsdf").build();
    final Buildable b2 = Buildable.builder().a(14).b("dfsdf").build();
    final Buildable b3 = Buildable.builder().a(142).b("dfsdddf").build();
    Assert.assertEquals(b, b2);
    Assert.assertNotEquals(b2, b3);
    Assert.assertNotEquals(b, b3);
  }

  @Test
  public void str() {
    final Buildable b = Buildable.builder().a(14).b("dfsdf").build();
    System.out.println(b);
  }

  @Test
  public void serialize() throws IOException, ClassNotFoundException {
    final Buildable b = Buildable.builder().a(14).b("dfsdf").build();

    final ByteArrayOutputStream baos = new ByteArrayOutputStream();
    final ObjectOutputStream oos = new ObjectOutputStream(baos);
    oos.writeObject(new Serial(b));

    Assert.assertEquals(b, ((Serial)new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray())).readObject()).unwrap());
  }

  @Test
  public void externalize() throws IOException, ClassNotFoundException {
    final Buildable b = Buildable.builder().a(14).b("dfsdf").build();

    final ByteArrayOutputStream baos = new ByteArrayOutputStream();
    final ObjectOutputStream oos = new ObjectOutputStream(baos);
    oos.writeObject(new Extern(b));

    Assert.assertEquals(b, ((Extern)new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray())).readObject()).unwrap());
  }

  @Test
  public void serializeBuildable() throws IOException, ClassNotFoundException {
    final BSerial b = new BSerial();
    b.a(14);
    b.b("dfsdf");

    final ByteArrayOutputStream baos = new ByteArrayOutputStream();
    final ObjectOutputStream oos = new ObjectOutputStream(baos);
    oos.writeObject(b);

    Assert.assertEquals(b, new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray())).readObject());
  }

  @Test
  public void externalizeBuildable() throws IOException, ClassNotFoundException {
    final BExtern b = new BExtern();
    b.a(14);
    b.b("dfsdf");

    final ByteArrayOutputStream baos = new ByteArrayOutputStream();
    final ObjectOutputStream oos = new ObjectOutputStream(baos);
    oos.writeObject(b);

    Assert.assertEquals(b, new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray())).readObject());
  }

  public static class Buildable extends Bean<Buildable> {

    private final int a;
    private final String b;

    private Buildable(final int a, final String b) {
      this.a = a;
      this.b = b;
    }

    public static Builder builder() {
      return new Builder();
    }

    public int a() {
      return a;
    }

    public String b() {
      return b;
    }

    public static class Builder {

      private int a;
      private String b;

      public Builder a(final int a) {
        this.a = a;
        return this;
      }

      public Builder b(final String b) {
        this.b = b;
        return this;
      }

      public Buildable build() {
        return new Buildable(a, b);
      }
    }
  }

  public static class Serial extends Bean.BuildableWrapperSerial<Buildable> {

    private static final long serialVersionUID = 1L;

    public Serial() {}

    private Serial(final Buildable buildable) {
      super(buildable);
    }

    @Override
    protected Class<Buildable> type() {
      return Buildable.class;
    }
  }

  public static class Extern extends Bean.BuildableWrapperExtern<Buildable> {

    public Extern() {}

    private Extern(final Buildable buildable) {
      super(buildable);
    }

    @Override
    protected Class<Buildable> type() {
      return Buildable.class;
    }
  }
}
