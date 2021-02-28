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
package am24j.example;

import am24j.example.services.HelloWorld;
import am24j.example.services.JAXRS1;
import am24j.example.services.JAXRS2;
import am24j.inject.Starter;
import am24j.rt.config.Config;
import am24j.vertx.HZCluster;
import am24j.vertx.Shell;
import am24j.vertx.VertxInstance;
import am24j.vertx.http.Http;
import am24j.vertx.http.RestEasy;

/**
 * @author avgustinmm
 */
public class App {

  public static void main(final String[] args) throws Exception {
    Starter.start(new Class<?>[] {
      Config.class,

      am24j.rt.hz.HZInstance.class,
      HZCluster.class,

      VertxInstance.class,

      Shell.class,

      JAXRS1.class,
      JAXRS2.class,
      RestEasy.class,

      HelloWorld.class,

      Http.class
    });
    System.out.println("Could get:");
    System.out.println("  http://localhost:1080/hello (simple hanling)");
    System.out.println("  http://localhost:1080/jaxrs/1/hello (JAX-RS service)");
    System.out.println("  http://localhost:1080/jaxrs/2/hello (JAX-RS service)");
  }
}
