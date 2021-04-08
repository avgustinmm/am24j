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

import java.io.ByteArrayInputStream;
import java.lang.reflect.Type;

import org.apache.avro.Protocol.Message;
import org.junit.Assert;
import org.junit.Test;

import am24j.rpc.IService;

/**
 * Avro encoding tests
 *
 * @author avgustinmm
 */
public class EncodingTest {

  private static final Message MESSAGE;
  static {
    try {
      MESSAGE =
        Proto.protocol(IService.class)
          .getMessages()
          .get(Proto.methodName(IService.class.getMethod("getCall", int.class, String.class)));
    } catch (final Exception e) {
      throw new IllegalStateException(e);
    }
  }

  @Test
  public void testBinReq() throws Exception {
    testReq(false);
  }

  @Test
  public void testJsonReq() throws Exception {
    testReq(true);
  }

  @Test
  public void testBinResp() throws Exception {
    testResp(false);
  }

  @Test
  public void testJsonResp() throws Exception {
    testResp(true);
  }

  @Test
  public void testJsonErrResp() throws Exception {
    final RPCException exc = new RPCException().setUUID("uuid").setMessage("meg").setType("AType");
    final byte[] encoded = Proto.encodeResp(MESSAGE.getResponse(), MESSAGE.getErrors(), String.class, exc, true);
    final Object decoded = Proto.decodeResp(MESSAGE.getResponse(), MESSAGE.getErrors(),  String.class, new ByteArrayInputStream(encoded), true);
    Assert.assertEquals("Encode / decode - correct typey", RPCException.class, decoded.getClass());
    Assert.assertEquals("Encode / decode - identity", exc.toString(), decoded.toString());
  }

  private void testReq(final boolean json) throws Exception {
    final Object[] args = new Object[] {5, "test"};
    final byte[] encoded = Proto.encodeReqy(MESSAGE.getRequest(), new Type[] {int.class, String.class}, args, json);
    final Object[] decoded = Proto.decodeReq(MESSAGE.getRequest(), new Type[] {int.class, String.class}, new ByteArrayInputStream(encoded), json);
    Assert.assertArrayEquals("Encode / decode - identity", args, decoded);
  }

  private void testResp(final boolean json) throws Exception {
    final Object resp = "test";
    final byte[] encoded = Proto.encodeResp(MESSAGE.getResponse(), MESSAGE.getErrors(), String.class, resp, json);
    final Object decoded = Proto.decodeResp(MESSAGE.getResponse(), MESSAGE.getErrors(),  String.class, new ByteArrayInputStream(encoded), json);
    Assert.assertEquals("Encode / decode - identity", resp, decoded);
  }
}
