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

import org.apache.avro.Protocol;
import org.apache.avro.Protocol.Message;
import org.apache.avro.Schema;

import am24j.rpc.avro.Proto;
import io.grpc.Metadata;
import io.grpc.Metadata.Key;
import io.grpc.MethodDescriptor;
import io.grpc.MethodDescriptor.Marshaller;
import io.grpc.MethodDescriptor.MethodType;

/**
 * Commons
 *
 * @author avgustinmm
 */
class Common {

  public static final Key<String> WWW_AUTHENTICATE = Key.of("WWW-Authenticate", Metadata.ASCII_STRING_MARSHALLER);

  public static MethodDescriptor<Object[], Object> methodDescriptor(final Method method, final Protocol aProto) {
    final Message aMessage = aProto.getMessages().get(Proto.methodName(method));
    return MethodDescriptor.<Object[], Object>newBuilder()
      .setType(Proto.isStream(method) ? MethodType.SERVER_STREAMING : MethodType.UNARY)
      .setFullMethodName(MethodDescriptor.generateFullMethodName(aProto.getName(), aMessage.getName()))
      .setRequestMarshaller(new ReqMarshaller(aMessage.getRequest()))
      .setResponseMarshaller(new RespMarshaller(aMessage.getResponse(), aMessage.getErrors()))
      .build();
  }

  private static class ReqMarshaller implements Marshaller<Object[]> {

    private final Schema reqSchema;

    private ReqMarshaller(final Schema reqSchema) {
      this.reqSchema = reqSchema;
    }

    @Override
    public InputStream stream(final Object[] args) {
      return new ByteArrayInputStream(Proto.encodeReqy(reqSchema, args, false));
    }

    @Override
    public Object[] parse(final InputStream is) {
      return Proto.decodeReq(reqSchema, is, false);
    }
  }

  private static class RespMarshaller implements Marshaller<Object> {

    private final Schema respSchema;
    private final Schema errorSchema;

    private RespMarshaller(final Schema respSchema, final Schema errorSchema) {
      this.respSchema = respSchema;
      this.errorSchema = errorSchema;
    }

    @Override
    public InputStream stream(final Object resp) {
      return new ByteArrayInputStream(Proto.encodeResp(respSchema, errorSchema, resp, false));
    }

    @Override
    public Object parse(final InputStream is) {
      return Proto.decodeResp(respSchema, errorSchema, is, false);
    }
  }
}