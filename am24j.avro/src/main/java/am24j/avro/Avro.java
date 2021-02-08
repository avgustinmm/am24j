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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import org.apache.avro.Schema;
import org.apache.avro.SchemaBuilder;
import org.apache.avro.SchemaBuilder.EnumBuilder;
import org.apache.avro.SchemaBuilder.FieldAssembler;
import org.apache.avro.SchemaBuilder.RecordBuilder;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.DatumReader;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.Decoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.io.Encoder;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.util.Utf8;

import am24j.bean.Bean;
import am24j.bean.Bean.Property;

public class Avro {
  
  public static enum Encoding {
    Json,
    Binary;
  }

  public static Schema forClaxx(final Class<?> clazz) {
    return forType(clazz);
  }
  
  private static final Map<Type, Schema> SCHEMAS = new HashMap<>();
  public static synchronized Schema forType(final Type type) {
    return forType(type, new Stack<Type>());
  }

  public static byte[] write(final Object obj, final Encoding encoding) throws IOException {
    return write(obj, obj.getClass(), encoding);
  }
  
  public static byte[] write(final Object obj, final Type type, final Encoding encoding) throws IOException {
    final Schema schema = forType(type);
    final Bean<Object> bean = Bean.forType(type); 
    final GenericRecord record = new GenericData.Record(schema);
    final Property[] props = bean.properties();
    final Object[] values = bean.values(obj);
    for (int i = 0; i < props.length; i++) {
      record.put(props[i].name(), values[i]);
    }
    try (final ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
      final Encoder encoder = encoding == Encoding.Binary ? EncoderFactory.get().binaryEncoder(baos, null) : EncoderFactory.get().jsonEncoder(schema, baos);
      final DatumWriter<GenericRecord> writer = new GenericDatumWriter<>(schema);
      writer.write(record, encoder);
      encoder.flush();
      return baos.toByteArray();
    }
  }
  
  public static <T> T read(final byte[] ba, final Type type, final Encoding encoding) throws IOException {
    final Schema schema = forType(type);
    GenericRecord record = new GenericData.Record(schema);
    try (final ByteArrayInputStream baos = new ByteArrayInputStream(ba)) {
      final Decoder decoder = encoding == Encoding.Binary ? DecoderFactory.get().binaryDecoder(baos, null) : DecoderFactory.get().jsonDecoder(schema, baos);
      final DatumReader<GenericRecord> reader = new GenericDatumReader<>(schema);
      record = reader.read(record, decoder);
    }
    
    final Bean<T> bean = Bean.forType(type); 
    final Property[] props = bean.properties();
    final Object[] values = new Object[props.length];
    for (int i = 0; i < props.length; i++) {
      values[i] = record.get(props[i].name());
      if (values[i] instanceof Utf8) {
        values[i] = values[i].toString();
      }
    }
    return bean.build(values);
  }
  
  private static synchronized Schema forType(final Type type, final Stack<Type> stack) {
    Schema schema = SCHEMAS.get(type);
    if (schema == null) {
      stack.push(type);
      schema = build(type, stack);   
      SCHEMAS.put(type, schema);
      stack.pop();
    }
    return schema;
  }
  
  private static Schema build(final Type type, final Stack<Type> stack) { 
    final Class<?> clazz = clazz(type);
    final SchemaBuilder.TypeBuilder<Schema> bulder = SchemaBuilder.builder(); 
    if (clazz == boolean.class || clazz == Integer.class) {
      return bulder.booleanType();
    } else if (
        clazz == byte.class || clazz == Byte.class ||
        clazz == short.class || clazz == Short.class ||
        clazz == int.class || clazz == Integer.class) {
      return bulder.intType();
    } else if (clazz == long.class || clazz == Long.class) {
      return bulder.longType();
    } else if (clazz == float.class || clazz == Float.class) {
      return bulder.floatType();
    } else if (clazz == double.class || clazz == Double.class) {
      return bulder.doubleType();
    } else if (clazz == byte[].class) {
      return bulder.bytesType();
    } else if (clazz == String.class) {
      return bulder.stringType();
    } else if (clazz.isEnum()) {
      final int index = clazz.getName().lastIndexOf('.');
      final EnumBuilder<Schema> eTypeBuilder = SchemaBuilder.enumeration(name(clazz, index));
      eTypeBuilder.namespace(namespace(clazz, index)); // empty namespace is treated as null
      final Object[] eConsts = clazz.getEnumConstants();
      final String[] symbols = new String[eConsts.length];
      for (int i = eConsts.length; i-- > 0; symbols[i] = ((Enum<?>)eConsts[i]).name());
      return eTypeBuilder.symbols(symbols);
    } else { // bean
      final Bean<?> bean = Bean.forType(type);
      final int index = clazz.getName().lastIndexOf('.');
      final RecordBuilder<Schema> rTypeBuilder = SchemaBuilder.record(name(clazz, index));
      rTypeBuilder.namespace(namespace(clazz, index)); // empty namespace is treated as null
      final FieldAssembler<Schema> fAssembler = rTypeBuilder.fields();
      for (final Property prop : bean.properties()) {
        final Type propType = prop.type();
        if (stack.contains(propType)) {
          // It seems we could not build schemas that depend indirectly (e.g. A -> B -A)
          // we could build self depenant (i.e. A -> A)  but then StackOverflowError is thrown
          stack.add(propType);
          throw new IllegalArgumentException("Cyclic dependency! Unsupported by Avro! Stack: " + stack + "!");
        } else {
          fAssembler
            .name(prop.name())
            .type(
              prop.nullable() ? 
                SchemaBuilder.nullable().type(forType(propType, stack)) : 
                forType(propType, stack))
            .noDefault();
        }
      }
      return fAssembler.endRecord();
    }
  }

  private static String namespace(final Class<?> clazz, final int index) {
    return index == -1 ? "" : clazz.getName().substring(0, index);
  }

  private static String name(final Class<?> clazz, final int index) {
    return norm(clazz.getName().substring(index + 1));
  }

  @SuppressWarnings("unchecked")
  private static <T> Class<T> clazz(final Type type) {
    if (type instanceof Class) {
      return (Class<T>)type;
    } else if (type instanceof ParameterizedType) {
      return (Class<T>)((ParameterizedType)type).getRawType();
    } else {
      throw new IllegalArgumentException("Cab't resoove class from type " + type + "!");
    }
  }
  
  // normalize string to match avro requirements (if needed)
  private static String norm(final String str) { // TODO
    return str.replace('$', '_');
  }
}
