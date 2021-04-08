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
package am24j.rpc.grpc;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

import org.apache.avro.Protocol;
import org.apache.avro.Protocol.Message;
import org.apache.avro.Schema;
import org.slf4j.Logger;

import am24j.commons.Ctx;
import am24j.rpc.avro.Proto;
import io.grpc.Metadata;
import io.grpc.Metadata.Key;
import io.grpc.MethodDescriptor;
import io.grpc.MethodDescriptor.Marshaller;
import io.grpc.MethodDescriptor.MethodType;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 * Commons
 *
 * @author avgustinmm
 */
public class Common {

  public static final Key<String> WWW_AUTHENTICATE = Key.of("WWW-Authenticate", Metadata.ASCII_STRING_MARSHALLER);

  private static final Logger LOG = Ctx.logger("rpc.grpc.commons");

  private Common() {}

  public static MethodDescriptor<Object[], Object> methodDescriptor(final Method method, final Protocol aProto) {
    final Message aMessage = aProto.getMessages().get(Proto.methodName(method));
    return MethodDescriptor.<Object[], Object>newBuilder()
      .setType(Proto.isStream(method) ? MethodType.SERVER_STREAMING : MethodType.UNARY)
      .setFullMethodName(MethodDescriptor.generateFullMethodName(aProto.getName(), aMessage.getName()))
      .setRequestMarshaller(new ReqMarshaller(aMessage.getRequest(), Proto.requestTypes(method)))
      .setResponseMarshaller(new RespMarshaller(aMessage.getResponse(), aMessage.getErrors(), Proto.responsType(method)))
      .build();
  }

  private static class ReqMarshaller implements Marshaller<Object[]> {

    private final Schema reqSchema;
    private final Type[] types;

    private ReqMarshaller(final Schema reqSchema, final Type[] types) {
      this.reqSchema = reqSchema;
      this.types = types;
    }

    @Override
    public InputStream stream(final Object[] args) {
      try {
        return new ByteArrayInputStream(Proto.encodeReqy(reqSchema, types, args, false));
      } catch (final RuntimeException | Error e) {
        LOG.error("Failed to stream request: {}!", args, e);
        throw e;
      }
    }

    @Override
    public Object[] parse(final InputStream is) {
      try {
        return Proto.decodeReq(reqSchema, types, is, false);
      } catch (final RuntimeException | Error e) {
        LOG.error("Failed to pars request!", e);
        throw e;
      }
    }

    @Override
    public String toString() {
      return "Request: " + new JsonObject(reqSchema.toString()).encodePrettily();
    }
  }

  private static class RespMarshaller implements Marshaller<Object> {

    private final Schema respSchema;
    private final Schema errorSchema;
    private final Type type;

    private RespMarshaller(final Schema respSchema, final Schema errorSchema, final Type type) {
      this.respSchema = respSchema;
      this.errorSchema = errorSchema;
      this.type = type;
    }

    @Override
    public InputStream stream(final Object resp) {
      try {
        return new ByteArrayInputStream(Proto.encodeResp(respSchema, errorSchema, type, resp, false));
      } catch (final RuntimeException | Error e) {
        LOG.error("Failed to stream response: {}!", resp, e);
        throw e;
      }
    }

    @Override
    public Object parse(final InputStream is) {
      try {
        return Proto.decodeResp(respSchema, errorSchema, type, is, false);
      } catch (final RuntimeException | Error e) {
        LOG.error("Failed to parse response!!", e);
        throw e;
      }
    }

    @Override
    public String toString() {
      return "Response:: " + new JsonObject(respSchema.toString()).encodePrettily() + "\nError: " + new JsonArray(errorSchema.toString()).encodePrettily();
    }
  }
}