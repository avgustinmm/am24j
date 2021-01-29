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
package am24j.inject;

import java.lang.reflect.Type;
import java.util.ArrayList;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.junit.Assert;
import org.junit.Test;

import am24j.inject.Injector.Key;

/**
 * @author avgustinmm
 */
public class InjectJSR330Test {

  @Test
  public void getInstanceObj() {
    final Injector injector = Injector.newInstance();
    final Object obj = injector.getInstance(Key.of(Object.class));
    Assert.assertTrue("Expected type", obj instanceof Object);
  }
  
  @Test
  public void getInstanceEmptyConstr() {
    final Injector injector = Injector.newInstance();
    final ArrayList<?> obj = injector.getInstance(Key.of(ArrayList.class));
    Assert.assertTrue("Expected type", obj instanceof ArrayList);
  }
  
  @Singleton
  public static class SingletonObj {}  
  
  @Test
  public void multiObject() {
    final Injector injector = Injector.newInstance();
    final Object o = injector.getInstance(Key.of(Object.class));
    final Object o2 = injector.getInstance(Key.of(Object.class));
    Assert.assertFalse("Not equal", o == o2);
  }
  
  @Test
  public void singletonObjectNoBinding() {
    final Injector injector = Injector.newInstance();
    final SingletonObj o = injector.getInstance(Key.of(SingletonObj.class));
    final SingletonObj o2 = injector.getInstance(Key.of(SingletonObj.class));
    Assert.assertTrue("Not equal", o == o2);
    final SingletonObj o3 = injector.getInstance(Key.of(SingletonObj.class));
    Assert.assertTrue("Not equal", o == o3);
  }
  
  public static class Constr {
    
    private final String str;
    private final @Named("x") String x;
    private final Object obj; 
    private final SingletonObj singl;
    private final SingletonObj singlPrv;
    
    @Inject
    public Constr(final String str, final @Named("x") String x, final Object obj, final SingletonObj singl, final Provider<SingletonObj> singlPrv) {
      this.str = str;
      this.x = x;
      this.obj = obj;
      this.singl = singl;
      this.singlPrv = singlPrv.get();
    }
  }
  
  @Test
  public void constrTest() {
    final Injector injector = Injector.newInstance();
    final String str = "str";
    final String x = "x";
    final Object obj = new Object();
    
    final SingletonObj singl = injector.getInstance(Key.of(SingletonObj.class));
    injector.bind(Key.of(String.class), str);
    injector.bind(Key.of(String.class, TestUtils.namedX()), x);
    injector.bind(Key.of(Object.class), () -> obj); // provider binding
    // auto provider - inject SingletonObj provider without explicit binding
    
    final Constr constr = injector.getInstance(Key.of(Constr.class));
    Assert.assertEquals("str", str, constr.str);
    Assert.assertEquals("x", x, constr.x);
    Assert.assertEquals("obj", obj, constr.obj);
    Assert.assertEquals("singl", singl, constr.singl);
    Assert.assertEquals("singlPrv", singl, constr.singlPrv);
  }
  
  public static class Method {
    
    private String str;
    private @Named("x") String x;
    private Object obj; 
    private SingletonObj singl;
    private SingletonObj singlPrv;
    
    private String strCopy;
    
    @Inject
    public void inj(final String str) {
      this.str = str;
      strCopy = str;
    }
    
    @Inject
    public void inj(final @Named("x") String x, final Object obj) {
      this.x = x;
      this.obj = obj;
    }
    
    @Inject
    public void inj2(final SingletonObj singl, final Provider<SingletonObj> singlPrv) {
      this.singl = singl;
      this.singlPrv = singlPrv.get();
    }
  }
  
  @Test
  public void methodTest() {
    final Injector injector = Injector.newInstance();
    final String str = "str";
    final String x = "x";
    final Object obj = new Object();
    
    final SingletonObj singl = injector.getInstance(Key.of(SingletonObj.class));
    injector.bind(Key.of(String.class), str);
    injector.bind(Key.of(String.class, TestUtils.namedX()), x);
    injector.bind(Key.of(sinlPrv()), () -> singl);
    injector.bind(Key.of(Object.class), () -> obj); // prvider binding
    
    final Method method = injector.getInstance(Key.of(Method.class));
    Assert.assertEquals("str", str, method.str);
    Assert.assertEquals("str copy", str, method.strCopy);
    Assert.assertEquals("x", x, method.x);
    Assert.assertEquals("obj", obj, method.obj);
    Assert.assertEquals("singl", singl, method.singl);
    Assert.assertEquals("singl", singl, method.singlPrv);
  }
  
  public static class Field {
    
    private @Inject String str;
    private @Inject @Named("x") String x;
    private @Inject Object obj; 
    private @Inject SingletonObj singl;
    private @Inject Provider<SingletonObj> singlPrv;
  }
  
  @Test
  public void fieldTest() {
    final Injector injector = Injector.newInstance();
    final String str = "str";
    final String x = "x";
    final Object obj = new Object();
    
    final SingletonObj singl = injector.getInstance(Key.of(SingletonObj.class));
    injector.bind(Key.of(String.class), str);
    injector.bind(Key.of(String.class, TestUtils.namedX()), x);
    injector.bind(Key.of(sinlPrv()), () -> singl);
    injector.bind(Key.of(Object.class), () -> obj); // prvider binding
    
    final Field field = injector.getInstance(Key.of(Field.class));
    Assert.assertEquals("str", str, field.str);
    Assert.assertEquals("x", x, field.x);
    Assert.assertEquals("obj", obj, field.obj);
    Assert.assertEquals("singl", singl, field.singl);
    Assert.assertEquals("singl", singl, field.singlPrv.get());
  }

  public static class MethodInh extends Method {
    
    private String str2;
    private String str3;
    
    @Override
    @Inject
    public void inj(final String str) {
      ((Method)this).str = str; // don't set copy
    }
    
    @Inject
    public void str2(final String str2) {
      this.str2 = str2;
    }
  }
  
  @Test
  public void methodInheritanceTest() {
    final Injector injector = Injector.newInstance();
    final String str = "str";
    final String x = "x";
    final Object obj = new Object();
    
    final SingletonObj singl = injector.getInstance(Key.of(SingletonObj.class));
    injector.bind(Key.of(String.class), str);
    injector.bind(Key.of(String.class, TestUtils.namedX()), x);
    injector.bind(Key.of(Object.class), () -> obj); // prvider binding
    
    final MethodInh method = injector.getInstance(Key.of(MethodInh.class));
    Assert.assertEquals("str", str, ((Method)method).str);
    Assert.assertEquals("str copy - not set", null, ((Method)method).strCopy); // not called on super
    Assert.assertEquals("x", x, ((Method)method).x);
    Assert.assertEquals("obj", obj, ((Method)method).obj);
    Assert.assertEquals("singl", singl, ((Method)method).singl);
    Assert.assertEquals("str2", str, method.str2);
    Assert.assertEquals("str3 not set", null, method.str3);
  }
  
  public static class FieldInh extends Field {
    
    private @Inject String str2;
    private String str3;
  }
  
  @Test
  public void fieldInheritanceTest() {
    final Injector injector = Injector.newInstance();
    final String str = "str";
    final String x = "x";
    final Object obj = new Object();
    
    final SingletonObj singl = injector.getInstance(Key.of(SingletonObj.class));
    injector.bind(Key.of(String.class), str);
    injector.bind(Key.of(String.class, TestUtils.namedX()), x);
    injector.bind(Key.of(Object.class), () -> obj); // prvider binding
    
    final FieldInh field = injector.getInstance(Key.of(FieldInh.class));
    Assert.assertEquals("str", str, ((Field)field).str);
    Assert.assertEquals("x", x, ((Field)field).x);
    Assert.assertEquals("obj", obj, ((Field)field).obj);
    Assert.assertEquals("singl", singl, ((Field)field).singl);
    Assert.assertEquals("str2", str, field.str2);
    Assert.assertEquals("str3 not set", null, field.str3); // not injected
  }
  
  public static class Combined {
    
    private final String str;
    private @Named("x") String x;
    private Object obj; 
    private @Inject SingletonObj singl;
    
    @Inject
    public Combined(final String str) {
      this.str = str;
    }
    
    @Inject
    public void mthd(final @Named("x") String x, final Object obj) {
      this.x = x;
      this.obj = obj;
    }
  }
  
  @Test
  public void combinedTest() {
    final Injector injector = Injector.newInstance();
    final String str = "str";
    final String x = "x";
    final Object obj = new Object();
    
    final SingletonObj singl = injector.getInstance(Key.of(SingletonObj.class));
    injector.bind(Key.of(String.class), str);
    injector.bind(Key.of(String.class, TestUtils.namedX()), x);
    injector.bind(Key.of(Object.class), () -> obj); // prvider binding
    
    final Combined combinec = injector.getInstance(Key.of(Combined.class));
    Assert.assertEquals("str", str, combinec.str);
    Assert.assertEquals("x", x, combinec.x);
    Assert.assertEquals("obj", obj, combinec.obj);
    Assert.assertEquals("singl", singl, combinec.singl);
  }
  
  private Type sinlPrv() {
    try {
      return Constr.class.getConstructor(String.class, String.class, Object.class, SingletonObj.class, Provider.class).getGenericParameterTypes()[4];
    } catch (final NoSuchMethodException | SecurityException e) {
      throw InjectException.of(e);
    }
  }
}
