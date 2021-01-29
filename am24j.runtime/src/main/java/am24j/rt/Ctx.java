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
package am24j.rt;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides access to: <br>
 * <ul>
 *   <li>External configuration - environment properties and system properties</li>
 *   <li>Call context - attach context to an execution</li>
 *   <li>Central place to access file system</li>
 * </ul
 * 
 * @author avgustinmm
 */
public class Ctx {
  
  // default namesace, also used for set up a custom namespace
  public static final String DEFAULT_NAMESPACE_ENV = "AM24J";
  public static final String DEFAULT_NAMESPACE_SYS = "am24j";

  public static final String NAMESPACE_ENV = get(DEFAULT_NAMESPACE_ENV + "_NAMESPACE_ENV", DEFAULT_NAMESPACE_SYS + ".namespace.env").orElse(DEFAULT_NAMESPACE_ENV);
  public static final String NAMESPACE_SYS = get(DEFAULT_NAMESPACE_ENV + "_NAMESPACE_SYS", DEFAULT_NAMESPACE_SYS + ".namespace.env").orElse(DEFAULT_NAMESPACE_SYS);
  
  private static final Map<String, Optional<String>> KEY_TO_VALUE = new ConcurrentHashMap<>();
  private static final String NAMESPACE_LOG = prop("namespace.log", NAMESPACE_SYS);
  
  private static final File RO_DIR = new File(prop("fs.roDir", "."));
  private static final File RW_DIR = new File(prop("fs.rwDir", "."));
  static {
    if (RW_DIR.isFile()) {
      throw new IllegalArgumentException("RW Dir is not a directory but a file (" + RW_DIR.getAbsolutePath() + "!");
    } else if (!RW_DIR.exists()) {
      RW_DIR.mkdirs(); 
    }   
  }
  
  private static final ThreadLocal<Map<Class<?>, ?>> THREAD_CTX = new ThreadLocal<>();
  
  private Ctx() {};
  
  public static Optional<String> prop(final String key) {
    return KEY_TO_VALUE.computeIfAbsent(key, k -> get(NAMESPACE_ENV + '_' + k.replace('.', '_').toUpperCase(), NAMESPACE_SYS + '.' + k));
  }
  public static String prop(final String key, final String defaultValue) {
    return prop(key).orElse(defaultValue);
  }

  public static Optional<Boolean> boolProp(final String key) {
    return prop(key).map(Boolean::parseBoolean);
  }
  public static boolean boolProp(final String key, final boolean defaultValue) {
    return boolProp(key).orElse(defaultValue);
  }

  public static Optional<Integer> intProp(final String key) {
    return prop(key).map(Integer::parseInt);
  }
  public static int intProp(final String key, final int defaultValue) {
    return intProp(key).orElse(defaultValue);
  }

  public static Optional<Long> longProp(final String key) {
    return prop(key).map(Long::parseLong);
  }
  public static long longProp(final String key, final long defaultValue) {
    return longProp(key).orElse(defaultValue);
  }

  public static Optional<Float> floatProp(final String key) {
    return prop(key).map(Float::parseFloat);
  }
  public static float floatProp(final String key, final float defaultValue) {
    return floatProp(key).orElse(defaultValue);
  }

  public static Optional<Double> doubleProp(final String key) {
    return prop(key).map(Double::parseDouble);
  }
  public static double doubleProp(final String key, final double defaultValue) {
    return doubleProp(key).orElse(defaultValue);
  }

  public static Logger logger(final Class<?> clazz) {
    return LoggerFactory.getLogger(clazz);
  }
  
  public static Logger logger(final String name) {
    return LoggerFactory.getLogger(NAMESPACE_LOG + "." + name);
  }
  
  public static InputStream resourceStrean(final String name) throws IOException {
    final File file = new File(RO_DIR, name);
    if (file.isFile()) {
      return new FileInputStream(file);
    } else {
      return Ctx.class.getClassLoader().getResourceAsStream(name);
    }
  }
  
  public static byte[] resource(final String name) throws IOException {
    final File file = new File(RO_DIR, name);
    if (file.isFile()) {
      return Files.readAllBytes(file.toPath());
    } else {
      try (final InputStream is = Ctx.class.getClassLoader().getResourceAsStream(name)) {
        if (is == null) {
          return null;
        } else {
          final byte[] buff = new byte[4196];
          try (final ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            for (int read; (read = is.read(buff)) != -1; os.write(buff, 0, read));
            return os.toByteArray();
          }
        }
      }
    }
  }
  
  public static File readOnlyFile(final String fileName) {    
    return new File(RO_DIR, fileName);
  }
  
  public static File readWriteFile(final String fileName) {    
    return new File(RW_DIR, fileName);
  }
  
  @SuppressWarnings("unchecked")
  public static <T> Optional<T> ctx(final Class<T> key) {
    return Optional.ofNullable(THREAD_CTX.get()).map(ctxMap -> (T)ctxMap.get(key));
  }
  
  /**
   * Returns the previous context value.
   * 
   * @param <T> type pf the context value
   * @param key key of the context value
   * @param ctx the new context to be set
   * @return the previous context value
   */
  @SuppressWarnings("unchecked")
  public static <T> Optional<? extends T> ctx(final Class<T> key,  T ctx) {
    @SuppressWarnings("rawtypes")
    Map ctxMap = THREAD_CTX.get();
    if (ctxMap == null) {
      ctxMap = new HashMap<>();
      THREAD_CTX.set(ctxMap);
    }
    return Optional.ofNullable((T)ctxMap.put(key, ctx));
  }
  
  private static Optional<String> get(final String envStyle, final String sysStyle) {
    String value = System.getenv(envStyle);
    if (value != null) {
      return Optional.of(value);
    }
    value = System.getenv(sysStyle);
    if (value != null) {
      return Optional.of(value);
    }
    value = System.getProperty(sysStyle);
    if (value != null) {
      return Optional.of(value);
    }
    value = System.getProperty(envStyle);
    return Optional.ofNullable(value);
  }
  
  public static class RunAs {
    
    private final Class<RunAs> key;
    
    @SuppressWarnings({ "unchecked", "rawtypes" })
    protected RunAs(final Class<? extends RunAs> key) {
      if (!key.isAssignableFrom(this.getClass())) {
        throw new IllegalArgumentException("The key must be a supper class / interface!");
      }
      this.key = (Class)key;
    }
    
    public void runAs(final Runnable runnable) {
      final RunAs current = ctx(key, this).orElse(null);
      try {
        runnable.run();
      } finally {
        ctx(key, current);
      }
    }
    
    public <T> T callAs(final Supplier<T> function) {
      final RunAs current = ctx(key, this).orElse(null);
      try {
        return function.get();
      } finally {
        ctx(key, current);
      }
    }
  }
}