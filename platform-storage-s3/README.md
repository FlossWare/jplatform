# platform-java Storage - S3

AWS S3/MinIO storage backend for platform-java volumes. Provides persistent, scalable object storage with support for AWS S3 and S3-compatible services.

## Features

- AWS S3 and MinIO compatibility
- Persistent volume storage with local caching
- Automatic bucket management
- Usage tracking and size limits
- Thread-safe implementation
- Upload/download synchronization
- Key prefix support for multi-environment isolation

## Maven Dependency

```xml
<dependency>
    <groupId>org.flossware.platform-java</groupId>
    <artifactId>platform-java-storage-s3</artifactId>
    <version>1.1</version>
</dependency>
```

## Quick Start

### AWS S3 Configuration

```java
S3StorageConfig config = S3StorageConfig.builder()
    .region("us-east-1")
    .accessKey("AKIA...")
    .secretKey("your-secret-key")
    .bucketName("my-app-volumes")
    .build();

List<VolumeMount> volumes = Arrays.asList(
    new VolumeMount("data", "/app/data", true, 1024),
    new VolumeMount("logs", "/app/logs", true, 0)
);

S3VolumeManager manager = new S3VolumeManager(config, volumes);
manager.start();

Path dataPath = manager.getVolumePath("data");
Files.writeString(dataPath.resolve("config.json"), "{}");
manager.uploadFile("data", "config.json");
```

### MinIO Configuration

```java
S3StorageConfig config = S3StorageConfig.builder()
    .endpoint("http://localhost:9000")
    .accessKey("minioadmin")
    .secretKey("minioadmin")
    .bucketName("platform-java")
    .pathStyleAccess(true)
    .build();

S3VolumeManager manager = new S3VolumeManager(config, volumes);
manager.start();
```

### Multi-Environment Isolation

```java
S3StorageConfig devConfig = S3StorageConfig.builder()
    .accessKey("key")
    .secretKey("secret")
    .bucketName("shared-bucket")
    .keyPrefix("dev/")
    .build();

S3StorageConfig prodConfig = S3StorageConfig.builder()
    .accessKey("key")
    .secretKey("secret")
    .bucketName("shared-bucket")
    .keyPrefix("prod/")
    .build();
```

## Configuration

### S3StorageConfig Options

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| endpoint | String | null | S3 endpoint URL (null for AWS S3) |
| region | String | us-east-1 | AWS region |
| accessKey | String | required | AWS access key ID |
| secretKey | String | required | AWS secret access key |
| bucketName | String | required | S3 bucket name |
| pathStyleAccess | boolean | false | Use path-style URLs (required for MinIO) |
| keyPrefix | String | null | Key prefix for environment isolation |

## Architecture

### Volume Management

Each volume is mapped to a prefix in the S3 bucket:
- Volume "data" -> s3://bucket/data/
- Volume "logs" -> s3://bucket/logs/

With a key prefix "prod/":
- Volume "data" -> s3://bucket/prod/data/
- Volume "logs" -> s3://bucket/prod/logs/

### Local Caching

Files are cached locally in the system temp directory under `s3-volumes/`:
- Reads from volume path access local cache
- Call `uploadFile()` to sync to S3
- Call `downloadFile()` to sync from S3

### Thread Safety

- All mutable state uses ConcurrentHashMap
- Volume path creation is thread-safe via computeIfAbsent
- S3 client is thread-safe per AWS SDK design

## Usage Patterns

### Basic Volume Operations

```java
Path volumePath = manager.getVolumePath("data");
boolean exists = manager.volumeExists("data");
long usage = manager.getVolumeUsageBytes("data");
long limit = manager.getVolumeSizeLimit("data");
boolean persistent = manager.isPersistent("data");
```

### File Upload/Download

```java
Path localPath = manager.getVolumePath("data");
Files.writeString(localPath.resolve("file.txt"), "content");
manager.uploadFile("data", "file.txt");

manager.downloadFile("data", "backup.txt");
String content = Files.readString(localPath.resolve("backup.txt"));
```

### Resource Cleanup

```java
try (S3VolumeManager manager = new S3VolumeManager(config, volumes)) {
    manager.start();
    // Use manager
}
```

## Testing

The module includes comprehensive tests with 60%+ coverage:
- Configuration validation tests
- Volume operations tests
- S3 client integration tests (mocked)
- Error handling tests
- Thread safety tests

Run tests:
```bash
mvn test -pl platform-java-storage-s3
```

## Status

Production-ready S3 volume implementation.
