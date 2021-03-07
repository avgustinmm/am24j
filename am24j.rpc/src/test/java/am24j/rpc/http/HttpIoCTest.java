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
package am24j.rpc.http;

import org.apache.logging.log4j.Level;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import am24j.commons.Log4j2Config;
import am24j.inject.Injector.Key;
import am24j.inject.Starter;
import am24j.rpc.BaseTest;
import am24j.rpc.IService;
import am24j.rpc.ServiceImpl;
import am24j.rpc.http.HttpTest.TestAuthVerfier;
import am24j.rt.config.Config;
import am24j.vertx.VertxInstance;
import am24j.vertx.http.Http;

/**
 * Tests build using inversion of control (with DI) app composition
 *
 * @author avgustinmm
 */
public class HttpIoCTest extends BaseTest {

  static {
    Log4j2Config.setUp(Level.INFO, Level.TRACE, "am24j.rcp.http");
  }

  private static Starter sStarter;
  private static Starter cStarter;

  @BeforeClass
  public static void before() {
    sStarter = Starter.start(
      Config.class,
      // vertx
      VertxInstance.class,
      // service
      ServiceImpl.class,
      // with verfiers
      TestAuthVerfier.class,
      // Http rpc server - wheb services are registered
      Server.class,
      Http.class);
    cStarter = Starter.start(
      Config.class,
      // vertx
      VertxInstance.class);
    service = cStarter.injector().<Client>getInstance(Key.of(Client.class)).service(() -> "user:pass", IService.class);
  }

  @AfterClass
  public static void after() throws Exception {
    cStarter.close();
    sStarter.close();
  }
}
