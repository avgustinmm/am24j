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

import org.apache.avro.Schema;
import org.apache.avro.SchemaBuilder;
import org.apache.avro.SchemaBuilder.FieldAssembler;
import org.apache.avro.SchemaBuilder.RecordBuilder;
import org.apache.avro.generic.IndexedRecord;

import am24j.avro.Avro;

/**
 * An exception that could be serialized by avro
 *
 * @author avgustinmm
 */
public class RPCException extends RuntimeException implements IndexedRecord {

  public static final Schema RPC_EXCEPTION_SCHEMA;
  static {
    final RecordBuilder<Schema> rTypeBuilder = SchemaBuilder.record("RPCException");
    rTypeBuilder.namespace("am24j.rpc.avro"); // empty namespace is treated as null
    final FieldAssembler<Schema> fAssembler = rTypeBuilder.fields();
    fAssembler
      .name("uuid")
      .type(Avro.forTypeNullable(String.class))
      .noDefault();
    fAssembler
      .name("message")
      .type(Avro.forTypeNullable(String.class))
      .noDefault();
    fAssembler
      .name("realExcClass")
      .type(Avro.forTypeNullable(String.class))
      .noDefault();
    RPC_EXCEPTION_SCHEMA = fAssembler.endRecord();
  }

  private static final long serialVersionUID = -7158994162852004773L;

  private String uuid;
  private String message;
  private String type;

  public RPCException() {}

  public Throwable toRPC() {
    final am24j.rpc.RPCException rpcExc = new am24j.rpc.RPCException(uuid, message, type);
    try {
      Exception real = null;
      final Class<?> clazz = Class.forName(type);
      if (message == null) {
        try {
          real = (Exception)clazz.getConstructor().newInstance();
        } catch (final Exception e) {}
      }
      real = (Exception)clazz.getConstructor(String.class).newInstance(message);
      real.addSuppressed(rpcExc);
      return real;
    } catch (final Throwable t) {
      return rpcExc;
    }
  }

  public String getUUID() {
    return uuid;
  }

  @Override
  public String getMessage() {
    return message;
  }

  public String getType() {
    return type;
  }

  public RPCException setUUID(final String uuid) {
    this.uuid = uuid;
    return this;
  }

  public RPCException setMessage(final String message) {
    this.message = message;
    return this;
  }

  public RPCException setType(final String type) {
    this.type = type;
    return this;
  }

  @Override
  public String toString() {
    return "[" + uuid + "] RPCException: " + getMessage() + " of type " + type + "!";
  }

  @Override
  public void put(final int i, final Object v) {
    if (i == 0) {
      uuid = (String)v;
    } else if (i == 1) {
      message = (String)v;
    } else if (i == 2) {
      type = (String)v;
    } else {
      throw new ArrayIndexOutOfBoundsException("i = " + i + "!") ;
    }
  }

  @Override
  public Object get(final int i) {
    if (i == 0) {
      return uuid;
    } else if (i == 1) {
      return message;
    } else if (i == 2) {
      return type;
    } else {
      throw new ArrayIndexOutOfBoundsException("i = " + i + "!") ;
    }
  }

  @Override
  public Schema getSchema() {
    return RPC_EXCEPTION_SCHEMA;
  }
}