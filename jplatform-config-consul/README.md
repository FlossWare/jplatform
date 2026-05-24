# JPlatform Config - Consul

HashiCorp Consul KV configuration source for JPlatform. Dynamic, distributed configuration with ACL support and change watching.

## Features

- Consul KV store integration
- ACL token support
- Configuration change watching
- Thread-safe operations
- Dynamic configuration updates

## Maven Dependency

```xml
<dependency>
    <groupId>org.flossware.jplatform</groupId>
    <artifactId>jplatform-config-consul</artifactId>
    <version>1.1</version>
</dependency>
```

## Quick Start

```java
ConsulConfigSourceConfig config = ConsulConfigSourceConfig.builder()
    .host("localhost")
    .port(8500)
    .keyPrefix("config/myapp")
    .build();

ConsulConfigSource source = new ConsulConfigSource(config);
source.start();

source.setConfig("database.host", "localhost");
String host = source.getConfig("database.host");
```

## Status

Production-ready Consul configuration implementation.
