# platform-java Registry - etcd

etcd-based service registry implementation for platform-java using distributed key-value storage.

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
    <groupId>org.flossware.platform-java</groupId>
    <artifactId>platform-java-registry-etcd</artifactId>
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
Key:   /platform-java/services/{interface}/{uuid}
Value: {"interface": "...", "implementation": "...", "timestamp": "..."}
Lease: TTL seconds (auto-expires if not renewed)
```

## Testing

```bash
mvn test -pl platform-java-registry-etcd
```

## See Also

- [platform-java-registry-consul](../platform-java-registry-consul) - Consul service registry
- [platform-java-cluster-etcd](../platform-java-cluster-etcd) - etcd clustering
