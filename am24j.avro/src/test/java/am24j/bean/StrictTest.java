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
package am24j.bean;

import java.util.Objects;
import java.util.Optional;

import org.junit.Assert;
import org.junit.Test;

import am24j.bean.StrictTest.Buildable.Builder;

public class StrictTest {
  
  @Test
  public void bean() throws Throwable {
    final BeanStd bean = new BeanStd().setX(23).opt("test-str");
    bean.y(90);
    check(bean);
  }
  
  @Test
  public void beanOptNull() throws Throwable {
    final BeanStd bean = new BeanStd().setX(23);
    bean.y(90);
    check(bean);
  }
  
  @Test
  public void buildable() throws Throwable {
    final Builder builder = Buildable.builder().setX(23).opt("test-str");
    builder.y(90);
    check(builder.build());
  }
  
  @Test
  public void buildableOptNull() throws Throwable {
    final Builder builder = Buildable.builder().setX(23).opt(null);
    builder.y(90);
    check(builder.build());
  }
  
  @Test
  public void buildableNewBuilder() throws Throwable {
    final BuildableNewBuilder.Builder builder = BuildableNewBuilder.newBuilder().setX(23).opt("test-str");
    builder.y(90);
    check(builder.build());
  }
  
  @Test
  public void buildableNewBuilderOptNull() throws Throwable {
    final BuildableNewBuilder.Builder builder = BuildableNewBuilder.newBuilder().setX(23);
    builder.y(90);
    check(builder.build());
  }
  
  @Test
  public void buildableReq() throws Throwable {
    final BuildableReq.Builder builder = BuildableReq.newBuilder().setX(23);
    builder.y(90);
    builder.req(BuildableReq.newBuilder().setX(24).build());
    check(builder.build());
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  private static void check(final Object obj) {
    final Object getAndBuild = Struct.forClass(obj.getClass()).build(((Struct)Struct.forClass(obj.getClass())).values(obj));
    Assert.assertEquals(obj, getAndBuild);
  }

  public static class BeanStd {
    
    private int x;
    private long z;
    private String opt;
    
    // setXXX pattern
    public BeanStd setX(final int x) {
      this.x = x;
      return this;
    }
    public int getX() {
      return x;
    }
    
    // XXX pattern + different name an local var
    public void y(final long y) {
      this.z = y;
    }
    public long y() {
      return z;
    }
    
    public BeanStd opt(final String opt) {
      this.opt = opt;
      return this;
    }
    public Optional<String> opt() {
      return Optional.ofNullable(opt);
    }
    
    @Override
    public int hashCode() {
      return Objects.hash(x, z, opt);
    }

    @Override
    public boolean equals(final Object o) {
      if (o instanceof BeanStd) {
        final BeanStd bStd = (BeanStd)o;
        return getX() == bStd.getX() && y() == bStd.y() && opt().equals(bStd.opt());
      } else {
        return false;
      }
    }

    @Override
    public String toString() {
      return "BeanStd: " + x + ":" + z + ":" + opt;
    }
  }
  
  public static class Buildable {
    
    private final int x;
    private final long z;
    private final String opt;
    
    private Buildable(final int x, final long z, final String opt) {
      this.x = x;
      this.z = z;
      this.opt = opt;
    }
    
    public static Builder builder() {
      return new Builder();
    }
    
    public static Builder newBuilder() { // should be ignored - builder is with priority
      return new Builder();
    }

    public int getX() {
      return x;
    }
    
    public long y() {
      return z;
    }

    public Optional<String> opt() {
      return Optional.ofNullable(opt);
    }
    
    @Override
    public int hashCode() {
      return Objects.hash(x, z, opt);
    }

    @Override
    public boolean equals(final Object o) {
      if (o instanceof Buildable) {
        final Buildable b = (Buildable)o;
        return getX() == b.getX() && y() == b.y() && opt().equals(b.opt());
      } else {
        return false;
      }
    }
    
    @Override
    public String toString() {
      return "Buildable: " + x + ":" + z + ":" + opt;
    }
    
    public static class Builder {
      
      private int x;
      private long z;
      private String opt;
      
      public Builder setX(final int x) {
        this.x = x;
        return this;
      }
      
      public void y(final long y) {
        this.z = y;
      }
      
      public Builder opt(final String opt) {
        this.opt = opt;
        return this;
      }
      
      public Buildable build() {
        return new Buildable(x, z, opt);
      }
    }
  }
  
  public static class BuildableNewBuilder {
    
    private final int x;
    private final long z;
    private final String opt;
    
    private BuildableNewBuilder(final int x, final long z, final String opt) {
      this.x = x;
      this.z = z;
      this.opt = opt;
    }
    
    public static Builder newBuilder() {
      return new Builder();
    }

    public int getX() {
      return x;
    }
    
    public long y() {
      return z;
    }

    public Optional<String> opt() {
      return Optional.ofNullable(opt);
    }
    
    @Override
    public int hashCode() {
      return Objects.hash(x, z, opt);
    }

    @Override
    public boolean equals(final Object o) {
      if (o instanceof BuildableNewBuilder) {
        final BuildableNewBuilder b = (BuildableNewBuilder)o;
        return getX() == b.getX() && y() == b.y() && opt().equals(b.opt());
      } else {
        return false;
      }
    }
    
    @Override
    public String toString() {
      return "BuildableNewBuilder: " + x + ":" + z + ":" + opt;
    }
    
    public static class Builder {
      
      private int x;
      private long z;
      private String opt;
      
      public Builder setX(final int x) {
        this.x = x;
        return this;
      }
      
      public void y(final long y) {
        this.z = y;
      }
      
      public Builder opt(final String opt) {
        this.opt = opt;
        return this;
      }
      
      public BuildableNewBuilder build() {
        return new BuildableNewBuilder(x, z, opt);
      }
    }
  }

  public static class BuildableReq {
    
    private final int x;
    private final long z;
    private final String opt;
    private final BuildableReq req;
    
    private BuildableReq(final int x, final long z, final String opt, final BuildableReq req) {
      this.x = x;
      this.z = z;
      this.opt = opt;
      
      this.req = req;
    }
    
    public static Builder builder() {
      return new Builder();
    }
    
    public static Builder newBuilder() { // should be ignored - builder is with priority
      return new Builder();
    }

    public int getX() {
      return x;
    }
    
    public long y() {
      return z;
    }

    public Optional<String> opt() {
      return Optional.ofNullable(opt);
    }
    
    public BuildableReq req() {
      return req;
    }
    
    @Override
    public int hashCode() {
      return Objects.hash(x, z, opt, req);
    }

    @Override
    public boolean equals(final Object o) {
      if (o instanceof BuildableReq) {
        final BuildableReq b = (BuildableReq)o;
        return getX() == b.getX() && y() == b.y() && opt().equals(b.opt()) && Objects.equals(req(), b.req());
      } else {
        return false;
      }
    }
    
    @Override
    public String toString() {
      return "BuildableReq: " + x + ":" + z + ":" + opt + ":" + req;
    }
    
    public static class Builder {
      
      private int x;
      private long z;
      private String opt;
      
      private BuildableReq req;
      
      public Builder setX(final int x) {
        this.x = x;
        return this;
      }
      
      public void y(final long y) {
        this.z = y;
      }
      
      public Builder opt(final String opt) {
        this.opt = opt;
        return this;
      }
      
      public Builder req(final BuildableReq req) {
        this.req = req;
        return this;
      }
      
      public BuildableReq build() {
        return new BuildableReq(x, z, opt, req);
      }
    }
  }
}
