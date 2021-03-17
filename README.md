[![javadoc](https://javadoc.io/badge2/com.github.avgustinmm/am24j/javadoc.svg)](https://javadoc.io/doc/com.github.avgustinmm/am24j)
[![CI](https://github.com/avgustinmm/am24j/actions/workflows/main.yml/badge.svg)](https://github.com/avgustinmm/am24j/actions/workflows/main.yml)
[![codecov.io](https://codecov.io/gh/avgustinmm/am24/branch/master/graphs/badge.svg?)](https://app.codecov.io/gh/avgustinmm/am24j/)

# AM24j 

### Overview
**am24j** project aim is to provide basic utilities and framework that could help application  developers to start their rich feature projects 
(including micro-services) having already ready for use base.

### Status
The project is still unstable and in preparation / development of its first release. You're welcome to check features and play with libraries. 
For any bugs or feature requests please create issues. Any feedback is highly appreciated.

### Modules
They could be classified as follows:
#### Basic utilities
* **Commons** - library of some very simple utilities. It, for instance, provides basic 
application context support - properties (configured via environment variables 
and system properties), resource access (files and class path) and so on.
* **Launcher** - an application launcher that allows packaging all application dependencies in 
 single jar as internal jars and then start it as a jar. The launcher 
provides the application with access to the classes and resources of the embedded dependency jars.
* **Injector** - simple but highly pluggable injector. It provides means for composing an 
 application from its components in declarative (inversion of control) way.
Also provides a simple Starter that could be used for fast composition of application.
* **Avro Encoding** - defines bean abstraction and encoding (powered by Avro) for such beans. 
#### Application framework
* **Vertx runtime** - components that could be used for composing a Vertx bases rich application 
 having - cluster (powered by Hazelcast), Http pluggins, JAX-RS support 
(powered by RestEasy), components (self deployable verticles), injectable json based configuration.
* **RPC** - RPC over Vertx runtime. It supports two implementations - gRPC and Http with Avro 
encoding. It supports unary calls and server streaming (there are plans to support client 
streaming and possible by directional streaming too)
#### Examples
* **Example application** - simple module providing an example how could be packed an application 
with it dependencies in single jar (without unpacking dependencies), 
launched by Launcher, composed by components using Injector, Vertx runtime with pluggable HTTP 
raw and JAX--RS components

### Details / Usage
#### Launcher
Launcher is a utility that allows to:
* pack a whole application and its dependencies in single jar with dependencies being packed 
as sub-jars (no need to be unpacked). This allows easy observing of dependencies, versions and 
do not mix them.
* the Launcher takes care to provide the sub-jars content ot the java class loader
* Launcher start one or more application main classes (classes with main method) in different 
threads (in parallel). These classes are passed as arguments of _am24j.launch.Launcher_'s 
main method

An application packed as jar (with Launcher) could be started with:

```
java -jar <application jar> <main class> [other main classes]
```
Steps to use it:
1. Pack application in a jar with embedded unpacked dependencies (for instance example app.jar)
1. Add _Main-Class: am24j.launch.Launcher_ header in the _META-INF/MAINFEST.MF_ (in order to 
be started as jar)
1. If your application has a main class _x.y.Z_, start application with command line: 
`java -jar app.jar x.y.Z`

am24j provides a simple example for such application. Example is a jar, example-app.jar which 
pack its dependencies as sub-jars, with _Main-Class am24j.launch.Launcher_ which is 
instructed (with argunments, to start / launch application class _am24j.example.App_.<br/>
To run examole get the built artifact _example-app.jar_ and start it with:
```
java -jar example-app.jar am24j.example.Server
```
Then you may access _Hello World_ HTTP and RPC app as printed on console:
* direct HTTP handler:</br>
`http://localhost/direct/hello[?name=<name>]` 
* JAX-RS:</br>
`http://localhost/jaxrs/hello[?name=<name>]`)
* start the same jar with different main class which makes request to a RPC service via gRPC:</br>
`java -jar example-app.jar am24j.example.Client`
* access HTTP RPC service via browser:</br>
`http://localhost/rpc/hellowprld_1.0.0/hello[?arg_0=<name>]`

Clustering:</br>
You may, also, start other instances of server with:
```
java -Dam24j.node.id=1 -jar example-app.jar am24j.example.Server
java -Dam24j.node.id=2 -jar example-app.jar am24j.example.Server
java -Dam24j.node.id=3 -jar example-app.jar am24j.example.Server
...
```
where node id is the id of nodes (omitting it starts default instance with id 0).</br>
All these nodes acts in cluster (powered by Hazalcast) discovered using multicast.

#### Injector
Injector is a simple implementation of Dependency Injector following concepts of JSR-330. 
Nevertheless, its simplicity it provides powerful pluggable framework that allows to be highly 
customized.<br/>
##### Pluggins
The main plugggable entities are resolvers and interceptors.
* _am24j.inject.spi.Resolver_ - may contribute for resolving objects 
* _am24j.inject.spi.Interceptor_ - are notified for already resolved objects and have chance to 
transform them. They may, for instance, collect _java.lang.AutoCloseable_ objects and close 
them on application end.
##### Annotations
Injector specifies some annotations that annotated some concepts:
* _am24j.inject.annotation.ImplementedBy_ - defines implementation of an interfaces
* _am24j.inject.annotation.Provides_ - marks method, field or class as a provider of a type
* _am24j.inject.annotation.Nullable_ - defines and injection point as a point that may 
accept _null_ 

In order to be compatible with objects that are already annotated with different annotations 
with same meaning Injector allows specifying different annotation classes for these three concepts.

#### Starter
Injector provides a Starter utility that provides injector with some predefined resolvers 
and interceptors that, based on list of classes, instantiates them (in ordered manner) and 
(on Starter.close()) finally closed auto closables.<br/>
The following code snipped shows how to start a simple app with Service implemented by 
ServiceImpl, exposed by a gRPC/Http RPC server:
```java 
// use default starter, it provides dependency injection (configured in default way)
final Starter starter = Starter.start(
  // adds json base configuration via files
  Config.class, 
  // instantiate Vertx instance, it will be injected in following components if needed
  VertxInstance.class, 
  // adds test (example) authentication verifier for gRPC
  TestAuthVerfier.class,
  // instantiate a RPC remote service (annotated with Remove, and its remote 
  // interfaces annotated with Service  
  ServiceImpl.class, 
  // finally instantiate and start gRPC/Http RPC server, which gets all already instantiated remote services and registers them
  Server.class); 
```
Then, at the end of the live of the starter you may call:
```java 
starter.close();
```
which will close all auto closeables created by Injector, implicitly created by Starter.

#### Avro Encoding
This module provides beans and primitive /simple types encoding with Avro.
Beans, in terms of this module, are java classes that follow the following patterns:
##### Standard bean
, having getters and setters, e.g.:
```java 
class StandardBean {
  
  void setXyz(int xyz) { ... }   
  int getXyz() { ...}  
  // setter may return StandardBean in order to provide fluent build API
  StandardBean setXyzFluent(int hyzDluebt) { ... }  
  int getXyzFluent() { ...}
}
```
The properties in the example above ire _xyz_ and  _xyzFluebt_ (first letter down cased).
##### Optimized bean
, having getters and setters but omitting _get/set_, e.g.:
```java 
class OptimizedBean {
  
  void xyz(int xyz) { ... }
  int xyz() { ... }
  // setter may return OptimizedBean in order to provide fluent build API
  OptimizedBean xyzFluent(int hyzDluebt) { ... }
  int xyzFluent() { ...}
}
```
The properties in the example above ire _xyz_ and  _xyzFluebt_ 
##### Buildable
, having builder e.g.:
```java 
class Buildable {

  // static builder method must be named builder or newBuilder
  static Builder builder() { 
    return new Builder();
  }

  int xyz() { ...}
  int zyzFluent() { ...}
    
  static class Builder {
  
    void xyz(int xyz) { ... } 
    // setter may return Builder in order to provide fluent build API
    Builder xyzFluent(int hyzDluebt) { ... }
    
    // Builder must have no argument build method returning buildable object
    Buildable build() { ... }
  }
}
```
The properties in the example above ire _xyz_ and  _xyzFluebt_. Not that getters of 
setters may follow standard or optimized bean pattern (the example above follows optimized).

Finally using _am24j.avro.Avrp_ methods forClaxx / forType and ther _nullable_ variants 
bean (and primitive / simple) types _org.apache.avro.Schema;_ cold be get in order to be 
used in encryption. 

#### RPC
RPC framework allows to define service interface(s), their implementations and to expose them via 
gRPC or HTTP with Avro based encoding. It allows (at the moment) unary calls and server streaming.
##### Service interface
Service interface is a Java interface that complies with some requirements
1. It is annotated with _am24j.rpc.Service_
1. Unary methods return parameterized _java.util.concurrent.CompletionStage_ or 
_java.util.concurrent.CompletableFuture_ 
1. Server streaming methods are void (i.e. return _void_ or _java.lang.Void_) and 
their last argument is a parameterized _java.util.concurrent.Flow.Subscriber_
1. Other (but server streaming last parameter) parameter types shall be compatible with 
the Avro Encoding (in general primitive / simple types or beans)
1. If gRPC or HTTP server should be resolved by Injector (not constructed explicitly) 
the implementations of service must be annotated with _am24j.rpc.Remote_

The example for Starter above shows how, for instance, a RPC server may be bootstrapped. 
For more in detail use of RPC you may take a look at RPC tests.
