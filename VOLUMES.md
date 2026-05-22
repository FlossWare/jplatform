# Persistent State and Data Volumes

## Overview

JPlatform 2.0 introduces **persistent storage volumes**, allowing applications to store data that survives restarts and redeployments. Each application can mount one or more volumes for database files, caches, logs, or any persistent data.

## Key Concepts

### Volume Types

1. **Persistent Volumes**:
   - Data survives application stop/restart
   - Data survives application undeploy (configurable)
   - Suitable for databases, user files, configuration

2. **Ephemeral Volumes**:
   - Data survives application stop/restart within same deployment
   - Data deleted on undeploy
   - Suitable for caches, temporary files, session data

### Volume Properties

Each volume has:
- **Name**: Unique identifier within the application (e.g., "database", "cache")
- **Mount Path**: Logical path for application code (e.g., "/var/myapp/db")
- **Persistent Flag**: true = data survives undeploy, false = ephemeral
- **Size Limit**: Optional maximum size in MB (0 = unlimited)

### Storage Location

Volumes are stored on the filesystem at:

```
/var/jplatform/volumes/{applicationId}/{volumeName}/
```

Example:
```
/var/jplatform/volumes/
  my-app/
    database/       ← Persistent database files
    cache/          ← Ephemeral cache files
    logs/           ← Persistent log files
```

The base path can be customized via system property:
```bash
java -Djplatform.volumes.dir=/custom/path -jar jplatform-launcher.jar
```

## Configuration Examples

### Example 1: Database Volume (Persistent)

Store SQLite database files that survive restarts:

```java
ApplicationDescriptor descriptor = ApplicationDescriptor.builder()
    .applicationId("database-app")
    .mainClass("com.example.DatabaseApp")
    .addVolume(new VolumeMount(
        "database",          // Volume name
        "/var/myapp/db",     // Mount path (logical)
        true,                // Persistent = true
        1024                 // 1GB size limit
    ))
    .build();
```

Application usage:
```java
public class DatabaseApp implements Application {
    @Override
    public void start(ApplicationContext context) {
        Path dbPath = context.getVolumeManager()
            .map(vm -> vm.getVolumePath("database"))
            .orElseThrow();
        
        // Use dbPath for file I/O
        Path dbFile = dbPath.resolve("myapp.db");
        // Initialize SQLite, H2, etc.
    }
}
```

### Example 2: Multiple Volumes

Application with both persistent and ephemeral volumes:

```java
ApplicationDescriptor descriptor = ApplicationDescriptor.builder()
    .applicationId("multi-volume-app")
    .mainClass("com.example.MultiVolumeApp")
    
    // Persistent database (survives undeploy)
    .addVolume(new VolumeMount("database", "/data/db", true, 2048))
    
    // Ephemeral cache (deleted on undeploy)
    .addVolume(new VolumeMount("cache", "/data/cache", false, 512))
    
    // Persistent logs (no size limit)
    .addVolume(new VolumeMount("logs", "/data/logs", true, 0))
    
    .build();
```

Application usage:
```java
public class MultiVolumeApp implements Application {
    @Override
    public void start(ApplicationContext context) {
        VolumeManager vm = context.getVolumeManager().orElseThrow();
        
        Path dbPath = vm.getVolumePath("database");
        Path cachePath = vm.getVolumePath("cache");
        Path logsPath = vm.getVolumePath("logs");
        
        // Initialize services with these paths
        initDatabase(dbPath);
        initCache(cachePath);
        initLogging(logsPath);
    }
}
```

### Example 3: Session Storage

Ephemeral volume for session data:

```java
ApplicationDescriptor descriptor = ApplicationDescriptor.builder()
    .applicationId("web-app")
    .mainClass("com.example.WebApp")
    .addVolume(new VolumeMount(
        "sessions",          // Volume name
        "/var/sessions",     // Mount path
        false,               // Ephemeral = false (deleted on undeploy)
        256                  // 256MB limit
    ))
    .build();
```

### Example 4: No Volumes (Optional Feature)

Applications without volumes work as before:

```java
ApplicationDescriptor descriptor = ApplicationDescriptor.builder()
    .applicationId("stateless-app")
    .mainClass("com.example.StatelessApp")
    // No volumes defined
    .build();

// context.getVolumeManager() returns Optional.empty()
```

## YAML Configuration

Volumes can be configured in YAML descriptors:

```yaml
applicationId: my-app
mainClass: com.example.MyApp
volumes:
  - name: database
    mountPath: /var/myapp/db
    persistent: true
    maxSizeMB: 1024
    
  - name: cache
    mountPath: /var/myapp/cache
    persistent: false
    maxSizeMB: 512
    
  - name: logs
    mountPath: /var/myapp/logs
    persistent: true
    maxSizeMB: 0  # No limit
```

## API Usage

### Accessing Volumes

```java
public class MyApp implements Application {
    @Override
    public void start(ApplicationContext context) throws Exception {
        // Get VolumeManager (Optional)
        Optional<VolumeManager> vmOpt = context.getVolumeManager();
        
        if (vmOpt.isPresent()) {
            VolumeManager vm = vmOpt.get();
            
            // Get volume path
            Path dbPath = vm.getVolumePath("database");
            
            // Check if volume exists and is accessible
            if (vm.volumeExists("database")) {
                // Use the volume
                Files.createFile(dbPath.resolve("data.db"));
            }
            
            // List all volumes
            List<VolumeMount> volumes = vm.getVolumes();
            for (VolumeMount vol : volumes) {
                System.out.println("Volume: " + vol.getName() + 
                    " at " + vm.getVolumePath(vol.getName()));
            }
            
            // Check volume usage
            long usageBytes = vm.getVolumeUsageBytes("database");
            long limitBytes = vm.getVolumeSizeLimit("database");
            System.out.println("Database using " + usageBytes + " of " + limitBytes + " bytes");
            
            // Check if persistent
            boolean isPersistent = vm.isPersistent("database");
        } else {
            // No volumes defined for this application
            System.out.println("Application has no volumes");
        }
    }
}
```

### Volume Operations

```java
VolumeManager vm = context.getVolumeManager().orElseThrow();

// Get absolute path to volume directory
Path volumePath = vm.getVolumePath("database");

// Write files
Path dataFile = volumePath.resolve("data.json");
Files.writeString(dataFile, "{\"key\": \"value\"}");

// Read files
String content = Files.readString(dataFile);

// Create subdirectories
Path subdir = volumePath.resolve("subdirectory");
Files.createDirectory(subdir);

// Check volume size
long usage = vm.getVolumeUsageBytes("database");
long limit = vm.getVolumeSizeLimit("database");
if (limit > 0 && usage > limit) {
    throw new IOException("Volume quota exceeded");
}

// List all volumes
for (VolumeMount vol : vm.getVolumes()) {
    System.out.println(vol.getName() + " persistent=" + vol.isPersistent());
}
```

## Volume Lifecycle

### Deployment

When an application is deployed:

1. Platform reads volume definitions from ApplicationDescriptor
2. Creates FileSystemVolumeManager for the application
3. Creates volume directories at `/var/jplatform/volumes/{appId}/{volumeName}`
4. Adds VolumeManager to ApplicationContext

```java
// During ApplicationManager.deploy()
VolumeManager volumeManager = new FileSystemVolumeManager(
    applicationId, 
    descriptor.getVolumes()
);

ApplicationContext context = ApplicationContextImpl.builder()
    .volumeManager(volumeManager)
    .build();
```

### Start/Stop

Volumes persist across start/stop operations:

```
deploy()  → volumes created
start()   → application accesses volumes
stop()    → volumes remain intact
start()   → application accesses same data
```

### Undeploy

When an application is undeployed:

- **Ephemeral volumes**: Automatically deleted
- **Persistent volumes**: Kept on disk (by default)

```java
// During ApplicationManager.undeploy()
volumeManager.cleanupEphemeralVolumes();
// Persistent volumes remain at /var/jplatform/volumes/{appId}/
```

### Redeploy

When redeploying an application with persistent volumes:

1. New deployment creates VolumeManager
2. Volume directories already exist (from previous deployment)
3. Application accesses existing data

```
undeploy()   → ephemeral deleted, persistent kept
deploy()     → volume directories reused
start()      → application sees previous data
```

## Volume Management

### Size Limits

Volumes can have optional size limits:

```java
new VolumeMount("database", "/data/db", true, 1024)  // 1GB limit
```

Check usage against limit:

```java
VolumeManager vm = context.getVolumeManager().orElseThrow();
long usage = vm.getVolumeUsageBytes("database");
long limit = vm.getVolumeSizeLimit("database");

if (limit > 0 && usage > limit * 0.9) {
    logger.warn("Volume database at 90% capacity: {} / {} bytes", usage, limit);
}
```

### Volume Cleanup

**Automatic Cleanup** (ephemeral volumes):
- Deleted automatically on undeploy
- No manual intervention required

**Manual Cleanup** (persistent volumes):

```java
// In FileSystemVolumeManager
public void cleanupAllVolumes() throws IOException {
    // Deletes ALL volumes (including persistent)
    // Use with caution!
}
```

Platform administrators can manually delete persistent volumes:

```bash
# List volumes
ls /var/jplatform/volumes/

# Delete specific application's volumes
rm -rf /var/jplatform/volumes/my-app/

# Delete specific volume
rm -rf /var/jplatform/volumes/my-app/old-cache/
```

### Monitoring Volume Usage

Track volume usage over time:

```java
VolumeManager vm = context.getVolumeManager().orElseThrow();

// Periodic monitoring
ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
scheduler.scheduleAtFixedRate(() -> {
    for (VolumeMount vol : vm.getVolumes()) {
        try {
            long usage = vm.getVolumeUsageBytes(vol.getName());
            long limit = vm.getVolumeSizeLimit(vol.getName());
            
            double percentUsed = limit > 0 ? (usage * 100.0 / limit) : 0;
            logger.info("Volume {}: {} bytes ({:.1f}%)",
                vol.getName(), usage, percentUsed);
                
            if (limit > 0 && usage > limit) {
                logger.error("Volume {} exceeded size limit!", vol.getName());
            }
        } catch (IOException e) {
            logger.error("Failed to check volume usage", e);
        }
    }
}, 0, 60, TimeUnit.SECONDS);  // Every 60 seconds
```

## Best Practices

### 1. Choose the Right Volume Type

- **Persistent**: Databases, user uploads, configuration files, audit logs
- **Ephemeral**: Caches, temporary files, session data, build artifacts

### 2. Set Appropriate Size Limits

```java
// Database: set realistic limit based on expected growth
.addVolume(new VolumeMount("database", "/data/db", true, 10240))  // 10GB

// Cache: limit to prevent unbounded growth
.addVolume(new VolumeMount("cache", "/data/cache", false, 512))   // 512MB

// Logs: no limit, but implement log rotation
.addVolume(new VolumeMount("logs", "/data/logs", true, 0))        // Unlimited
```

### 3. Implement Volume Usage Monitoring

Track usage and alert when approaching limits:

```java
long usage = vm.getVolumeUsageBytes("database");
long limit = vm.getVolumeSizeLimit("database");

if (limit > 0 && usage > limit * 0.8) {
    alerting.sendWarning("Database volume at 80% capacity");
}
```

### 4. Handle Volume Errors

```java
try {
    Path dbPath = vm.getVolumePath("database");
    Files.writeString(dbPath.resolve("data.txt"), content);
} catch (NoSuchFileException e) {
    logger.error("Volume directory does not exist", e);
} catch (FileSystemException e) {
    logger.error("Disk full or permission denied", e);
} catch (IOException e) {
    logger.error("Failed to write to volume", e);
}
```

### 5. Clean Up Old Data

For persistent volumes, implement data retention policies:

```java
public void cleanupOldLogs(VolumeManager vm) throws IOException {
    Path logsPath = vm.getVolumePath("logs");
    long cutoffTime = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(30);
    
    Files.walk(logsPath)
        .filter(Files::isRegularFile)
        .filter(path -> {
            try {
                return Files.getLastModifiedTime(path).toMillis() < cutoffTime;
            } catch (IOException e) {
                return false;
            }
        })
        .forEach(path -> {
            try {
                Files.delete(path);
            } catch (IOException e) {
                logger.warn("Failed to delete old log: {}", path, e);
            }
        });
}
```

### 6. Backup Persistent Volumes

Implement backup strategies for critical data:

```bash
#!/bin/bash
# Backup script for persistent volumes
BACKUP_DIR=/backups/jplatform
DATE=$(date +%Y%m%d)

# Backup all persistent volumes
for app in /var/jplatform/volumes/*; do
    app_id=$(basename $app)
    tar czf $BACKUP_DIR/${app_id}-${DATE}.tar.gz $app
done

# Retain only last 7 days
find $BACKUP_DIR -name "*.tar.gz" -mtime +7 -delete
```

## Architecture

### Components

1. **VolumeMount** (in `jplatform-api`):
   - Value object describing volume configuration
   - Immutable with validation

2. **VolumeManager** (interface in `jplatform-api`):
   - Defines volume management operations
   - Accessed via `ApplicationContext.getVolumeManager()`

3. **FileSystemVolumeManager** (in `jplatform-storage`):
   - Filesystem-based VolumeManager implementation
   - Uses Java NIO for directory operations
   - Thread-safe using ConcurrentHashMap

4. **ApplicationDescriptor** (in `jplatform-api`):
   - Extended with volumes list
   - Builder pattern with `addVolume()` method

5. **ApplicationContext** (in `jplatform-api`):
   - Extended with `getVolumeManager()` method
   - Returns `Optional<VolumeManager>`

6. **ApplicationManager** (in `jplatform-core`):
   - Creates FileSystemVolumeManager during deploy
   - Cleans up ephemeral volumes on undeploy

### Data Flow

```
ApplicationDescriptor.getVolumes()
  ↓
ApplicationManager.deploy()
  ↓
new FileSystemVolumeManager(appId, volumes)
  ↓
Creates directories at /var/jplatform/volumes/{appId}/{volumeName}
  ↓
ApplicationContextImpl.Builder.volumeManager(vm)
  ↓
ApplicationContext.getVolumeManager() → Optional<VolumeManager>
  ↓
Application code: vm.getVolumePath("database")
  ↓
File I/O operations
```

## API Reference

### VolumeMount

```java
package org.flossware.jplatform.api;

public class VolumeMount {
    public VolumeMount(String name, String mountPath, boolean persistent, long maxSizeMB);
    
    public String getName();
    public String getMountPath();
    public boolean isPersistent();
    public long getMaxSizeMB();
    public boolean hasSizeLimit();  // true if maxSizeMB > 0
}
```

### VolumeManager Interface

```java
package org.flossware.jplatform.api;

public interface VolumeManager {
    Path getVolumePath(String volumeName);
    List<VolumeMount> getVolumes();
    long getVolumeUsageBytes(String volumeName) throws IOException;
    boolean volumeExists(String volumeName);
    long getVolumeSizeLimit(String volumeName);  // In bytes
    boolean isPersistent(String volumeName);
}
```

### FileSystemVolumeManager

```java
package org.flossware.jplatform.storage;

public class FileSystemVolumeManager implements VolumeManager {
    public FileSystemVolumeManager(String applicationId, List<VolumeMount> volumes) throws IOException;
    public FileSystemVolumeManager(String applicationId, List<VolumeMount> volumes, Path basePath) throws IOException;
    
    // VolumeManager methods...
    
    // Management methods
    public void cleanupEphemeralVolumes() throws IOException;
    public void cleanupAllVolumes() throws IOException;
    
    // Getters
    public String getApplicationId();
    public Path getBasePath();
}
```

### ApplicationContext Extension

```java
package org.flossware.jplatform.api;

public interface ApplicationContext {
    // Existing methods...
    
    /**
     * Returns the volume manager if volumes are defined for this application.
     * @return optional volume manager, empty if no volumes are defined
     * @since 2.0
     */
    Optional<VolumeManager> getVolumeManager();
}
```

## Migration from JPlatform 1.0

Applications without volumes continue to work without changes:

```java
// JPlatform 1.0 application (no volumes)
public class MyApp implements Application {
    @Override
    public void start(ApplicationContext context) {
        // context.getVolumeManager() returns Optional.empty()
    }
}
```

Add volumes incrementally:

```java
// JPlatform 2.0 application (with volumes)
ApplicationDescriptor descriptor = ApplicationDescriptor.builder()
    .applicationId("my-app")
    .mainClass("com.example.MyApp")
    .addVolume(new VolumeMount("data", "/var/data", true, 1024))  // NEW
    .build();

public class MyApp implements Application {
    @Override
    public void start(ApplicationContext context) {
        context.getVolumeManager().ifPresent(vm -> {
            Path dataPath = vm.getVolumePath("data");
            // Use volume
        });
    }
}
```

## Troubleshooting

### Volume Directory Not Found

**Cause**: Volume name mismatch or volumes not initialized.

**Solution**:
1. Check volume name matches descriptor: `vm.getVolumePath("database")`
2. Verify `getVolumeManager()` returns `Optional` with value
3. Check logs for volume creation errors

### Permission Denied

**Cause**: Insufficient filesystem permissions.

**Solution**:
1. Ensure platform has write access to `/var/jplatform/volumes/`
2. Check directory ownership: `chown -R jplatform:jplatform /var/jplatform/volumes`
3. Check SELinux/AppArmor policies

### Volume Quota Exceeded

**Cause**: Application wrote more data than `maxSizeMB` limit.

**Solution**:
1. Increase size limit in descriptor
2. Implement data cleanup in application
3. Monitor usage and alert before hitting limit

### Persistent Data Lost

**Cause**: Volume was marked ephemeral instead of persistent.

**Solution**:
1. Verify `persistent=true` in VolumeMount constructor
2. Check volume definition in YAML: `persistent: true`
3. Review logs for "Deleted ephemeral volume" messages

## See Also

- [Resource Enforcement](RESOURCE_ENFORCEMENT.md) - Automatic enforcement of resource limits
- [Application Lifecycle](LIFECYCLE.md) - Application states and transitions
- [Configuration Guide](CONFIGURATION.md) - YAML/JSON descriptor format
