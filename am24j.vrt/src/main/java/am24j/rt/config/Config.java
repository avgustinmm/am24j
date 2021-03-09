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
package am24j.rt.config;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;

import am24j.commons.Ctx;
import am24j.commons.Types;
import am24j.inject.InjectException;
import am24j.inject.Injector;
import am24j.inject.spi.Resolver;
import io.vertx.core.json.JsonObject;

/**
 * Config component that:
 * <ul>
 *   <li>Reads config.sysJson file (sys.json by default) if exists, which has to be key/simple value json file
 *     and sets key/values as system properties</li>
 *   <li>Adds {@link Injector}'s {@link Resolver} that resolves named (having @ {@link Named} annotation)
 *     {@link JsonObject} and objects having single {@link JsonObject} argument constructors </li>
 * </ul>
 * Note: the json's are resolved using {@link Named} value from resources (get from {@link Ctx}).
 *
 * @author avgustinmm
 */
@Singleton
public class Config {

  private static final Logger LOG = Ctx.logger("Config");

  private static final Pattern PATTERN = Pattern.compile("\\$\\{([^:}]+)(?::-([^}]*))?\\}");

  @SuppressWarnings("rawtypes")
  @Inject
  public Config(final Injector injector) throws IOException {
    // supports json, prs, properties ...
    final String sysJson = Ctx.prop("config.sysJson", "sys.json");
    final byte[] sysJsonBA = Ctx.resource(sysJson);
    if (sysJsonBA != null) {
      // Apply conversion to file (if there are place holders) on the copy of original file
      final File target = Ctx.readWriteFile("_tmp_efective_" + sysJson);
      target.deleteOnExit();
      try (final OutputStream os = new FileOutputStream(target)) {
        os.write(sysJsonBA);
      }
      applyEnv(target);

      toMap(target).forEach((key, value) -> System.setProperty(key, String.valueOf(value)));
    }

    injector.add((key, point, inj) -> {
      if (key.type() instanceof Class) {
        final Class clazz = (Class)key.type();
        try  {
          final String name = key.qualifer().filter(q -> q instanceof Named).map(q -> ((Named)q).value()).orElse(null);
          if (name != null) {
            // try prioperty
            if (clazz == boolean.class || clazz == Boolean.class) {
              return () -> Ctx.boolProp(name);
            } else if (clazz == int.class || clazz == Integer.class) {
              return () -> Ctx.intProp(name);
            } else if (clazz == long.class || clazz == Long.class) {
              return () -> Ctx.longProp(name);
            } else if (clazz == float.class || clazz == Float.class) {
              return () -> Ctx.floatProp(name);
            } else if (clazz == double.class || clazz == Double.class) {
              return () -> Ctx.doubleProp(name);
            } else if (clazz == String.class) {
              return () -> Ctx.prop(name);
            }

            // try config file - support JsonObject and object with JsonObject constructor
            final byte[] configFile = Ctx.resource(name);
            if (configFile != null) {
              final File target = Ctx.readWriteFile("_tmp_efective_" + name);
              target.deleteOnExit();
              try (final OutputStream os = new FileOutputStream(target)) {
                os.write(configFile);
              }
              applyEnv(target);

              final JsonObject json = new JsonObject(new String(Files.readAllBytes(target.toPath()), StandardCharsets.UTF_8));

              if (clazz == JsonObject.class) {
                return () -> json;
              } else if (clazz == Map.class) {
                return () -> json.getMap();
              } else if (Annotation.class.isAssignableFrom(clazz)) {
                return () -> Proxy.newProxyInstance(clazz.getClassLoader(), new Class<?>[] {clazz}, (proxy, method, args) -> {
                  final Object value = json.getValue(method.getName());
                  return value == null ? method.getDefaultValue() : Types.toType(value, method.getReturnType());
                });
              } else {
                try {
                  @SuppressWarnings("unchecked")
                  final Constructor<Object> constr =  clazz.getDeclaredConstructor(JsonObject.class);
                  try { // if possible
                    constr.setAccessible(true);
                  } catch (final Throwable t) {}
                  final Object obj = constr.newInstance(json);
                  return () -> obj;
                } catch (final NoSuchMethodException e) {
                  LOG.debug(".. no Json opbject const");
                }
              }
            }
          }
        } catch (final InvocationTargetException e) {
          throw InjectException.of(e.getCause());
        } catch (final Exception e) {
          throw InjectException.of(e);
        }
      }
      throw Resolver.NOT_FOUND;
    });
  }

  private static void applyEnv(final File file) throws IOException {
    final String str = new String(Files.readAllBytes(file.toPath()));
    final String applied = applyEnv(str);
    if (str != applied) {
      Files.copy(new ByteArrayInputStream(applied.getBytes(StandardCharsets.UTF_8)), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }
  }
  private static String applyEnv(final String str) {
    return replace(str, PATTERN, matcher -> Ctx.prop(matcher.group(1)).orElse(matcher.groupCount() > 1 ? matcher.group(2) : ""));
  }

  private static Map<String, Object> toMap(final File file) throws IOException {
    final String fileName = file.getName();
    final int index = fileName.lastIndexOf('.');
    if (index == -1) throw new IllegalArgumentException("Only files with extensions could be converted to a maps!");
    final String ext = fileName.substring(index + 1).toLowerCase();
    if ("prs".equals(ext) || "properties".equals(ext)) {
      final Properties props = new Properties();
      try (final InputStream fis = new FileInputStream(file)) {
        props.load(fis);
      }
      final Map<String, Object> map = new HashMap<>();
      for (final Enumeration<Object> e = props.keys(); e.hasMoreElements();) {
        final String key = (String)e.nextElement();
        map.put(key, props.getProperty(key));
      }
      return map;
    } else if ("json".equals(ext)) {
      return new JsonObject(new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8)).getMap();
    } else {
      throw new IllegalArgumentException("Only files with extensions prs, properties or json could be converted to a map!");
    }
  }

  private static String replace(final String str, final Pattern replaceTarget, final Function<Matcher, String> replacer) {
    final Matcher matcher = replaceTarget.matcher(str);
    if (matcher.find()) {
      final StringBuilder sb = new StringBuilder();
      int start = 0;
      do {
        sb.append(str.substring(start, matcher.start()));
        sb.append(replacer.apply(matcher));
        start = matcher.end();
      } while (matcher.find());
      sb.append(str.substring(start));
      return sb.toString();
    } else {
      return str;
    }
  }
}
