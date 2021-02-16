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

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Flow.Subscriber;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.avro.Protocol;
import org.apache.avro.Schema;
import org.apache.avro.SchemaBuilder;
import org.apache.avro.SchemaBuilder.FieldAssembler;
import org.apache.avro.SchemaBuilder.RecordBuilder;
import org.apache.avro.grpc.AvroRequestMarshaller;
import org.apache.avro.grpc.AvroResponseMarshaller;

import am24j.avro.Avro;
import am24j.commons.RE;
import am24j.commons.Reflect;
import am24j.rpc.Service;
import io.grpc.MethodDescriptor;
import io.grpc.MethodDescriptor.Marshaller;
import io.grpc.MethodDescriptor.MethodType;

class Common {

  public static final Predicate<Method> IS_SERVICE_METHOD = method -> {
    if (method.getReturnType() == void.class || method.getReturnType() == Void.class) {
      final Class<?>[] parameterTypes = method.getParameterTypes();
      if (parameterTypes.length == 0 || parameterTypes[parameterTypes.length - 1] != Subscriber.class || !(method.getGenericParameterTypes()[parameterTypes.length - 1] instanceof ParameterizedType)) {
        return false;
      }
    } else {
      final Class<?> returnType = method.getReturnType();
      if (returnType != CompletionStage.class && returnType != CompletableFuture.class) {
        return false;
      }
    }
    return true;
  };

  static String serviceName(final Class<?> iClass) {
    final Service serviceAnn = iClass.getAnnotation(Service.class);
    final String serviceName = 
      (serviceAnn.serviceName().length() == 0 ? iClass.getName() : serviceAnn.serviceName()) +
      (serviceAnn.version().length() == 0 ? "" : '_'+ serviceAnn.version());
    return serviceName;
  }
  
  static MethodType methodType(final Method method) {
    if (method.getReturnType() == void.class || method.getReturnType() == Void.class) {
      return MethodType.SERVER_STREAMING;
    } else {
      return MethodType.UNARY;
    }
  }
  
  static String methodName(final Method method) {
    final List<Method> sameNameMethos = Reflect.findByName(method.getDeclaringClass(), method.getName()).collect(Collectors.toList());;
    if (sameNameMethos.size() == 1) {
      return method.getName();
    } else {
      Collections.sort(sameNameMethos, Reflect.METHOD_COMPARATOR);
      return method.getName() + '_' + sameNameMethos.indexOf(method);
    }
  }
  
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
  };

  private static final Map<Method, MethodDescriptor<Object[], Object>> CACHE = new ConcurrentHashMap<>();
  static MethodDescriptor<Object[], Object> methodDescriptor(final Method method) {
    return CACHE.computeIfAbsent(method, Common::methodDescriptor0);
  }
  @SuppressWarnings("unchecked")
  private static MethodDescriptor<Object[], Object> methodDescriptor0(final Method method) {
    final Protocol aProto = new Protocol(serviceName(method.getDeclaringClass()), "am24j");
    final String methodName = methodName(method);
    final Protocol.Message pMessage = 
      aProto.createMessage(
        methodName, "Auto generated AM24J message", 
        Collections.emptyMap(), 
        requestSchema(aProto.getNamespace(), methodName, method), 
        responseSchema(method), null);
    return MethodDescriptor.<Object[], Object>newBuilder()
      .setType(methodType(method))
      .setFullMethodName(MethodDescriptor.generateFullMethodName(aProto.getName(), Common.methodName(method)))
      .setRequestMarshaller(new AvroRequestMarshaller(pMessage))
      .setResponseMarshaller(pMessage.getResponse() == null ? NOP : new AvroResponseMarshaller(pMessage))
      .build();
  }
  
  private static Schema requestSchema(final String namespace, final String name, final Method method) {
    final RecordBuilder<Schema> rTypeBuilder = SchemaBuilder.record(name + "_Req");
    rTypeBuilder.namespace(namespace); // empty namespace is treated as null
    final FieldAssembler<Schema> fAssembler = rTypeBuilder.fields();
    Type[] types = requestTypes(method);
    for (int i = 0; i < types.length; i++) {
      final Type propType = types[i];
      fAssembler
        .name("arg" + i)
        .type(Avro.forType(propType))
        .noDefault();
    }
    return fAssembler.endRecord();
  }
  
  private static Schema responseSchema(final Method method) {
    final Type returnType = responsType(method);
    return returnType == null ? null : Avro.forType(returnType);
  }

  private static Type[] requestTypes(final Method method) {
    Type[] types = method.getGenericParameterTypes();
    if (types.length > 0 && method.getParameterTypes()[types.length - 1] == Subscriber.class) {
      final Type[] cutLast = new Type[types.length - 1];
      System.arraycopy(types, 0, cutLast, 0, cutLast.length);
      types = cutLast;
    }
    return types;
  }
  
  private static Type responsType(final Method method) {
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
}
