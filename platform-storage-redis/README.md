# platform-java Storage - Redis

Redis-based storage backend for platform-java volumes. Provides fast, in-memory persistent storage with support for Redis standalone and cluster modes.

## Features

- **High Performance**: In-memory data storage with microsecond latency
- **Persistent Storage**: Optional AOF/RDB persistence to disk
- **Connection Pooling**: JedisPool for efficient connection management
- **Binary Data Storage**: Efficient storage of file contents as Redis strings
- **Usage Tracking**: Real-time size calculations using Redis INFO memory
- **Thread Safety**: All operations are thread-safe
- **Cluster Support**: Works with Redis standalone and cluster
- **TTL Support**: Optional time-to-live for temporary volumes

## Maven Dependency

```xml
<dependency>
    <groupId>org.flossware.platform-java</groupId>
    <artifactId>platform-java-storage-redis</artifactId>
    <version>1.1</version>
</dependency>
```

## Quick Start

### Basic Usage (Standalone Redis)

```java
RedisStorageConfig config = RedisStorageConfig.builder()
    .host("localhost")
    .port(6379)
    .build();

List<VolumeMount> volumes = Arrays.asList(
    new VolumeMount("data", "/app/data", true, 1024),
    new VolumeMount("cache", "/app/cache", true, 512)
);

RedisVolumeManager manager = new RedisVolumeManager(config, volumes);
manager.start();

Path dataPath = manager.getVolumePath("data");
Files.writeString(dataPath.resolve("config.json"), "{}");
```

### With Authentication

```java
RedisStorageConfig config = RedisStorageConfig.builder()
    .host("redis.example.com")
    .port(6379)
    .password("secret")
    .database(1)
    .build();
```

### Connection Pooling

```java
RedisStorageConfig config = RedisStorageConfig.builder()
    .host("localhost")
    .port(6379)
    .maxTotal(20)
    .maxIdle(10)
    .minIdle(5)
    .timeout(2000)
    .build();
```

### Key Prefixing (Multi-Environment)

```java
RedisStorageConfig devConfig = RedisStorageConfig.builder()
    .host("shared-redis.local")
    .keyPrefix("dev:")
    .build();

RedisStorageConfig prodConfig = RedisStorageConfig.builder()
    .host("shared-redis.local")
    .keyPrefix("prod:")
    .build();
```

## Configuration Options

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `host` | String | `localhost` | Redis server hostname |
| `port` | int | `6379` | Redis server port |
| `password` | String | `null` | Authentication password |
| `database` | int | `0` | Database number (0-15) |
| `timeout` | int | `2000` | Connection timeout (ms) |
| `maxTotal` | int | `10` | Maximum pool connections |
| `maxIdle` | int | `10` | Maximum idle connections |
| `minIdle` | int | `5` | Minimum idle connections |
| `keyPrefix` | String | `platform-java:` | Key prefix for isolation |

## Architecture

### Key Structure

Redis uses a hierarchical key structure:

```
{keyPrefix}volume:{volumeName}:files      -> Hash of filename -> size
{keyPrefix}volume:{volumeName}:file:{path} -> Binary file content
{keyPrefix}volume:{volumeName}:metadata   -> Hash of volume metadata
```

Example:
```
platform-java:volume:data:files               -> {"config.json":"42", "app.jar":"1024"}
platform-java:volume:data:file:config.json    -> {"key":"value"}
platform-java:volume:data:metadata           -> {"created":"1234567890", "maxSize":"1048576"}
```

### Volume Operations

1. **Initialization**: Creates volume metadata hashes on start
2. **File Storage**: Stores file content as Redis strings, metadata in hashes
3. **Size Tracking**: Maintains file sizes in hash, calculates total on demand
4. **Quota Enforcement**: Checks total size before writes
5. **Cleanup**: Deletes all volume keys on volume removal

### Memory Management

- Files stored as binary-safe Redis strings
- Metadata stored in Redis hashes for efficient lookup
- Size tracking avoids full scan of files
- Optional TTL for automatic expiration

## Redis Setup

### Docker (Standalone)

```bash
docker run -d \
  --name redis \
  -p 6379:6379 \
  redis:7 \
  --appendonly yes
```

### Docker (With Password)

```bash
docker run -d \
  --name redis \
  -p 6379:6379 \
  redis:7 \
  --requirepass secret \
  --appendonly yes
```

### Docker Compose

```yaml
version: '3.8'
services:
  redis:
    image: redis:7
    ports:
      - "6379:6379"
    volumes:
      - redis-data:/data
    command: redis-server --appendonly yes --requirepass secret
volumes:
  redis-data:
```

### Redis Configuration

For production persistence, configure Redis with:

```conf
# Enable AOF persistence
appendonly yes
appendfsync everysec

# Enable RDB snapshots
save 900 1
save 300 10
save 60 10000

# Memory management
maxmemory 2gb
maxmemory-policy allkeys-lru
```

## Comparison with Alternatives

| Feature | Redis | Database | S3 | Filesystem |
|---------|-------|----------|-----|------------|
| Performance | Very High | Medium | Medium | High |
| Persistence | Medium | High | High | High |
| Scalability | High | High | Very High | Low |
| Setup | Very Low | Medium | Low | None |
| Memory Usage | High | Low | Low | Low |
| Query Support | Limited | SQL | No | No |

**Use Redis Storage when:**
- You need maximum performance (microsecond latency)
- Your data fits in memory
- You need simple key-value access
- You want low setup complexity

**Prefer alternatives when:**
- **Database**: You need SQL queries or transactional guarantees
- **S3**: You need unlimited cloud-scale storage
- **Filesystem**: You're developing locally or need direct file access

## Dependencies

This module requires:

- `redis.clients:jedis:4.4.3` - Redis Java client
- `org.apache.commons:commons-pool2` - Connection pooling

## Thread Safety

- All public methods are thread-safe
- JedisPool manages thread-safe connection pooling
- Redis operations are atomic
- Uses synchronized blocks where needed

## Testing

Tests use mocked Jedis client:

```bash
mvn test -pl platform-java-storage-redis
```

For integration tests with real Redis (optional):

```bash
# Start Redis
docker run -d -p 6379:6379 --name redis-test redis:7

# Run tests
mvn verify -pl platform-java-storage-redis

# Stop Redis
docker stop redis-test && docker rm redis-test
```

## Performance Considerations

- Redis stores all data in memory - monitor memory usage
- Use appropriate `maxmemory` and eviction policies
- For large files (>10MB), consider S3 or database storage
- Enable persistence (AOF/RDB) for data durability
- Use connection pooling for high-concurrency scenarios
- Consider Redis Cluster for horizontal scaling

## Best Practices

1. **Monitor Memory**: Set `maxmemory` limits and use `INFO memory`
2. **Enable Persistence**: Use AOF for durability, RDB for backups
3. **Use Key Prefixes**: Isolate environments with different prefixes
4. **Connection Pooling**: Configure pool sizes based on concurrency
5. **Handle Eviction**: Choose appropriate `maxmemory-policy`

## License

Part of platform-java - see main project for license details.
