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
package am24j.example.services;

import javax.inject.Inject;
import javax.inject.Singleton;

import am24j.vertx.http.Http;
import io.vertx.core.http.HttpServerRequest;

/**
 * @author avgustinmm
 */
@Singleton
public class HelloWorld implements Http.HttpHandler {

  @Inject
  public HelloWorld() {}

  @Override
  public String path() {
    return "/hello";
  }

  @Override
  public void handle(final HttpServerRequest request) {
    request.response().setStatusCode(200).end("Hellow World!");
  }
}
