# platform-java Config - Consul

HashiCorp Consul KV configuration source for platform-java. Dynamic, distributed configuration with ACL support and change watching.

## Features

- **Consul KV Store**: Store and retrieve configuration from Consul KV
- **Dynamic Updates**: Watch for configuration changes in real-time
- **ACL Support**: Token-based authentication for secure access
- **Hierarchical Keys**: Support for hierarchical configuration structure  
- **Thread-Safe**: Concurrent access to configuration
- **Datacenter Support**: Connect to specific Consul datacenters
- **Caching**: Local configuration caching for performance
- **Event Listeners**: Notification of configuration changes

## Maven Dependency

```xml
<dependency>
    <groupId>org.flossware.platform-java</groupId>
    <artifactId>platform-java-config-consul</artifactId>
    <version>1.1</version>
</dependency>
```

## Quick Start

### Basic Usage

```java
ConsulConfigSourceConfig config = ConsulConfigSourceConfig.builder()
    .host("localhost")
    .port(8500)
    .keyPrefix("config/myapp")
    .build();

ConsulConfigSource source = new ConsulConfigSource(config);
source.start();

// Set configuration
source.setConfig("database.host", "localhost");
source.setConfig("database.port", "5432");

// Get configuration
String dbHost = source.getConfig("database.host");

// Load all configuration
Map<String, String> allConfig = source.loadConfig();
```

### With ACL Token

```java
ConsulConfigSourceConfig config = ConsulConfigSourceConfig.builder()
    .host("consul.example.com")
    .port(8500)
    .aclToken("your-acl-token-here")
    .keyPrefix("config/myapp")
    .build();
```

### With Datacenter

```java
ConsulConfigSourceConfig config = ConsulConfigSourceConfig.builder()
    .host("consul.example.com")
    .port(8500)
    .datacenter("dc1")
    .keyPrefix("config/myapp")
    .build();
```

### Configuration Change Listeners

```java
ConsulConfigSourceConfig config = ConsulConfigSourceConfig.builder()
    .host("localhost")
    .port(8500)
    .keyPrefix("config/myapp")
    .watchEnabled(true)
    .build();

ConsulConfigSource source = new ConsulConfigSource(config);
source.start();

// Register listener for configuration changes
source.addListener(updatedConfig -> {
    System.out.println("Configuration updated:");
    updatedConfig.forEach((key, value) -> 
        System.out.println(key + " = " + value));
});

// Configuration changes will trigger the listener
```

### Environment-Specific Configuration

```java
// Development
ConsulConfigSourceConfig devConfig = ConsulConfigSourceConfig.builder()
    .host("localhost")
    .keyPrefix("config/myapp/dev")
    .build();

// Production
ConsulConfigSourceConfig prodConfig = ConsulConfigSourceConfig.builder()
    .host("consul.prod.example.com")
    .aclToken(System.getenv("CONSUL_TOKEN"))
    .keyPrefix("config/myapp/prod")
    .datacenter("us-east-1")
    .build();
```

## Configuration Options

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `host` | String | `localhost` | Consul agent hostname |
| `port` | int | `8500` | Consul HTTP API port |
| `aclToken` | String | `null` | ACL token for authentication |
| `datacenter` | String | `null` | Datacenter name (uses agent's DC if null) |
| `keyPrefix` | String | required | Key prefix for configuration hierarchy |
| `watchEnabled` | boolean | `false` | Enable configuration change watching |
| `watchIntervalSeconds` | long | `30` | Interval for polling configuration changes |

## Architecture

### Key Structure

Consul KV uses a hierarchical key structure:

```
{keyPrefix}/database/host   -> "localhost"
{keyPrefix}/database/port   -> "5432"
{keyPrefix}/app/name        -> "My Application"
```

Example with prefix `config/myapp`:
```
config/myapp/database/host  -> "localhost"
config/myapp/database/port  -> "5432"
config/myapp/app/name       -> "My Application"
```

### Watch Mechanism

When `watchEnabled` is true, the configuration source:

1. Polls Consul KV at configured interval
2. Compares with cached configuration
3. If changed, notifies registered listeners
4. Updates local cache

### Caching

- Configuration is cached locally for performance
- Cache is updated on each `getConfig()` call
- Watch mechanism updates cache automatically
- Cache is cleared on `close()`

## Consul Setup

### Docker (Development)

```bash
docker run -d \
  --name consul \
  -p 8500:8500 \
  consul:latest \
  agent -dev -ui -client=0.0.0.0
```

### Docker (Production Mode)

```bash
docker run -d \
  --name consul \
  -p 8500:8500 \
  -p 8600:8600/udp \
  consul:latest \
  agent -server -bootstrap-expect=1 -ui -client=0.0.0.0
```

### Kubernetes

```yaml
apiVersion: v1
kind: Service
metadata:
  name: consul
spec:
  selector:
    app: consul
  ports:
    - port: 8500
      name: http
    - port: 8600
      protocol: UDP
      name: dns
---
apiVersion: apps/v1
kind: StatefulSet
metadata:
  name: consul
spec:
  serviceName: consul
  replicas: 3
  selector:
    matchLabels:
      app: consul
  template:
    metadata:
      labels:
        app: consul
    spec:
      containers:
      - name: consul
        image: consul:latest
        ports:
        - containerPort: 8500
        - containerPort: 8600
        args:
        - "agent"
        - "-server"
        - "-bootstrap-expect=3"
        - "-ui"
```

### Setting Configuration via Consul CLI

```bash
# Set values
consul kv put config/myapp/database/host localhost
consul kv put config/myapp/database/port 5432

# Get values
consul kv get config/myapp/database/host

# List all keys
consul kv get -keys config/myapp/

# Delete values
consul kv delete config/myapp/database/host
```

## ACL Setup (Production)

### Enable ACLs

```bash
# In consul config
acl {
  enabled = true
  default_policy = "deny"
  enable_token_persistence = true
}
```

### Create Token for Application

```bash
# Create policy
consul acl policy create \
  -name "myapp-config" \
  -description "Policy for myapp configuration" \
  -rules 'key_prefix "config/myapp/" { policy = "write" }'

# Create token
consul acl token create \
  -description "Token for myapp" \
  -policy-name "myapp-config"
```

## Comparison with Alternatives

| Feature | Consul | Etcd | Vault | ZooKeeper |
|---------|--------|------|-------|-----------|
| KV Store | Yes | Yes | Yes (v2) | Yes |
| Watch Support | Yes | Yes | No | Yes |
| ACL Support | Yes | Yes | Yes | Yes |
| UI | Yes | Limited | Yes | No |
| Service Mesh | Yes | No | No | No |
| Setup | Medium | Low | Medium | Medium |

**Use Consul when:**
- You need service discovery beyond configuration
- You want built-in ACLs and UI
- You're building a microservices architecture
- You need service mesh capabilities

**Prefer alternatives when:**
- **Etcd**: You're using Kubernetes (etcd is native)
- **Vault**: You need secrets management with rotation
- **ZooKeeper**: You have existing ZooKeeper infrastructure

## Testing

### Test Coverage: 60%

This module has comprehensive unit tests covering all business logic and API methods. The uncovered code consists primarily of infrastructure and framework integration code.

**What IS Tested:**
- ✅ All configuration builder validation
- ✅ Get/set/delete configuration operations
- ✅ Configuration caching
- ✅ Listener registration and management
- ✅ Error handling and exception paths
- ✅ Thread safety (concurrent operations)
- ✅ Edge cases and null checks

**What is NOT Tested:**
- ❌ Consul client connection/bootstrap - Requires real Consul server
- ❌ Watch polling mechanism - Background thread integration testing
- ❌ Network I/O and retry logic - Deep within Consul client library

### Running Tests

```bash
mvn test -pl platform-java-config-consul
```

### Integration Tests (Optional)

```bash
# Start Consul
docker run -d -p 8500:8500 --name consul-test consul agent -dev

# Run integration tests
mvn verify -pl platform-java-config-consul

# Stop Consul
docker stop consul-test && docker rm consul-test
```

## Thread Safety

- All public methods are thread-safe
- CopyOnWriteArrayList for listener collections
- ConcurrentHashMap for configuration caching
- Synchronized access to Consul client

## Best Practices

1. **Use Key Prefixes**: Isolate environments with different prefixes
2. **Enable ACLs**: Use tokens for production deployments
3. **Watch Selectively**: Only enable watching when needed
4. **Handle Failures**: Consul may be unavailable during deployment
5. **Cache Appropriately**: Balance freshness vs. Consul load

## Dependencies

This module requires:

- `com.orbitz.consul:consul-client:1.5.3` - Consul Java client
- `org.slf4j:slf4j-api` - Logging facade

## License

Part of platform-java - see main project for license details.
