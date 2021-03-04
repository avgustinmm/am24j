am24j project aim is to provide basic utilities and framework that could help application developers to start their rich feature projects 
(including micro-services] having already ready for use base.

They are could be classified as follows:
### Basic utilities
* Commons - library of some very simple utilities. It for instance provides basic application context support - properties (configured via environment variables 
and system properties), resource access (files and class path) and so on.
* Launcher - an application launcher that allows packaging all application dependencies in single jar as internal jars and then start it as a jar. The launcher 
provides the application with access to the classes and resources of the embedded dependency jars.
* Injector - simple but highly pluggable injector. It provides means for composing an application from its components in declarative (inversion of control) way.
Provides a simple Starter that could be used for fast composition of application.
* Avro  Encoding - defines bean abstraction and encoding (powered by Avro) for such beans. 
### Application framework
* Vertx runtime - components that could be used for composing a Vertx bases rich application having - clusster (powered by Hazelcast), Http pluggins, JAX-RS support 
(powered by RestEasy), components (self deployable verticles), injectable json based configuration.
* RPC - RPC over Vertx runtime. It supports two implementations - gRPC with Avro encoding and Http with Avro encoding. It supports unary calls and server streaming 
(there are plans to support client streaming and possible by directional streaming too)
### Examples
* Example application - simple module providing an example how could be packed an application with it dependencies in single jar (without unpacking dependencies), 
launched by Launcher, composed by components using Injector, Vertx runtime with pluggable HTTP raw and JAX--RS components

### Use
The following code snipped shows how to start a simple app with Service implemented by ServiceImpl exposed by a gRPC/Http RPC server
```java
Starter.start( // use default starter, it provides dependency injection (configured in default way)
  Config.class, // adds json base configuraion via files
  VertxInstance.class, // instantiate Vertx instance, iut will be injected in following components if needed
  TestAuthVerfier.class, // adds test (example) authentication verifier for gRPC
  ServiceImpl.class, // instantiate a RPC remote service (annotated with Remove, and its remote interfaces annotated with Service  
  Server.class); // finally instantiate and start gRPC/Http RPC server, which gets all already instantiated remote services and registers them
```

