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
package am24j.jcl;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * lass loader that provides class kept in internal jars
 * 
 * @author avgustinmm
 */
public class JarClassLoader extends URLClassLoader {

  // it is essntial that it has sintle leter before 'jar'
  private static final String INTERNAL_JAR_PROTOCOL = "ijar";

  static {
    // since jar url handler doesn't support internal jars we fake it - 
    // wrap it in ijar (internal jar) protocol hander and thus
    // ijar provides access to internal jar (as regular url handler) 
    // while jar provides access to the internals of that internal jar (when used by URLClassLoader) ..
    // url is similar to jar's but * (unexpected symbol) is used as replacement of ! from the jar url 
    URL.setURLStreamHandlerFactory(new URLStreamHandlerFactory() {
      
      private final URLStreamHandler handler = new URLStreamHandler() {

        @Override
        protected URLConnection openConnection(final URL u) throws IOException {
          return new URL(u.toString().substring(1).replace("*/", "!/")).openConnection();
        }
      };
    
      @Override
      public URLStreamHandler createURLStreamHandler(final String protocol) {
        if (INTERNAL_JAR_PROTOCOL.equals(protocol)) {
          return handler;
        }
        return null;
      }
    });
  }
 
  public JarClassLoader(final ClassLoader parent) {
    super(urls(), parent);
  }

  private final ConcurrentHashMap<URL, ConcurrentHashMap<String, Boolean>> usage = new ConcurrentHashMap<>();
  
  @Override
  protected Package definePackage(final String name, final Manifest man, final URL url) throws IllegalArgumentException {
    usage.computeIfAbsent(url, u -> new ConcurrentHashMap<>()).put(name, Boolean.TRUE);
    return super.definePackage(name, man, url);
  }
  
  @Override
  public String toString() {
    final URL[] urls = getURLs();
    final StringBuilder sb = new StringBuilder();
    
    final List<String> used = new ArrayList<>();
    final List<String> unused = new ArrayList<>();
    for (final URL url : urls) {
      (usage.get(url) == null ? unused : used).add(url.toString());
    }
    Collections.sort(used);
    Collections.sort(unused);

    sb.append("UTLs [").append(urls.length).append("]:\n");
    sb.append("Used [").append(used.size()).append("]:\n");
    for (final String url : used) {
      sb.append("  ").append(url).append(" (size: ").append(SIZES.get(url)).append(")\n");
    }
    sb.append("Unused [").append(unused.size()).append("]:\n");
    for (final String url : unused) {
      sb.append("  ").append(url).append(" (size: ").append(SIZES.get(url)).append(")\n");
    }
    return sb.toString();
  }

  private static final String CLASS_PATH="java.class.path";
  private static final ConcurrentHashMap<String, Long> SIZES = new ConcurrentHashMap<>();
  private static URL[] urls() {
    return Arrays
      .asList(System.getProperty(CLASS_PATH).split(File.pathSeparator))
      .stream()
      .filter(element -> element.toLowerCase().endsWith(".jar"))
      .flatMap(jar -> {
        try {
          try (final JarFile jarFile = new JarFile(new File(jar))) {
            return jarFile
              .stream()
              .filter(name -> name.getName().toLowerCase().endsWith(".jar"))
              .map(jarEntry -> {
                try {
                  // TODO - it's always, add suppor for urls - e.g. http ...
                  final URL url = new URL(INTERNAL_JAR_PROTOCOL + ":" + (jar.contains("://") ? jar : "file:" + jar.replace('\\', '/')) + "*/" + jarEntry.getName()); // replaxes \ (windows path) with / (alsow works and is valid URI character)
                  SIZES.put(url.toString(), jarEntry.getCompressedSize());
                  return url;
                } catch (final MalformedURLException e) {
                  throw new RuntimeException(e);
                }
              }).collect(Collectors.toList()).stream(); // process here - after that steam is closed
          }
        } catch (final IOException | RuntimeException e) {
          System.err.println("Failed to load from " + jar + "!");
          e.printStackTrace();
          return Stream.empty();
        }
      }).collect(Collectors.toList()).toArray(new URL[0]);
  }
}