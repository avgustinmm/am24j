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

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;

import am24j.commons.Reflect;
import am24j.test.Gen;
import io.grpc.MethodDescriptor;
import io.grpc.MethodDescriptor.Marshaller;

public class MarshallerTest {

  @Test
  public void marashaler() {
    Arrays.stream(IService.class.getMethods()).forEach(method -> {
      final MethodDescriptor<Object[], Object> md = Common.methodDescriptor(method);
      final Marshaller<Object[]> reqMarshaller = md.getRequestMarshaller();
      final Object[] req = Gen.gen((Type[])Reflect.call(null, Reflect.method(Common.class, "requestTypes", Method.class), method));
      final Object[] reqPT = reqMarshaller.parse(reqMarshaller.stream(req));
      Assert.assertArrayEquals("Request passed through correctly", req, reqPT);
      final Marshaller<Object> respMarshaller = md.getResponseMarshaller();
      final Object resp = Gen.gen((Class<?>)Reflect.call(null, Reflect.method(Common.class, "responsType", Method.class), method));
      final Object respPT = respMarshaller.parse(respMarshaller.stream(resp));
      Assert.assertEquals("Request passed through correctly", resp, respPT);      
    });
  }
}
