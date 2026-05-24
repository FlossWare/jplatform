# JPlatform Storage - Database

JDBC-based storage backend for JPlatform volumes. Provides persistent, transactional storage using relational databases with support for H2, PostgreSQL, and MySQL.

## Features

- **Multi-Database Support**: H2, PostgreSQL, MySQL/MariaDB compatibility
- **Connection Pooling**: HikariCP for efficient connection management
- **Transactional Storage**: ACID-compliant volume operations
- **Schema Management**: Automatic table creation and schema initialization
- **Binary Data Storage**: Efficient BLOB storage for file contents
- **Usage Tracking**: Real-time size calculations and quota enforcement
- **Thread Safety**: All operations are thread-safe
- **In-Memory Mode**: H2 embedded database for testing and development

## Maven Dependency

```xml
<dependency>
    <groupId>org.flossware.jplatform</groupId>
    <artifactId>jplatform-storage-database</artifactId>
    <version>1.1</version>
</dependency>
```

## Quick Start

### H2 In-Memory (Development)

```java
DatabaseStorageConfig config = DatabaseStorageConfig.builder()
    .jdbcUrl("jdbc:h2:mem:jplatform")
    .username("sa")
    .password("")
    .build();

List<VolumeMount> volumes = Arrays.asList(
    new VolumeMount("data", "/app/data", true, 1024),
    new VolumeMount("logs", "/app/logs", true, 0)
);

DatabaseVolumeManager manager = new DatabaseVolumeManager(config, volumes);
manager.start();

Path dataPath = manager.getVolumePath("data");
Files.writeString(dataPath.resolve("config.json"), "{}");
```

### H2 Persistent File

```java
DatabaseStorageConfig config = DatabaseStorageConfig.builder()
    .jdbcUrl("jdbc:h2:./data/jplatform")
    .username("sa")
    .password("")
    .build();
```

### PostgreSQL

```java
DatabaseStorageConfig config = DatabaseStorageConfig.builder()
    .jdbcUrl("jdbc:postgresql://localhost:5432/jplatform")
    .username("jplatform")
    .password("secret")
    .driverClassName("org.postgresql.Driver")
    .build();
```

### MySQL

```java
DatabaseStorageConfig config = DatabaseStorageConfig.builder()
    .jdbcUrl("jdbc:mysql://localhost:3306/jplatform")
    .username("jplatform")
    .password("secret")
    .driverClassName("com.mysql.cj.jdbc.Driver")
    .build();
```

### Connection Pooling Configuration

```java
DatabaseStorageConfig config = DatabaseStorageConfig.builder()
    .jdbcUrl("jdbc:postgresql://localhost:5432/jplatform")
    .username("jplatform")
    .password("secret")
    .maximumPoolSize(20)
    .minimumIdle(5)
    .connectionTimeout(30000)
    .build();
```

## Configuration Options

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `jdbcUrl` | String | required | JDBC connection URL |
| `username` | String | required | Database username |
| `password` | String | required | Database password |
| `driverClassName` | String | auto-detected | JDBC driver class name |
| `maximumPoolSize` | int | 10 | Maximum connections in pool |
| `minimumIdle` | int | 5 | Minimum idle connections |
| `connectionTimeout` | long | 30000 | Connection timeout (ms) |

## Architecture

### Database Schema

The volume manager creates two tables:

```sql
CREATE TABLE volumes (
    volume_name VARCHAR(255) PRIMARY KEY,
    created_at TIMESTAMP NOT NULL,
    max_size_bytes BIGINT NOT NULL
);

CREATE TABLE volume_data (
    volume_name VARCHAR(255) NOT NULL,
    file_path VARCHAR(1024) NOT NULL,
    file_data BLOB NOT NULL,
    file_size BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    PRIMARY KEY (volume_name, file_path),
    FOREIGN KEY (volume_name) REFERENCES volumes(volume_name) ON DELETE CASCADE
);

CREATE INDEX idx_volume_data_volume ON volume_data(volume_name);
```

### Volume Operations

1. **Initialization**: Creates schema and volume entries on start
2. **File Storage**: Stores file content as BLOBs with metadata
3. **Size Tracking**: Calculates total size using SUM(file_size)
4. **Quota Enforcement**: Checks size before writes
5. **Cleanup**: Cascading deletes on volume removal

### Connection Pooling

Uses HikariCP for efficient connection management:
- Pre-configured connection pool
- Automatic connection validation
- Leak detection and recovery
- Configurable pool sizing

## Database Setup

### PostgreSQL

```bash
# Create database
createdb jplatform

# Create user
psql -c "CREATE USER jplatform WITH PASSWORD 'secret';"
psql -c "GRANT ALL PRIVILEGES ON DATABASE jplatform TO jplatform;"
```

### MySQL

```bash
mysql -e "CREATE DATABASE jplatform CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
mysql -e "CREATE USER 'jplatform'@'localhost' IDENTIFIED BY 'secret';"
mysql -e "GRANT ALL PRIVILEGES ON jplatform.* TO 'jplatform'@'localhost';"
mysql -e "FLUSH PRIVILEGES;"
```

### Docker PostgreSQL

```bash
docker run -d \
  --name postgres \
  -e POSTGRES_DB=jplatform \
  -e POSTGRES_USER=jplatform \
  -e POSTGRES_PASSWORD=secret \
  -p 5432:5432 \
  postgres:15
```

### Docker MySQL

```bash
docker run -d \
  --name mysql \
  -e MYSQL_DATABASE=jplatform \
  -e MYSQL_USER=jplatform \
  -e MYSQL_PASSWORD=secret \
  -e MYSQL_ROOT_PASSWORD=rootsecret \
  -p 3306:3306 \
  mysql:8
```

## Comparison with Alternatives

| Feature | Database | S3 | Redis | Filesystem |
|---------|----------|-----|-------|------------|
| Persistence | High | High | Medium | High |
| Transactions | Yes | No | Limited | No |
| Query Support | SQL | No | Limited | No |
| Performance | Medium | Medium | Very High | High |
| Scalability | High | Very High | High | Low |
| Setup | Medium | Low | Very Low | None |

**Use Database Storage when:**
- You need transactional guarantees
- You want to query volume metadata with SQL
- You have existing database infrastructure
- You need strong consistency

**Prefer alternatives when:**
- **S3**: You need cloud-native object storage at scale
- **Redis**: You need maximum performance and simple key-value access
- **Filesystem**: You're developing locally or need direct file access

## Dependencies

This module requires:

- `com.zaxxer:HikariCP:5.0.1` - Connection pooling
- `com.h2database:h2:2.2.220` - Embedded database (optional)
- `org.postgresql:postgresql:42.6.0` - PostgreSQL driver (optional)
- `com.mysql:mysql-connector-j:8.0.33` - MySQL driver (optional)

## Thread Safety

- All public methods are thread-safe
- HikariCP manages thread-safe connection pooling
- Uses synchronized blocks for critical sections
- Database-level locking for concurrent operations

## Testing

Tests use H2 in-memory database:

```bash
mvn test -pl jplatform-storage-database
```

## Performance Considerations

- Use connection pooling for high-concurrency scenarios
- Consider database-specific tuning (indexes, cache sizes)
- BLOBs are efficient for files up to 10MB
- For very large files (>100MB), consider S3 storage instead
- PostgreSQL generally offers better performance than MySQL for BLOBs

## License

Part of JPlatform - see main project for license details.
