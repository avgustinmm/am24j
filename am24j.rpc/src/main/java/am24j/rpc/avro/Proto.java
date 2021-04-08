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
package am24j.rpc.avro;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Flow.Subscriber;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.avro.Protocol;
import org.apache.avro.Schema;
import org.apache.avro.SchemaBuilder;
import org.apache.avro.SchemaBuilder.FieldAssembler;
import org.apache.avro.SchemaBuilder.RecordBuilder;
import org.apache.avro.generic.GenericData.Record;
import org.apache.avro.io.Decoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.io.Encoder;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.avro.specific.SpecificDatumWriter;
import org.apache.avro.util.Utf8;

import am24j.avro.Avro;
import am24j.commons.Reflect;
import am24j.commons.Tuple2;
import am24j.rpc.Service;

/**
 * Avro protocol builder
 *
 * @author avgustinmm
 */
public class Proto {

  private Proto() {}

  public static void checkServiceMethod(final Method method) {
    if (method.getReturnType() == void.class || method.getReturnType() == Void.class) {
      final Class<?>[] parameterTypes = method.getParameterTypes();
      if (parameterTypes.length == 0 || parameterTypes[parameterTypes.length - 1] != Subscriber.class || !(method.getGenericParameterTypes()[parameterTypes.length - 1] instanceof ParameterizedType)) {
        throw new IllegalArgumentException("Void method must be a stream so the last argument must be a Subscriber with generic type! Incompatible method: " + method + "!");
      }
    } else {
      final Class<?> returnType = method.getReturnType();
      if ((returnType != CompletionStage.class && returnType != CompletableFuture.class) || !(method.getGenericReturnType() instanceof ParameterizedType)) {
        throw new IllegalArgumentException("Call method must return CompletionStage or CompletableFuture with generic type! Incompatible method: " + method + "!");
      }
    }
  }

  public static boolean isServiceMethod(final Method method) {
    try {
      checkServiceMethod(method);
      return true;
    } catch (final IllegalArgumentException e) {
      return false;
    }
  }

  public static void checkServiceInterface(final Class<?> iClass) {
    if (iClass.isInterface()) {
      for (final Method method : iClass.getMethods()) {// all methods - not only declared
        checkServiceMethod(method);
      }
    } else {
      throw new IllegalArgumentException("Service insterace must be an insterface! Found class: " + iClass.getName() + "!");
    }
  }

  public static boolean isServiceInterface(final Class<?> iClass) {
    try {
      checkServiceInterface(iClass);
      return true;
    } catch (final IllegalArgumentException e) {
      return false;
    }
  }

  public static String serviceName(final Class<?> iClass) {
    final Protocol proto = protocol(iClass);
    return proto.getNamespace() == null || proto.getNamespace().length() == 0 ? proto.getName() : proto.getNamespace() + "." + proto.getName();
  }

  public static boolean isStream(final Method method) {
    return method.getReturnType() == void.class || method.getReturnType() == Void.class;
  }

  public static String methodName(final Method method) {
    final List<Method> sameNameMethos = Reflect.findByName(method.getDeclaringClass(), method.getName()).collect(Collectors.toList());;
    if (sameNameMethos.size() == 1) {
      return method.getName();
    } else {
      Collections.sort(sameNameMethos, Reflect.METHOD_COMPARATOR);
      return method.getName() + '_' + sameNameMethos.indexOf(method);
    }
  }

  private static final Map<Class<?>, Protocol> A_PROTOS = new ConcurrentHashMap<>();
  public static Protocol protocol(final Class<?> iClass) {
    return A_PROTOS.computeIfAbsent(iClass, Proto::protocol0);
  }
  private static Protocol protocol0(final Class<?> iClass) {
    if (!isServiceInterface(iClass)) {
      throw new IllegalArgumentException("The class " + iClass.getName() + " doesn't comply with service requirements!");
    }

    final Service serviceAnn = iClass.getAnnotation(Service.class);
    final String namespace;
    final String name;
    if (serviceAnn == null) {
       final String serviceName = iClass.getName();
       final int index = serviceName.lastIndexOf('.');
       namespace = index == -1 ? "" : serviceName.substring(0, index);
       name = serviceName.substring(index + 1);
    } else {
      final String serviceName = serviceAnn.name().length() == 0 ? iClass.getName() : serviceAnn.name();
      final int index = serviceName.lastIndexOf('.');
      namespace = index == -1 ? "" : serviceName.substring(0, index);
      name = serviceName.substring(index + 1) + (serviceAnn.version().length() == 0 ? "" : '_'+ serviceAnn.version());
    }
    final Protocol protocol = new Protocol(name, namespace);

    final Collection<Schema> types = new ArrayList<>();
    types.add(RPCException.RPC_EXCEPTION_SCHEMA);
    Arrays.stream(iClass.getMethods()) // all methods - not only declared
      .collect(Collectors.toMap(Reflect::methodSig, Function.identity()))
      .values()
      .forEach(method -> {
        final String methodName = methodName(method);
        protocol.getMessages().put(
          methodName,
          protocol.createMessage(
            methodName, null,
            Collections.emptyMap(),
            requestSchema(protocol.getNamespace(), methodName, method, types),
            responseSchema(method, types),
            Schema.createUnion(Schema.create(Schema.Type.STRING), RPCException.RPC_EXCEPTION_SCHEMA)));
      });
    protocol.setTypes(types);
    return protocol;
  }

  private static final EncoderFactory ENCODER_FACTORY = new EncoderFactory();
  private static final DecoderFactory DECODER_FACTORY = new DecoderFactory();
  private static final Map<Tuple2<Schema, Schema>, Schema> JSON_RESSP_CACHE = new ConcurrentHashMap<>();
  private static Schema jsonRespScehma(final Schema respSchema, final Schema errorSchema ) {
    return JSON_RESSP_CACHE.computeIfAbsent(new Tuple2<>(respSchema, errorSchema), t2 -> {
      final RecordBuilder<Schema> rTypeBuilder = SchemaBuilder.record("Responce");
      rTypeBuilder.namespace("am24j.rpc.avro"); // empty namespace is treated as null
      final FieldAssembler<Schema> fAssembler = rTypeBuilder.fields();
      fAssembler
        .name("value")
        .type(respSchema.getType() == Schema.Type.NULL ? respSchema : SchemaBuilder.nullable().type(respSchema))
        .noDefault();
      fAssembler
        .name("error")
        .type(SchemaBuilder.nullable().type(errorSchema.getTypes().get(1)))
        .noDefault();
      return fAssembler.endRecord();
    });
  }
  public static byte[] encodeReqy(final Schema reqSchema, final Type[] types, final Object[] args, final boolean json) {
    try (final ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
      final Encoder encoder = json ?
        ENCODER_FACTORY.jsonEncoder(reqSchema, baos, true) :
        ENCODER_FACTORY.binaryEncoder(baos, null);
      final List<Schema.Field> params = reqSchema.getFields();
      if (args != null) {
        if (args.length != params.size()) {
          throw new IllegalArgumentException("Invalid args size! Expect " + params.size() + ", found " + args.length + "!");
        }
        for (int i = 0; i < args.length; i++) {
          Avro.write(args[i], params.get(i).schema(), types[i], encoder);
        }
      }
      encoder.flush();
      return baos.toByteArray();
    } catch (final IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static Object[] decodeReq(final Schema reqSchema, final Type[] types, final InputStream is, final boolean json) {
    try {
      final Decoder deooder = json ?
        DECODER_FACTORY.jsonDecoder(reqSchema, is) :
        DECODER_FACTORY.binaryDecoder(is, null);
      final Record request = (Record)new SpecificDatumReader<>(reqSchema).read(null, deooder);
      final List<Schema.Field> params = reqSchema.getFields();
      final Object[] args = new Object[params.size()];
      for (int i = 0; i < args.length; i++) {
        args[i] = Avro.unwrap(request.get(params.get(i).name()), types[i]);
      }
      return args;
    } catch (final IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static byte[] encodeResp(final Schema respSchema, final Schema errorSchema, final Type type, final Object resp, final boolean json) {
    try (final ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
      if (json) {
        return encodeReqy(
          jsonRespScehma(respSchema, errorSchema),
          new Type[] {type, RPCException.class},
          new Object[] {resp instanceof Exception ? null : resp, resp instanceof Exception ? resp : null},
          json);
      } else {
        Encoder encoder = ENCODER_FACTORY.binaryEncoder(baos, null);

        if (resp instanceof Exception) {
          try {
            encoder.writeBoolean(true);
            new SpecificDatumWriter<>(errorSchema).write(resp, encoder);
          } catch (final Throwable t) {
            baos.reset();
            encoder = json ?
              ENCODER_FACTORY.jsonEncoder(respSchema, baos) :
              ENCODER_FACTORY.binaryEncoder(baos, null);
            encoder.writeBoolean(true);
            new SpecificDatumWriter<>(Protocol.SYSTEM_ERRORS).write(new Utf8(t.toString()), encoder);
          }
        } else {
          encoder.writeBoolean(false);
          Avro.write(resp, respSchema, type, encoder);
        }
        encoder.flush();
      }

      return baos.toByteArray();
    } catch (final IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static Object decodeResp(final Schema respSchema, final Schema errorSchema, final Type type, final InputStream is, final boolean json) {
    try {
      if (json) {
        final Object[] valueAndError =
          decodeReq(
            jsonRespScehma(respSchema, errorSchema),
            new Type[] {type, RPCException.class},
            is,
            true);
        return valueAndError[1] == null ? valueAndError[0] : valueAndError[1];
      } else {
        final Decoder deooder = DECODER_FACTORY.binaryDecoder(is, null);
        if (deooder.readBoolean()) {
          final Object value = new SpecificDatumReader<>(errorSchema).read(null, deooder);
          if (value instanceof Exception) {
            return value;
          }
          return new am24j.rpc.RPCException(null, value.toString(), null);
        } else {
          return Avro.read(respSchema, type, deooder);
        }
      }
    } catch (final IOException e) {
      throw new RuntimeException();
    }
  }

  public static Type[] requestTypes(final Method method) {
    Type[] types = method.getGenericParameterTypes();
    if (types.length > 0 && method.getParameterTypes()[types.length - 1] == Subscriber.class) {
      final Type[] cutLast = new Type[types.length - 1];
      System.arraycopy(types, 0, cutLast, 0, cutLast.length);
      types = cutLast;
    }
    return types;
  }

  public static Type responsType(final Method method) {
    final Type type = method.getGenericReturnType();
    Type realType = type;
    if (type instanceof ParameterizedType) {
      final ParameterizedType pType = (ParameterizedType)type;
      final Type rawType = pType.getRawType();
      if (rawType == CompletionStage.class || rawType == CompletableFuture.class) {
        realType = ((ParameterizedType)type).getActualTypeArguments()[0];
      }
    }
    if (realType == void.class || realType == Void.class) {
      final Type[] paramTypes = method.getGenericParameterTypes();
      if (paramTypes.length == 0 || method.getParameterTypes()[paramTypes.length - 1] != Subscriber.class) {
        return null;
      } else {
        realType = ((ParameterizedType)paramTypes[paramTypes.length - 1]).getActualTypeArguments()[0];
      }
    }
    return realType;
  }

  private static Schema requestSchema(final String namespace, final String name, final Method method, final Collection<Schema> protcolTypes) {
    final RecordBuilder<Schema> rTypeBuilder = SchemaBuilder.record(name + "_Req");
    rTypeBuilder.namespace(namespace); // empty namesoace is treated as null
    final FieldAssembler<Schema> fAssembler = rTypeBuilder.fields();
    Type[] types = requestTypes(method);
    for (int i = 0; i < types.length; i++) {
      final Type propType = types[i];
      final Schema schema = Avro.forType(propType);
      fAssembler
        .name("arg_" + i)
        .type(schema)
        .noDefault();
      addType(schema, protcolTypes);
    }
    return fAssembler.endRecord();
  }

  private static Schema responseSchema(final Method method, final Collection<Schema> protcolTypes) {
    final Type returnType = responsType(method);
    final Schema schema = returnType == null ? Schema.create(Schema.Type.NULL) : Avro.forType(returnType);
    addType(schema, protcolTypes);
    return schema;
  }

  private static void addType(final Schema schema, final Collection<Schema> protcolTypes) {
    if ((schema.getType() == org.apache.avro.Schema.Type.RECORD ||
        schema.getType() == org.apache.avro.Schema.Type.FIXED ||
        schema.getType() == org.apache.avro.Schema.Type.ENUM)
        && !protcolTypes.contains(schema)) {
      protcolTypes.add(schema);
    }
  }
  /**
  private static final InputStream EMPTY = new InputStream() {

    @Override
    public int read() throws IOException {
      return -1;
    }
  };
  @SuppressWarnings("rawtypes")
  private static final Marshaller NOP = new Marshaller() {

    @Override
    public InputStream stream(final Object value) {
      if (value != null) throw new IllegalArgumentException("NOP marshaller supports only void (i.e. null object is expected):! Called with: " + value + "!");
      return EMPTY;
    }

    @Override
    public Object parse(final InputStream stream) {
      try {
        if (stream.read() != -1) throw new IllegalArgumentException("NOP marshaller supports only void (i.e. empty steam is expected)!");
      } catch (final IOException e) {
        throw RE.toRuntime(e);
      }
      return null;
    }
  };*/
}
