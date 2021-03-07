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
package am24j.example.hellowold;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import am24j.rpc.Remote;

@Remote
public class RPCImpl implements RPCInterface {

  @Override
  public CompletionStage<String> hello(final String name) {
    return CompletableFuture.completedStage("Hello " + (name == null || name.trim().length() == 0 ? "World" : name) + "! (by RPC)");
  }
}
