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
package am24j.rt.hz;

import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.slf4j.Logger;

import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;

import am24j.commons.Builder;
import am24j.commons.Ctx;
import am24j.inject.annotation.Provides;
import io.vertx.core.json.JsonObject;

/**
 * Provides hazelcast instnace
 *
 * @author avgustinmm
 */
@Singleton
@Provides
public class HZInstance implements Provider<HazelcastInstance>, AutoCloseable {

  private static final Logger LOG = Ctx.logger("Hazelcast");

  private final HazelcastInstance hazelcast;

  @Inject
  public HZInstance(@Named("hazelcast.json") final JsonObject clusterConfig) throws IOException {
    hazelcast = hazelcast(clusterConfig);
  }

  @Override
  public HazelcastInstance get() {
    return hazelcast;
  }

  @Override
  public void close() {
    hazelcast.shutdown();
  }

  private HazelcastInstance hazelcast(final JsonObject clusterConfig) throws IOException {
    System.setProperty("hazelcast.logging.type", "slf4j"); // set logger

    final Config config = new Config();

    LOG.info("Apply cluster json config");
    Builder.inject(clusterConfig, config);

    LOG.info("Creating cluster (config: {}) ...", config);
    return Hazelcast.newHazelcastInstance(config);
  }
}