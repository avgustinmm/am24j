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

import java.io.Externalizable;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Objects;

import org.apache.avro.Schema;

import am24j.avro.Avro;
import am24j.avro.Avro.Encoding;
import am24j.bean.Struct.Property;
import io.vertx.core.json.JsonObject;

/**
 * Interface that may be used by bean in order to get som useful mehotds
 *
 * @param T type of the bean
 * @author avgustinmm
 */
public class Bean<T> {

  @Override
  public int hashCode() {
    return Arrays.hashCode(struct().values(cast()));
  }

  @Override
  public boolean equals(final Object o) {
    if (o != null) {
      if (this.getClass( ) == o.getClass()) {
        final Struct<T> struct = struct();
        final Object[] values = values(struct);
        @SuppressWarnings("unchecked")
        final Object[] oValues = struct.values((T)o);
        for (int i = oValues.length; i-- > 0;) {
          if (!Objects.equals(oValues[i], values[i])) {
            return false;
          }
        }
        return true;
      }
    }
    return false;
  }

  @Override
  public String toString() {
    return toStringB(this);
  }

  @SuppressWarnings("unchecked")
  public Struct<T> struct() {
    return Struct.forClass((Class<T>)this.getClass());
  }

  public Schema schema() {
    return Avro.forClaxx(this.getClass());
  }

  @SuppressWarnings("unchecked")
  protected T cast() {
    return (T)this;
  }

  @SuppressWarnings("unchecked")
  protected Object[] values(final Struct<T> struct) {
    return struct.values((T)this);
  }

  private static String toString(final Object obj) {
    if (obj == null) {
      return "null";
    } else {
      final Class<?> clazz = obj.getClass();
      if (clazz == boolean.class || clazz == Integer.class ||
          clazz == byte.class || clazz == Byte.class ||
          clazz == short.class || clazz == Short.class ||
          clazz == int.class || clazz == Integer.class ||
          clazz == long.class || clazz == Long.class ||
          clazz == float.class || clazz == Float.class ||
          clazz == double.class || clazz == Double.class ||
          clazz.isEnum()) {
        return String.valueOf(obj);
      } else if (clazz == byte[].class) {
        return Arrays.toString((byte[])obj);
      } else if (obj instanceof Object[]) {
        return Arrays.asList((Object[])obj).toString();
      } else if (clazz == String.class) {
        return (String)obj;
      } else {
        return toStringB(obj);
      }
    }
  }

  private static String toStringB(final Object obj) {
    final JsonObject json = new JsonObject();
    json.put("#type", obj.getClass().getName());
    @SuppressWarnings("rawtypes")
    final Struct struct = Struct.forClass(obj.getClass());
    @SuppressWarnings("unchecked")
    final Object[] values = struct.values(obj);
    final Property[] properties = struct.properties();
    for (int i = 0; i < properties.length; i++) {
      json.put(properties[i].name(), toString(values[i]));
    }
    return json.encodePrettily();
  }

  /**
   * Externalizable base for standard or direct beans (not buildable).
   *
   * @author avgustinmm
   */
  public static class Extern<T> extends Bean<T> implements Externalizable {

    @Override
    public void writeExternal(final ObjectOutput out) throws IOException {
      Avro.write(this, encoding(), new OO2OS(out));
    }

    @Override
    public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
      final Struct<T> struct = struct();
      final Object[] values = Avro.values(this.getClass(), encoding(), struct, new InputStream() {

        @Override
        public int read() throws IOException {
          return in.read();
        }

        @Override
        public int read(final byte[] b, final int off, final int len) throws IOException {
          return in.read(b, off, len);
        }
      });
      final Property[] props = struct.properties();
      for (int i = 0; i < props.length; i++) {
        try {
          props[i].set(values[i], this);
        } catch (final Throwable t) {
          throw t instanceof RuntimeException ? (RuntimeException)t : new RuntimeException(t);
        }
      }
    }

    protected Encoding encoding() {
      return Encoding.Binary;
    }
  }

  /**
   * Serializable base for standard or direct beans (not buildable).
   *
   * @author avgustinmm
   */
  public static class Serial<T> extends Bean<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    private void writeObject(final ObjectOutputStream out) throws IOException {
      Avro.write(this, encoding(), out);
    }

    private void readObject(final ObjectInputStream in) throws IOException, ClassNotFoundException {
      final Struct<T> struct = struct();
      final Object[] values = Avro.values(this.getClass(), encoding(), struct, in);
      final Property[] props = struct.properties();
      for (int i = 0; i < props.length; i++) {
        try {
          props[i].set(values[i], this);
        } catch (final Throwable t) {
          throw t instanceof RuntimeException ? (RuntimeException)t : new RuntimeException(t);
        }
      }
    }

    protected Encoding encoding() {
      return Encoding.Binary;
    }
  }

  /**
   * Wrapper for buildable beans to externalizable externalizable
   *
   * @author avgustinmm
   */
  public static abstract class BuildableWrapperExtern<T> implements Externalizable {

    private volatile T buildable;

    public BuildableWrapperExtern() {} // for deserialization purposes

    public BuildableWrapperExtern(final T buildable) {
      this.buildable = buildable;
    }

    public T unwrap() {
      return buildable;
    }

    @Override
    public void writeExternal(final ObjectOutput out) throws IOException {
      Avro.write(buildable, encoding(), new OO2OS(out));
    }

    @Override
    public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
      buildable = Avro.read(type(), encoding(), new OI2IS(in));
    }

    protected abstract Class<T> type();

    protected Encoding encoding() {
      return Encoding.Binary;
    }
  }

  /**
   * Wrapper for buildable beans to externalizable serializable
   *
   * @author avgustinmm
   */
  public static abstract class BuildableWrapperSerial<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    private volatile T buildable;

    public BuildableWrapperSerial() {} // for deserialization purposes

    public BuildableWrapperSerial(final T buildable) {
      this.buildable = buildable;
    }

    public T unwrap() {
      return buildable;
    }

    private void writeObject(final ObjectOutputStream out) throws IOException {
      Avro.write(buildable, encoding(), out);
    }

    private void readObject(final ObjectInputStream in) throws IOException, ClassNotFoundException {
      buildable = Avro.read(type(), encoding(), in);
    }

    protected abstract Class<T> type();

    protected Encoding encoding() {
      return Encoding.Binary;
    }
  }

  private static final class OO2OS extends OutputStream {

    private final ObjectOutput out;

    private OO2OS(ObjectOutput out) {
      this.out = out;
    }

    @Override
    public void write(final int b) throws IOException {
      out.write(b);
    }

    @Override
    public void write(final byte b[], final int off, final int len) throws IOException {
      out.write(b, off, len);
    }
  }

  private static final class OI2IS extends InputStream {

    private final ObjectInput in;

    private OI2IS(ObjectInput in) {
      this.in = in;
    }

    @Override
    public int read() throws IOException {
      return in.read();
    }

    @Override
    public int read(final byte[] b, final int off, final int len) throws IOException {
      return in.read(b, off, len);
    }
  }
}
