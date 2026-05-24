# JPlatform Storage - S3

AWS S3/MinIO storage backend for JPlatform volumes.

## Features
- S3-backed persistent storage
- Compatible with AWS S3 and MinIO
- Bucket-based volume isolation
- Size tracking via object metadata

## Usage

```java
S3VolumeConfig config = S3VolumeConfig.builder()
    .endpoint("https://s3.amazonaws.com")
    .accessKey("your-key")
    .secretKey("your-secret")
    .bucketName("jplatform-volumes")
    .region("us-east-1")
    .build();

S3VolumeManager manager = new S3VolumeManager(config, volumeMounts);
Path volumePath = manager.getVolumePath("data");
```

## Status
✅ Production-ready S3 volume implementation.
