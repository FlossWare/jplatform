# JPlatform Config - ZooKeeper

Apache ZooKeeper configuration source for JPlatform. Provides dynamic, distributed configuration management with hierarchical key-value storage and real-time change notifications.

## Features

- ZooKeeper-based distributed configuration
- Hierarchical key structure (app.database.host)
- Real-time configuration updates via watchers
- Automatic reconnection handling
- Thread-safe implementation
- Configuration change listeners

## Maven Dependency

```xml
<dependency>
    <groupId>org.flossware.jplatform</groupId>
    <artifactId>jplatform-config-zookeeper</artifactId>
    <version>1.1</version>
</dependency>
```

## Quick Start

### Basic Configuration

```java
ZooKeeperConfigSourceConfig config = ZooKeeperConfigSourceConfig.builder()
    .connectString("localhost:2181")
    .basePath("/config/myapp")
    .build();

ZooKeeperConfigSource source = new ZooKeeperConfigSource(config);
source.start();

source.setConfig("database.host", "localhost");
source.setConfig("database.port", "5432");

String host = source.getConfig("database.host");
Map<String, String> allConfig = source.loadConfig();
```

### ZooKeeper Ensemble

```java
ZooKeeperConfigSourceConfig config = ZooKeeperConfigSourceConfig.builder()
    .connectString("zk1:2181,zk2:2181,zk3:2181")
    .basePath("/config/production")
    .sessionTimeoutMs(30000)
    .connectionTimeoutMs(10000)
    .retryCount(5)
    .build();
```

### Configuration Listeners

```java
source.addListener("app-config-listener", updatedConfig -> {
    System.out.println("Configuration updated!");
    System.out.println("Database host: " + updatedConfig.get("database.host"));
});

source.setConfig("database.host", "db-server");
```

## Configuration

### ZooKeeperConfigSourceConfig Options

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| connectString | String | localhost:2181 | ZooKeeper connection string |
| basePath | String | /config | Base path for configuration keys |
| sessionTimeoutMs | int | 60000 | Session timeout in milliseconds |
| connectionTimeoutMs | int | 15000 | Connection timeout in milliseconds |
| retryCount | int | 3 | Number of connection retry attempts |
| retryIntervalMs | int | 1000 | Interval between retry attempts |

## Architecture

### Hierarchical Key Structure

Keys use dot notation and are mapped to ZooKeeper paths:
- Key: `app.database.host`
- ZooKeeper path: `/config/app/database/host`

### Configuration Watching

The implementation uses Apache Curator's TreeCache to watch for changes:
1. Configuration changes in ZooKeeper trigger events
2. Local cache is updated automatically
3. Registered listeners are notified with updated configuration

### Thread Safety

- All mutable state uses ConcurrentHashMap
- Configuration cache is thread-safe
- Multiple threads can read/write configuration safely

## Usage Patterns

### Environment-Specific Configuration

```java
String env = System.getenv("ENVIRONMENT");
String basePath = "/config/" + env;

ZooKeeperConfigSourceConfig config = ZooKeeperConfigSourceConfig.builder()
    .basePath(basePath)
    .build();
```

### Hierarchical Configuration

```java
source.setConfig("app.server.port", "8080");
source.setConfig("app.server.host", "0.0.0.0");
source.setConfig("app.database.url", "jdbc:postgresql://localhost/db");
source.setConfig("app.database.pool.size", "10");

Map<String, String> allConfig = source.loadConfig();
```

### Dynamic Reconfiguration

```java
source.addListener("reconfig", config -> {
    int newPoolSize = Integer.parseInt(config.get("database.pool.size"));
    updateConnectionPool(newPoolSize);
});
```

### Resource Cleanup

```java
try (ZooKeeperConfigSource source = new ZooKeeperConfigSource(config)) {
    source.start();
    // Use configuration source
}
```

## Testing

The module includes comprehensive tests using Curator's TestingServer:
- Configuration CRUD operations
- Listener functionality
- Hierarchical key handling
- Error handling
- Thread safety

Run tests:
```bash
mvn test -pl jplatform-config-zookeeper
```

## Dependencies

- Apache Curator Framework 5.5.0
- Apache Curator Recipes 5.5.0
- Jackson Databind (for JSON support)

## Status

Production-ready ZooKeeper configuration implementation with real-time change notifications.
