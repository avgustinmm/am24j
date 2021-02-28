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
package am24j.rpc;

import java.util.UUID;

/**
 * RPC exception
 *
 * @author avgustinmm
 */
@SuppressWarnings("serial")
public class RPCException extends RuntimeException {

  private final String uuid;
  private final String type;

  public RPCException(final String uuid, final String message, final String type) {
    super(message, null, false, false);
    this.uuid = uuid == null ? uuid() : uuid;
    this.type = type;
  }

  public String getUUID() {
    return uuid;
  }

  public String getType() {
    return type;
  }

  @Override
  public String toString() {
    return "[" + uuid + "] RPCException: " + getMessage() + " of type " + type + "!";
  }

  private static final String PREFIX = UUID.randomUUID().toString() + "-";
  private static int counter;
  public static synchronized String uuid() {
    return PREFIX + counter++;
  }
}
