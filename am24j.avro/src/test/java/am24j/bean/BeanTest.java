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

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;

import am24j.avro.Avro.Encoding;

public class BeanTest {

  private final B b = new B();
  private final B b2 = new B();
  private final B b3 = new B();
  private final B b4 = new B();
  {
    b.i(14);
    b.str("dfsdf");
    b2.i(14);
    b2.str("dfsdf");
    b3.i(142);
    b3.str("dfsdddf");
    b4.i(142);
    b4.str("dfsdf");
  }

  private final BOfB bOfB = new BOfB();
  private final BOfB bOfB2 = new BOfB();
  private final BOfB bOfB3 = new BOfB();
  private final BOfB bOfB4 = new BOfB();
  {
    bOfB.i(14);
    bOfB.b(b);
    bOfB2.i(14);
    bOfB2.b(b2);
    bOfB3.i(142);
    bOfB3.b(b3);
    bOfB4.i(142);
    bOfB4.b(b);
  }

  @Test
  public void hash() {
    Assert.assertEquals(b.hashCode(), b2.hashCode());
    Assert.assertNotEquals(b2.hashCode(), b3.hashCode());
    Assert.assertNotEquals(b.hashCode(), b3.hashCode());
    Assert.assertNotEquals(b2.hashCode(), b4.hashCode());
    Assert.assertNotEquals(b.hashCode(), b4.hashCode());

    Assert.assertEquals(bOfB.hashCode(), bOfB2.hashCode());
    Assert.assertNotEquals(bOfB2.hashCode(), bOfB3.hashCode());
    Assert.assertNotEquals(bOfB.hashCode(), bOfB3.hashCode());
    Assert.assertNotEquals(bOfB2.hashCode(), bOfB4.hashCode());
    Assert.assertNotEquals(bOfB.hashCode(), bOfB4.hashCode());

    final Object o = "1";
    Assert.assertNotEquals(b.hashCode(), o.hashCode());
  }

  @Test
  public void equals() {
    Assert.assertEquals(b, b2);
    Assert.assertNotEquals(b2, b3);
    Assert.assertNotEquals(b, b3);
    Assert.assertNotEquals(b, b4);

    Assert.assertEquals(bOfB, bOfB2);
    Assert.assertNotEquals(bOfB2, bOfB3);
    Assert.assertNotEquals(bOfB, bOfB3);
    Assert.assertNotEquals(bOfB, bOfB4);

    final Object o = "1";
    Assert.assertNotEquals(b, o);
    Assert.assertNotEquals(bOfB, o);
    Assert.assertNotEquals(b, null);
    Assert.assertNotEquals(bOfB, null);
  }

  @Test
  public void str() {
    final B b = new B();
    b.i(14);
    b.str("dfsdf");
    final String str = b.toString();
    assertValidJson(str);
  }

  @Test
  public void serialize() throws IOException, ClassNotFoundException {
    final BSerial b = new BSerial();
    b.i(14);
    b.b(this.b);

    final ByteArrayOutputStream baos = new ByteArrayOutputStream();
    final ObjectOutputStream oos = new ObjectOutputStream(baos);
    oos.writeObject(b);

    Assert.assertEquals(b, new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray())).readObject());
  }

  @Test
  public void externalize() throws IOException, ClassNotFoundException {
    final BExtern b = new BExtern();
    b.i(14);
    b.b(this.b);

    final ByteArrayOutputStream baos = new ByteArrayOutputStream();
    final ObjectOutputStream oos = new ObjectOutputStream(baos);
    oos.writeObject(b);

    Assert.assertEquals(b, new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray())).readObject());
  }

  @Test
  public void serializeBuildable() throws IOException, ClassNotFoundException {
    final BSerial b = new BSerial();
    b.i(14);
    b.b(this.b);

    final ByteArrayOutputStream baos = new ByteArrayOutputStream();
    final ObjectOutputStream oos = new ObjectOutputStream(baos);
    oos.writeObject(b);

    Assert.assertEquals(b, new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray())).readObject());
  }

  @Test
  public void externalizeBuildable() throws IOException, ClassNotFoundException {
    final BExtern b = new BExtern();
    b.i(14);
    b.b(this.b);

    final ByteArrayOutputStream baos = new ByteArrayOutputStream();
    final ObjectOutputStream oos = new ObjectOutputStream(baos);
    oos.writeObject(b);

    Assert.assertEquals(b, new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray())).readObject());
  }

  static void assertValidJson(final String str) {
    try {
      final JsonParser parser = new ObjectMapper().getFactory().createParser(str);
      while (parser.nextToken() != null);
    } catch (final Exception e) {
      Assert.fail("Invalid json! " + e);
    }
  }

  public static class B extends Bean<B> {

    private int i;
    private String str;

    public B() {}

    public int i() {
      return i;
    }

    public void i(final int i) {
      this.i = i;
    }

    public String str() {
      return str;
    }

    public void str(final String str) {
      this.str = str;
    }
  }

  public static class BOfB extends Bean<B> {

    private int i;
    private B b;

    public BOfB() {}

    public int i() {
      return i;
    }

    public void i(final int i) {
      this.i = i;
    }

    public B b() {
      return b;
    }

    public void b(final B b) {
      this.b = b;
    }
  }

  public static class BSerial extends Bean.Serial<BSerial> {

    private static final long serialVersionUID = 1L;

    private int i;
    private B b;

    public BSerial() {}

    public int i() {
      return i;
    }

    public void i(final int i) {
      this.i = i;
    }

    public B b() {
      return b;
    }

    public void b(final B b) {
      this.b = b;
    }

    @Override
    public Encoding encoding() {
      return Encoding.Json;
    }

    private void writeObject(final ObjectOutputStream out) throws IOException {
      super.writeObject0(out);
    }

    private void readObject(final ObjectInputStream in) throws IOException, ClassNotFoundException {
      super.readObject0(in);
    }
  }

  public static class BExtern extends Bean.Extern<BExtern> {

    private int i;
    private B b;

    public BExtern() {}

    public int i() {
      return i;
    }

    public void i(final int i) {
      this.i = i;
    }

    public B b() {
      return b;
    }

    public void b(final B b) {
      this.b = b;
    }

    @Override
    public Encoding encoding() {
      return Encoding.Json;
    }
  }
}