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

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Qualifier;
import javax.inject.Singleton;

import org.slf4j.Logger;

import am24j.commons.Ctx;
import am24j.inject.annotation.Nullable;
import am24j.inject.spi.Interceptor;
import am24j.inject.spi.Resolver;

/**
 * Injector - not thread safe
 *
 * @author avgustinmm
 */
public class Injector {

  private final Logger log;

  private final List<Resolver> resolvers = new ArrayList<>();
  private final List<Interceptor> interceptors = new ArrayList<>();
  private final List<BindListener> bindListeners = new ArrayList<>();

  private final Map<Key, Provider<Object>> bindings = new HashMap<>();
  private final Class<?>[] nullableAnnotations;

  private Injector(final Logger log, final Class<?>[] nullableAnnotations) {
    this.log = log == null ? Ctx.logger("Injector") : log;
    this.nullableAnnotations = nullableAnnotations == null ? new Class<?>[] {Nullable.class} : nullableAnnotations;
  }

  public static Injector newInstance() {
    return newInstance((Logger)null);
  }

  public static Injector newInstance(final Class<? extends Annotation>[] nullableAnnotations) {
    return newInstance(null, nullableAnnotations);
  }

  public static Injector newInstance(final Logger log) {
    return newInstance(log, null);
  }

  public static Injector newInstance(final Logger log, final Class<?>[] nullableAnnotations) {
    final Injector injector = new Injector(log, nullableAnnotations);
    injector.bind(Key.of(Injector.class), injector);
    return injector;
  }

  // Note: if resolver implements interceptor interface and is not explicitly added as interceptor it intercepts the
  // final created object BUT is unable to modify it - i.e. the resilt is not taken in account
  public <T> Injector add(final Resolver resolver) {
    log.info("Add resolver: {}", resolver);
    resolvers.add(resolver);
    if (resolver instanceof BindListener) {
      bindListeners.add((BindListener)resolver);
    }
    return this;
  }

  public Injector add(final Interceptor interceptor) {
    log.info("Add interceptor: {}", interceptor);
    interceptors.add(interceptor);
    return this;
  }

  public Injector add(final BindListener bindListener) {
    bindListeners.add(bindListener);
    return this;
  }

  public Injector bind(final Key key, final Object instance) { // instance - instance or provider
    log.info("Bind an instance {} -> {}", key, instance);
    // TODO - check if no binding (configuration may allow overriding)
    final Provider<Object> newProvider = () -> instance;
    bindings.put(key, newProvider); // singleton
    bindListeners.forEach(bl -> bl.bound(key, newProvider, Injector.this));
    return this;
  }

  @SuppressWarnings("unchecked")
  public Injector bind(final Key key, final Provider<?> provider) {
    log.info("Bind an provider: {} -> {}", key, provider);
    // TODO - check if no binding (configuration may allow overriding)
    // TOOD - check if instance or provider are of type of the key (on load)
    bindings.put(key, () -> intercapt(key, Optional.empty(), provider.get()));
    bindListeners.forEach(bl -> bl.bound(key, (Provider<Object>)provider, Injector.this));
    return this;
  }

  public Injector bind(final Key key, final Class<?> clazz) {
    log.info("Bind an class: {} -> {}", key, clazz);
    // TODO - check if no binding (configuration may allow overriding)
    Provider<Object> newProvider;
    try {
      newProvider = getInstanceApplySingleton(Key.of(clazz), Optional.empty());
    } catch (final InjectException e) {
      if (nullable(clazz.getAnnotations())) {
        newProvider = () -> null; // do not bind null values
      } else {
        throw e;
      }
    }
    bindings.put(key, newProvider);
    for (final BindListener bindListener : bindListeners) {
      bindListener.bound(key, newProvider, this);
    }
    return this;
  }

  public <T> T getInstance(final Key key) {
    log.info("[{}] Resolving instnace ...", key);
    try {
      final T obj = getInstance(key, Optional.empty());
      log.info("[{}] Resolved -> {} (hashCode: {})", key, obj, obj == null ? 0 : obj.hashCode());
      return obj;
    } catch (final Throwable t) {
      log.error("[{}] Failed to resolve ({})!", key, this, t);
      throw t;
    }
  }

  @Override
  public String toString() {
    return "Injector (Resolvers: " + resolvers + ", interceptors: " + interceptors + ")";
  }

  @SuppressWarnings("unchecked")
  private <T> T getInstance(final Key key, final Optional<Point> point) {
    log.debug("[{}][{}] getInstance ...", key, point);
    Provider<T> provider;
    synchronized (bindings) { // compute if absent may fall in reqursive update
      final Provider<T> cached = (Provider<T>)bindings.get(key);
      if (cached == null) {
        provider = (Provider<T>)newProvider(key, point);
        if (point.isEmpty()) {
          final Class<?> clazz = Utils.clazz(key.type());
          final Type providerType = Utils.providerType(clazz);
          if (providerType != null) {
            // TODO - add test for: X implements Provider<Y> => add X as provider for Y
            bindings.put(Key.of(providerType, key.qualifer().orElse(null)), (Provider<Object>)provider);
          }
          bindings.put(key, (Provider<Object>)provider);
        }
      } else {
        provider = cached;
      }
    }
    if (point.map(Point::isProvider).orElse(provider.getClass() == key.type())) {
      return (T)provider;
    } else {
      return provider.get();
    }
  }
  private Provider<Object> newProvider(final Key key, final Optional<Point> point) {
    log.debug("[{}][{}] not bound, go though resolvers", key, point);
    for (final Resolver resolver : resolvers) {
      log.trace("[{}][{}] tries {}", key, point, resolver);
      try {
        return resolver.get(key, point, Injector.this);
      } catch (final InjectException ie) {
        if (ie != Resolver.NOT_FOUND) {
          log.debug("[{}][{}] instantiation error by {}!", key, point, resolver, ie);
          throw ie;
        }
        log.trace("[{}][{}] not found by {}", key, point, resolver);
      }
    }

    final Provider<Object> newProvider;
    if (key.type() instanceof ParameterizedType && Utils.clazz(key.type()) == Provider.class) {
      log.debug("[{}][{}] provider is not bound, returns auto provider", key, point);
      newProvider = () -> getInstance(Key.of(((ParameterizedType)key.type()).getActualTypeArguments()[0], key.qualifer().orElse(null)));
    } else {
      newProvider = getInstanceApplySingleton(key, point);
    }
    bindListeners.forEach(bl -> bl.bound(key, newProvider, Injector.this));
    return newProvider;
  }

  @SuppressWarnings("unchecked")
  private Provider<Object> getInstanceApplySingleton(final Key key, final Optional<Point> point) {
    final Class<?> clazz = Utils.clazz(key.type());
    if (Provider.class.isAssignableFrom(clazz)) {
      return intercapt(key, point, (Provider<Object>)getInstanceCheckCyclicDependencies(key, clazz, point));
    } else if (clazz.getAnnotation(Singleton.class) == null) {
      log.trace("[{}][{}] not a singleton, get evety time", key, point);
      return () -> intercapt(key, point, getInstanceCheckCyclicDependencies(key, clazz, point));
    } else {
      log.trace("[{}][{}] singleton, create once", key, point);
      final Object singleton = intercapt(key, point, getInstanceCheckCyclicDependencies(key, clazz, point));
      return () -> singleton;
    }
  }
  @SuppressWarnings({ "unchecked", "unlikely-arg-type" })
  private <T> T intercapt(final Key key, final Optional<Point> point, T obj) {
    log.trace("[{}][{}] go through interceptors ...", key, point);
    for (final Interceptor interceptor : interceptors) {
      log.trace("[{}][{}] pass to ...", key, point, interceptor);
      obj = (T)interceptor.handle(key, point, obj, Injector.this);
    }
    final Object objF = obj;
    resolvers.forEach(resolver -> {
      if (resolver instanceof Interceptor && !interceptors.contains(resolver)) {
        ((Interceptor)resolver).handle(key, point, objF, Injector.this);
      }
    });
    return obj;
  }

  private final List<Key> stack = new ArrayList<>();
  private Object getInstanceCheckCyclicDependencies(final Key key, final Class<?> clazz, final Optional<Point> point) {
    final int index = stack.indexOf(key);
    if (index == -1) {
      stack.add(key);
      try {
        if (stack.size() > 1) {
          log.debug("[{}][{}] get instance, stack: {}", key, clazz.getName(), stack);
        }
        return createByJSR330(key, clazz);
      } catch (final InjectException e) {
        if (point.isPresent() && point.get().nullable()) {
          return null; // do not bind null values
        } else {
          throw e;
        }
      } finally {
        stack.remove(key); // TODO -check it's last
      }
    } else {
      final StringBuilder sb = new StringBuilder("Cyclic dependencies! ");
      for (int i = index; i < stack.size(); i++) {
        sb.append(stack.get(i)).append(" -> ");
      }
      sb.append(key);
      throw InjectException.of(sb.toString());
    }
  }

  // creates an instances by JSR 330 annotations
  private <T> T createByJSR330(final Key key, final Class<T> clazz) {
    try {
      final Constructor<T> iConstr = findIConstr(clazz);
      try { // if possible
        iConstr.setAccessible(true);
      } catch (final Throwable t) {}

      final Type[] types = iConstr.getGenericParameterTypes();
      final T obj;
      if (types == null || types.length == 0) {
        log.debug("[{}][{}] no injector constructor, try empty", key, clazz.getName());
        obj = iConstr.newInstance();
      } else {
        log.debug("[{}][{}] injector constructor: {}", key, clazz.getName(), iConstr);
        final Class<?>[] paramTypes = iConstr.getParameterTypes();
        final Annotation[][] paramAnnotations = iConstr.getParameterAnnotations();
        final Object[] args = new Object[types.length];
        for (int i = 0; i < args.length; i++) {
          final int iF = i;
          args[i] = getInstance(
            Key.of(types[i], qualifier(paramAnnotations[i], () -> "constructor " + iConstr + " param " + iF)),
            Optional.of(Point.of(iConstr, i, this::nullable)));
          if (args[i] != null && !paramTypes[i].isAssignableFrom(args[i].getClass())) {
            log.error("[{}][{}] Constructor {}'s parameter {} is of type {} but resolved object is of type {}!", key, clazz.getName(), iConstr, i, paramTypes[i], args[i].getClass());
            throw InjectException.of("Constructor " + iConstr + "'s param " + i + " is of type " + paramTypes[i] + " but resolved object is of type " + args[i].getClass().getName() + "!");
          }
        }
        obj = iConstr.newInstance(args);
      }
      injectFields(key, clazz, obj);
      return injectMethids(key, clazz, obj, new ArrayList<>());
    } catch (final Throwable e) {
      throw InjectException.of(e);
    }
  }

  private <T extends S, S> T injectFields(final Key key, final Class<S> clazz, final T obj) throws IllegalArgumentException, IllegalAccessException {
    for (final Field field : clazz.getDeclaredFields()) {
      if (field.getAnnotation(Inject.class) != null) {
        final Object value = getInstance(
          Key.of(field.getGenericType(), qualifier(field.getAnnotations(), () -> "field " + field)),
          Optional.of(Point.of(field, obj, this::nullable)));
        if (value != null && !field.getType().isAssignableFrom(value.getClass())) {
          log.error("[{}][{}] Field {} is of type {} but resolved object is of type {}!", key, clazz.getName(), field, field.getType(), value.getClass());
          throw InjectException.of("Field " + field + " is of type " + field.getType() + " but resolved object is of type " + value.getClass().getName() + "!");
        }
        try { // if possible
          field.setAccessible(true);
        } catch (final Throwable t) {}
        field.set(obj, value);
      }
    }

    // inject super
    @SuppressWarnings("unchecked")
    final Class<S> superCLazz = (Class<S>)clazz.getSuperclass();
    return superCLazz == null || superCLazz == Object.class ? obj : injectFields(key, superCLazz, obj);
  }

  private <T extends S, S> T injectMethids(final Key key, final Class<S> clazz, final T obj, final List<String> injected) throws Throwable {
    for (final Method method : clazz.getDeclaredMethods()) {
      if (method.getAnnotation(Inject.class) != null) {
        final String id = id(method);
        if (!injected.contains(id)) {
          try { // if possible
            method.setAccessible(true);
          } catch (final Throwable t) {}

          log.debug("[{}][{}] inject method: {}", key, clazz.getName(), method);
          final Type[] types = method.getGenericParameterTypes();
          try {
            if (types == null || types.length == 0) {
              method.invoke(obj);
            } else {
              final Class<?>[] paramTypes = method.getParameterTypes();
              final Annotation[][] paramAnnotations = method.getParameterAnnotations();
              final Object[] args = new Object[types.length];
              for (int i = 0; i < args.length; i++) {
                final int iF = i;
                args[i] = getInstance(
                  Key.of(types[i], qualifier(paramAnnotations[i], () -> "method " + method + " param " + iF)),
                  Optional.of(Point.of(method, i, obj, this::nullable)));
                if (args[i] != null && !paramTypes[i].isAssignableFrom(args[i].getClass())) {
                  log.error("[{}][{}] Method {}'s parameter {} is of type {} but resolved object is of type {}!", key, clazz.getName(), method, i, paramTypes[i], args[i].getClass());
                  throw InjectException.of("Method " + method + "'s param " + i + " is of type " + paramTypes[i] + " but resolved object is of type " + args[i].getClass().getName() + "!");
                }
              }
              method.invoke(obj, args);
            }
            injected.add(id);
          } catch (final InvocationTargetException e) {
            throw e.getCause() == null ? e : e.getCause();
          }
        }
      }
    }

    // inject super
    @SuppressWarnings("unchecked")
    final Class<S> superCLazz = (Class<S>)clazz.getSuperclass();
    return superCLazz == null || superCLazz == Object.class ? obj : injectMethids(key, superCLazz, obj, injected);
  }

  @SuppressWarnings("unchecked")
  private static <T> Constructor<T> findIConstr(final Class<T> clazz) {
    Constructor<T> iConstr = null;
    for (final Constructor<?> constr : clazz.getConstructors()) {
      if (constr.getAnnotation(Inject.class) != null) {
        if (iConstr == null) {
          iConstr = (Constructor<T>)constr;
        } else {
          throw InjectException.of("Ambiguos injectable constructors for " + clazz.getName() + ": " + iConstr + " and " + constr);
        }
      }
    }
    if (iConstr == null) {
      try {
        return clazz.getConstructor(); // find empty constructor
      } catch (final NoSuchMethodException e) {
        throw InjectException.of("No injectable constructor for " + clazz.getName() + "!");
      }
    } else {
      return iConstr;
    }
  }

  private static Annotation qualifier(final Annotation[] annotations, final Supplier<String> target) {
    Annotation qualifier = null;
    for (final Annotation annotation : annotations) {
      if (annotation.annotationType().getAnnotation(Qualifier.class) != null) {
        if (qualifier == null) {
          qualifier = annotation;
        } else {
          throw InjectException.of("Ambiguos qualifier for " + target.get() + "!");
        }
      }
    }
    return qualifier;
  }

  private static String id(final Method method) {
    final StringBuilder sb = new StringBuilder(method.getName()).append(":(");
    final Class<?>[] parameterTypes = method.getParameterTypes();
    if (method.getParameterCount() != 0) {
      sb.append(parameterTypes[0].getName());
      for (int i = 1; i < parameterTypes.length; i++) {
        sb.append(", ").append(parameterTypes[i].getName());
      }
    }
    sb.append(")");
    return sb.toString();
  }

  private boolean nullable(final Annotation[] annotations) {
    for (final Annotation annotation : annotations) {
      for (final Class<?> nullableAnnotation : nullableAnnotations) {
        if (nullableAnnotation.equals(annotation.annotationType())) {
          return true;
        }
      }
    }
    return false;
  }

  public static class Key {

    private final Type type;
    private final Optional<Annotation> qualifer;

    private Key(final Type type, final Annotation qualifer) {
      Objects.requireNonNull(type, "Type must be non-null!");
      // TOOD - check if annotation (if not null is Quailigef - maybe congigurable
      this.type = type;
      this.qualifer = Optional.ofNullable(qualifer);
    }

    public static Key of(final Type type) {
      return new Key(type, null);
    }

    public static Key of(final Type type, final Annotation qualifer) {
      return new Key(type, qualifer);
    }

    public static Key of(final Type type, final String name) {
      return new Key(type, Utils.named(name));
    }

    public Type type() {
      return type;
    }

    public Optional<Annotation> qualifer() {
      return qualifer;
    }

    @Override
    public boolean equals(final Object o) {
      if (o instanceof Key) {
        final Key k = (Key)o;
        return Objects.equals(type, k.type) && Objects.equals(qualifer, k.qualifer);
      } else {
        return false;
      }
    }

    @Override
    public int hashCode() {
      return Objects.hashCode(type) + 31 * Objects.hashCode(qualifer);
    }

    @Override
    public String toString() {
      return qualifer.map(q -> type.getTypeName() + "(qualifier: " + q + ")").orElse(type.getTypeName());
    }
  }

  public static class Point {

    private final Member member; // constructor, method or field
    private final int index; // index of parameter if the member is constructor or method
    private final boolean nullable;

    private final Object obj; // object to be injected if field or method injection

    private Point(final Member member, final int index, final boolean nullable, final Object obj) {
      this.member = member;
      this.index = index;
      this.nullable = nullable;
      this.obj = obj;
    }

    private static Point of(final Method method, final int index, final Object obj, final Function<Annotation[], Boolean> nullableTest) {
      return new Point(method, index, nullableTest.apply(method.getParameterAnnotations()[index]), obj);
    }

    private static <T> Point of(final Constructor<T> constr, final int index, final Function<Annotation[], Boolean> nullableTest) {
      return new Point(constr, index, nullableTest.apply(constr.getParameterAnnotations()[index]), null);
    }

    private static Point of(final Field field, final Object obj, final Function<Annotation[], Boolean> nullableTest) {
      return new Point(field, -1, nullableTest.apply(field.getAnnotations()), obj);
    }

    public Object obj() {
      return obj;
    }

    public Member member() {
      return member;
    }

    public int index() {
      return index;
    }

    public boolean nullable() {
      return nullable;
    }

    @Override
    public String toString() {
      return member.toString() + "[" + index + "]" + (nullable ? " nullable" : "");
    }

    private boolean isProvider() {
      if (member instanceof Executable) {
        return ((Executable)member).getParameterTypes()[index] == Provider.class;
      } else {
        return ((Field)member).getType() == Provider.class;
      }
    }
  }

  public static interface BindListener {

    public void bound(final Key key, final Provider<Object> provider, final Injector injector);
  }
}