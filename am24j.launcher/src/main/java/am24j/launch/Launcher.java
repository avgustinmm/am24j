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
package am24j.launch;

import am24j.jcl.JarClassLoader;

/**
 * Launches applications with class loader having access to internal jars
 *
 * @author avgustinmm
 */
public class Launcher {

  private static final JarClassLoader JAR_CLASS_LOADER = new JarClassLoader(Launcher.class.getClassLoader());

  public static void main(final String[] args) throws Exception {
    if (args.length == 0) {
      System.err.println("No man classes passed!");
    } else {
      for (final String arg : args) {
        new Thread(() -> {
          System.out.println("Start -> " + arg);
          try {
            JAR_CLASS_LOADER.loadClass(arg).getMethod("main", String[].class).invoke(null, (Object)new String[0]);
          } catch (final Throwable t) {
            System.err.println("Failed to start " + arg + " class!");
            t.printStackTrace();
          }
        }, "Runner-" + arg).start();
      }
    }
  }

  public static JarClassLoader jarClassLoader() {
    return JAR_CLASS_LOADER;
  }
}
