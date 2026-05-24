# JPlatform Registry - etcd

etcd-based service registry implementation for JPlatform using distributed key-value storage.

## Features

- **Service Discovery**: Distributed service registration using etcd KV store
- **Automatic Cleanup**: Lease-based expiration for dead services
- **Local Caching**: Fast service lookups via local cache
- **Metadata Storage**: Rich service metadata stored in JSON format
- **Thread Safety**: All operations are thread-safe
- **Auto-renewal**: Periodic lease renewal to keep registrations alive

## Maven Dependency

```xml
<dependency>
    <groupId>org.flossware.jplatform</groupId>
    <artifactId>jplatform-registry-etcd</artifactId>
    <version>1.1</version>
</dependency>
```

## Quick Start

```java
// Configure etcd connection
EtcdRegistryConfig config = EtcdRegistryConfig.builder()
    .addEndpoint("http://localhost:2379")
    .leaseTtl(30)
    .build();

// Create and start registry
EtcdServiceRegistry registry = new EtcdServiceRegistry(config);
registry.start();

// Register a service
MyService serviceImpl = new MyServiceImpl();
registry.registerService(MyService.class, serviceImpl);

// Discover services
Optional<MyService> service = registry.getService(MyService.class);
List<MyService> allServices = registry.getAllServices(MyService.class);

// Cleanup
registry.close();
```

## Configuration Options

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| endpoints | List<String> | ["http://localhost:2379"] | etcd server endpoints |
| leaseTtl | long | 30 | Service registration TTL in seconds (min: 10) |
| namespace | String | null | Key namespace prefix (optional) |

## Architecture

Services are stored as etcd keys with JSON metadata:

```
Key:   /jplatform/services/{interface}/{uuid}
Value: {"interface": "...", "implementation": "...", "timestamp": "..."}
Lease: TTL seconds (auto-expires if not renewed)
```

## Testing

```bash
mvn test -pl jplatform-registry-etcd
```

## See Also

- [jplatform-registry-consul](../jplatform-registry-consul) - Consul service registry
- [jplatform-cluster-etcd](../jplatform-cluster-etcd) - etcd clustering
