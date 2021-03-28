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
package am24j.config;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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

import am24j.commons.Ctx;
import io.vertx.core.json.JsonObject;

/**
 * Config component that:
 * <ul>
 *   <li>Reads config.sysJson file (sys.json by default) if exists, which has to be key/simple value json file
 *     and sets key/values as system properties</li>
 * </ul>
 * Note: the json's are resolved using {@link Named} value from resources (get from {@link Ctx}).
 *
 * @author avgustinmm
 */
@Singleton
public class Config {

  private static final Pattern PATTERN = Pattern.compile("\\$\\{([^:}]+)(?::-([^}]*))?\\}");

  @Inject
  public Config() throws IOException {
    // supports json, prs, properties ...
    final String sysJson = Ctx.prop("config.sysJson", "sys.json");
    final byte[] sysJsonBA = Ctx.resource(sysJson);
    if (sysJsonBA != null) {
      // Apply conversion to file (if there are place holders) on the copy of original file
      final File target = new File(tmpDir(), "efective_" + sysJson);
      target.deleteOnExit();
      try (final OutputStream os = new FileOutputStream(target)) {
        os.write(sysJsonBA);
      }
      applyEnv(target);

      toMap(target).forEach((key, value) -> System.setProperty(key, String.valueOf(value)));
    }
  }

  public synchronized byte[] resource(final String name) throws IOException {
    final byte[] configFile = Ctx.resource(name);
    if (configFile != null) {
      final File target = new File(tmpDir(), "efective_" + name);
      target.deleteOnExit();
      try (final OutputStream os = new FileOutputStream(target)) {
        os.write(configFile);
      }
      Config.applyEnv(target);

      return Files.readAllBytes(target.toPath());
    } else {
      return null;
    }
  }

  private File tmpDir;
  private synchronized File tmpDir() throws IOException {
    if (tmpDir == null) {
      tmpDir = Ctx.readWriteFile(".tmp" + System.currentTimeMillis());
      if (!tmpDir.mkdirs()) {
        throw new IOException("Failed to get temp dir!");
      }
      tmpDir.deleteOnExit();
    }
    return tmpDir;
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