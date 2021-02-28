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
package am24j.commons;

import java.util.Optional;

/**
 * Version structure.
 *
 * @author avgustinmm
 */
public class Version {

  public static final Version NULL = builder().build();

  private final int major;
  private final int minor;
  private final Optional<String> qualifier;

  private Version(final int major, final int minor, final String qualifier) {
    this.major = major;
    this.minor = minor;
    this.qualifier = Optional.ofNullable(qualifier);
  }

  public static Builder builder() {
    return new Builder();
  }

  public int major() {
    return major;
  }

  public int minor() {
    return minor;
  }

  public Optional<String> qualifier() {
    return qualifier;
  }

  @Override
  public String toString() {
    return major + "." + minor + qualifier.map(q -> "." + q).orElse("");
  }

  public static class Builder {

    private int major;
    private int minor;
    private String qualifier;

    private Builder() {}

    public Builder major(final int major) {
      this.major = major;
      return this;
    }

    public Builder minor(final int minor) {
      this.major = minor;
      return this;
    }

    public Builder qualifier(final String qualifier) {
      this.qualifier = qualifier;
      return this;
    }

    public Version build() {
      return new Version(major, minor, qualifier);
    }
  }
}
