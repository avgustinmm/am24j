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

import com.am2.rt.vertx.http.Http;
import com.am2.rt.vertx.http.RestEasy;

import am24j.example.services.HelloWorld;
import am24j.example.services.JAXRS1;
import am24j.example.services.JAXRS2;
import am24j.rt.Starter;
import am24j.rt.config.Config;
import am24j.vertx.HZCluster;
import am24j.vertx.Instance;
import am24j.vertx.Shell;

/**
 * @author avgustinmm
 */
public class App {
  
  public static void main(final String[] args) throws Exception {
    Starter.start(new Class<?>[] {
      Config.class,
      
      am24j.rt.hz.Instance.class,      
      HZCluster.class,
      
      Instance.class,
      
      Shell.class,
      
      JAXRS1.class,
      JAXRS2.class,
      RestEasy.class,
      
      HelloWorld.class,
      
      Http.class
    });
  }
}
