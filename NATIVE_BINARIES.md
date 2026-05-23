# Native Binary Support Guide

## Overview

JPlatform supports loading native libraries (`.so`, `.dll`, `.dylib`) and deploying GraalVM native images. Native libraries are extracted to isolated directories per application, ensuring proper isolation and avoiding library conflicts.

## Platform Detection

JPlatform automatically detects the current platform using Java system properties:

### Supported Platforms

| Platform | OS | Architecture |
|----------|-----|--------------|
| `LINUX_X64` | Linux | x86_64 / amd64 |
| `LINUX_ARM64` | Linux | aarch64 / arm64 |
| `WINDOWS_X64` | Windows | x86_64 / amd64 |
| `WINDOWS_ARM64` | Windows | aarch64 / arm64 |
| `MACOS_X64` | macOS | x86_64 / amd64 |
| `MACOS_ARM64` | macOS | aarch64 / arm64 (Apple Silicon) |
| `ANY` | Any | Any (platform-independent) |

### Detection Algorithm

```java
String osName = System.getProperty("os.name").toLowerCase();
String osArch = System.getProperty("os.arch").toLowerCase();

// Example outputs:
// Linux x86_64 → LINUX_X64
// Windows 10 amd64 → WINDOWS_X64
// Mac OS X aarch64 → MACOS_ARM64
```

## Declaring Native Libraries

### In Java (ApplicationDescriptor)

```java
import org.flossware.jplatform.api.NativeLibrary;
import org.flossware.jplatform.api.Platform;

ApplicationDescriptor descriptor = ApplicationDescriptor.builder()
    .applicationId("sqlite-app")
    .mainClass("com.example.SqliteApp")
    .addClasspathEntry("file:///path/to/app.jar")
    
    // Linux x86_64
    .addNativeLibrary(new NativeLibrary(
        "sqlite3",                              // Library name
        Platform.LINUX_X64,                     // Platform
        "file:///libs/linux-x64/libsqlite3.so"  // Path
    ))
    
    // Windows x86_64
    .addNativeLibrary(new NativeLibrary(
        "sqlite3",
        Platform.WINDOWS_X64,
        "file:///libs/windows-x64/sqlite3.dll"
    ))
    
    // macOS ARM64 (Apple Silicon)
    .addNativeLibrary(new NativeLibrary(
        "sqlite3",
        Platform.MACOS_ARM64,
        "file:///libs/macos-arm64/libsqlite3.dylib"
    ))
    
    .build();
```

### In YAML

```yaml
applicationId: sqlite-app
mainClass: com.example.SqliteApp
classpathEntries:
  - file:///path/to/app.jar

nativeLibraries:
  - name: sqlite3
    platform: LINUX_X64
    libraryPath: file:///libs/linux-x64/libsqlite3.so
    
  - name: sqlite3
    platform: WINDOWS_X64
    libraryPath: file:///libs/windows-x64/sqlite3.dll
    
  - name: sqlite3
    platform: MACOS_ARM64
    libraryPath: file:///libs/macos-arm64/libsqlite3.dylib
```

### In JSON

```json
{
  "applicationId": "sqlite-app",
  "mainClass": "com.example.SqliteApp",
  "classpathEntries": [
    "file:///path/to/app.jar"
  ],
  "nativeLibraries": [
    {
      "name": "sqlite3",
      "platform": "LINUX_X64",
      "libraryPath": "file:///libs/linux-x64/libsqlite3.so"
    },
    {
      "name": "sqlite3",
      "platform": "WINDOWS_X64",
      "libraryPath": "file:///libs/windows-x64/sqlite3.dll"
    },
    {
      "name": "sqlite3",
      "platform": "MACOS_ARM64",
      "libraryPath": "file:///libs/macos-arm64/libsqlite3.dylib"
    }
  ]
}
```

## Native Library Loading Process

### 1. Deploy-Time Extraction

When an application is deployed, JPlatform:

1. **Detects current platform** (e.g., LINUX_X64)
2. **Filters native libraries** by matching platform
3. **Creates isolated directory**: `/var/jplatform/natives/{applicationId}/`
4. **Extracts libraries** to isolated directory
5. **Updates `java.library.path`** to include isolated directory

### 2. Application Startup

When application calls `System.loadLibrary()`, the JVM:

1. Searches `java.library.path`
2. Finds library in isolated directory: `/var/jplatform/natives/{applicationId}/libsqlite3.so`
3. Loads library using `dlopen()` (Linux) or `LoadLibrary()` (Windows)

### 3. Cleanup on Undeploy

When application is undeployed:

1. Libraries are unloaded (if possible)
2. Isolated directory is deleted: `/var/jplatform/natives/{applicationId}/`

## Using Native Libraries in Applications

### Example: SQLite Application

```java
package com.example;

import org.flossware.jplatform.api.Application;
import org.flossware.jplatform.api.ApplicationContext;

public class SqliteApp implements Application {
    
    @Override
    public void start(ApplicationContext context) throws Exception {
        // Load native library
        // JPlatform has already added /var/jplatform/natives/sqlite-app/ to java.library.path
        System.loadLibrary("sqlite3");
        
        // Use native library via JNI
        Connection conn = DriverManager.getConnection("jdbc:sqlite:mydb.db");
        Statement stmt = conn.createStatement();
        stmt.executeUpdate("CREATE TABLE IF NOT EXISTS users (id INTEGER PRIMARY KEY, name TEXT)");
        
        context.getLogger().info("SQLite library loaded and database initialized");
    }
    
    @Override
    public void stop() throws Exception {
        // Cleanup
    }
}
```

### Example: Custom JNI Library

```java
package com.example;

import org.flossware.jplatform.api.Application;
import org.flossware.jplatform.api.ApplicationContext;

public class JniApp implements Application {
    
    // Native method declaration
    private native void processData(byte[] data);
    private native String getVersion();
    
    @Override
    public void start(ApplicationContext context) throws Exception {
        // Load custom JNI library
        System.loadLibrary("mylib");
        
        // Call native methods
        String version = getVersion();
        context.getLogger().info("Native library version: {}", version);
        
        byte[] data = {1, 2, 3, 4, 5};
        processData(data);
    }
    
    @Override
    public void stop() throws Exception {
        // Cleanup
    }
}
```

## Library Path Resolution

### Supported URI Schemes

Native library paths can use the following URI schemes:

#### file:// (Local Filesystem)

```java
.addNativeLibrary(new NativeLibrary(
    "mylib",
    Platform.LINUX_X64,
    "file:///opt/libs/libmylib.so"  // Absolute path
))
```

#### http:// and https:// (Remote Download)

```java
.addNativeLibrary(new NativeLibrary(
    "mylib",
    Platform.LINUX_X64,
    "https://example.com/downloads/libmylib.so"  // Downloaded at deploy time
))
```

#### maven: (Maven Repository) - Future Enhancement

```java
.addNativeLibrary(new NativeLibrary(
    "sqlite3",
    Platform.LINUX_X64,
    "maven://org.xerial:sqlite-jdbc:3.42.0:jar:natives-linux-x64"
))
```

## Library Isolation

### Per-Application Directories

Each application gets its own isolated directory:

```
/var/jplatform/natives/
  ├── app-1/
  │   ├── libsqlite3.so        (version 3.42)
  │   └── libcustom.so
  ├── app-2/
  │   ├── libsqlite3.so        (version 3.39)  ← Different version!
  │   └── libother.so
  └── app-3/
      └── libjni.so
```

### Version Conflicts Avoided

Two applications can use different versions of the same library:

```java
// app-1: SQLite 3.42
ApplicationDescriptor.builder()
    .applicationId("app-1")
    .addNativeLibrary(new NativeLibrary(
        "sqlite3",
        Platform.LINUX_X64,
        "file:///libs/sqlite-3.42/libsqlite3.so"
    ))
    .build();

// app-2: SQLite 3.39
ApplicationDescriptor.builder()
    .applicationId("app-2")
    .addNativeLibrary(new NativeLibrary(
        "sqlite3",
        Platform.LINUX_X64,
        "file:///libs/sqlite-3.39/libsqlite3.so"
    ))
    .build();
```

Both applications run simultaneously without conflicts.

## GraalVM Native Images (Future Enhancement)

### Configuration

```java
ApplicationDescriptor descriptor = ApplicationDescriptor.builder()
    .applicationId("graal-app")
    .mainClass("com.example.GraalApp")
    .nativeImage(true)  // Flag indicating this is a native executable
    .addClasspathEntry("file:///path/to/graal-app")  // Path to native binary
    .build();
```

### Execution

When `nativeImage=true`, JPlatform will:

1. Skip classloader creation
2. Use `ProcessBuilder` to launch native binary
3. Redirect stdout/stderr to logging
4. Track process via PID
5. Kill process on stop/undeploy

### Example Native Image Deployment (Future)

```bash
# Build GraalVM native image
$ native-image -jar myapp.jar -o myapp-native

# Deploy to JPlatform
$ cat graal-app.yaml
applicationId: graal-app
mainClass: com.example.GraalApp
nativeImage: true
classpathEntries:
  - file:///path/to/myapp-native

$ jplatform-launcher.jar deploy --yaml graal-app.yaml
```

## Platform-Specific Library Naming

### Linux

- **Extension**: `.so` (Shared Object)
- **Naming**: `lib{name}.so`
- **Example**: `libsqlite3.so`
- **Load with**: `System.loadLibrary("sqlite3")`

### Windows

- **Extension**: `.dll` (Dynamic Link Library)
- **Naming**: `{name}.dll`
- **Example**: `sqlite3.dll`
- **Load with**: `System.loadLibrary("sqlite3")`

### macOS

- **Extension**: `.dylib` (Dynamic Library)
- **Naming**: `lib{name}.dylib`
- **Example**: `libsqlite3.dylib`
- **Load with**: `System.loadLibrary("sqlite3")`

### Platform.ANY

For platform-independent libraries (rare):

```java
.addNativeLibrary(new NativeLibrary(
    "universal",
    Platform.ANY,  // Loaded on all platforms
    "file:///libs/universal.lib"
))
```

## Security Considerations

### File Permissions

Extracted libraries have restrictive permissions:

```bash
$ ls -la /var/jplatform/natives/my-app/
-rw-r----- 1 jplatform jplatform 1234567 May 22 libsqlite3.so
```

### Code Signing (Recommended)

For production, sign native libraries:

```bash
# Linux: No built-in signing
# Windows: signtool
$ signtool sign /f mycert.pfx /p password sqlite3.dll

# macOS: codesign
$ codesign -s "Developer ID Application" libsqlite3.dylib
```

### Verification (Future Enhancement)

JPlatform can verify signatures before loading:

```java
.addNativeLibrary(new NativeLibrary(
    "sqlite3",
    Platform.LINUX_X64,
    "file:///libs/libsqlite3.so",
    "sha256:abcd1234..."  // Expected checksum
))
```

## Troubleshooting

### Error: "java.lang.UnsatisfiedLinkError: no sqlite3 in java.library.path"

**Cause**: Library not loaded or wrong platform.

**Solution**:
1. Verify current platform matches declared platform:
   ```java
   System.out.println("OS: " + System.getProperty("os.name"));
   System.out.println("Arch: " + System.getProperty("os.arch"));
   ```
2. Check library was extracted:
   ```bash
   $ ls /var/jplatform/natives/{app-id}/
   ```
3. Verify library path in logs:
   ```
   INFO  [main] o.f.j.c.NativeLibraryLoader - Loaded native library: /var/jplatform/natives/my-app/libsqlite3.so
   ```

### Error: "cannot open shared object file: No such file or directory"

**Cause**: Library has dependencies not available on system.

**Solution**:
1. Check library dependencies:
   ```bash
   $ ldd /var/jplatform/natives/my-app/libsqlite3.so
   ```
2. Install missing system libraries:
   ```bash
   $ sudo apt-get install libc6  # Ubuntu/Debian
   $ sudo yum install glibc      # RHEL/CentOS
   ```

### Error: "wrong ELF class"

**Cause**: 32-bit/64-bit mismatch.

**Solution**:
1. Verify JVM architecture:
   ```bash
   $ java -version
   # OpenJDK 64-Bit Server VM
   ```
2. Use matching library architecture (64-bit JVM requires 64-bit library)

### Warning: "Native library already loaded in another classloader"

**Cause**: Attempting to load same library twice.

**Solution**:
- Native libraries are JVM-global, not per-classloader
- Only call `System.loadLibrary()` once per JVM
- For hot reload, library remains loaded from old classloader

## Best Practices

### 1. Provide Libraries for All Platforms

```java
// ✅ Good: support all major platforms
.addNativeLibrary(new NativeLibrary("mylib", Platform.LINUX_X64, "file:///libs/linux-x64/libmylib.so"))
.addNativeLibrary(new NativeLibrary("mylib", Platform.LINUX_ARM64, "file:///libs/linux-arm64/libmylib.so"))
.addNativeLibrary(new NativeLibrary("mylib", Platform.WINDOWS_X64, "file:///libs/windows-x64/mylib.dll"))
.addNativeLibrary(new NativeLibrary("mylib", Platform.MACOS_X64, "file:///libs/macos-x64/libmylib.dylib"))
.addNativeLibrary(new NativeLibrary("mylib", Platform.MACOS_ARM64, "file:///libs/macos-arm64/libmylib.dylib"))

// ❌ Bad: only Linux support
.addNativeLibrary(new NativeLibrary("mylib", Platform.LINUX_X64, "file:///libs/libmylib.so"))
```

### 2. Use Consistent Library Versions

```java
// ✅ Good: same version across platforms
// libsqlite3.so v3.42.0
// sqlite3.dll v3.42.0
// libsqlite3.dylib v3.42.0

// ❌ Bad: different versions
// libsqlite3.so v3.42.0
// sqlite3.dll v3.39.0  ← Behavior may differ!
```

### 3. Test on All Platforms

```bash
# Test matrix
$ mvn test -Dplatform=linux-x64
$ mvn test -Dplatform=linux-arm64
$ mvn test -Dplatform=windows-x64
$ mvn test -Dplatform=macos-x64
$ mvn test -Dplatform=macos-arm64
```

### 4. Handle Missing Libraries Gracefully

```java
@Override
public void start(ApplicationContext context) throws Exception {
    try {
        System.loadLibrary("mylib");
        useNativeImplementation();
    } catch (UnsatisfiedLinkError e) {
        context.getLogger().warn("Native library not available, using Java fallback");
        useJavaImplementation();
    }
}
```

### 5. Document Library Dependencies

```markdown
# Native Dependencies

This application requires:
- SQLite 3.42+ native library
- OpenSSL 1.1.1+ (libssl, libcrypto)
- zlib 1.2.11+

Install on Ubuntu:
```bash
sudo apt-get install libsqlite3-0 libssl1.1 zlib1g
```

Install on macOS:
```bash
brew install sqlite openssl zlib
```
```

## Example: Multi-Platform Application

```java
package com.example;

import org.flossware.jplatform.api.Application;
import org.flossware.jplatform.api.ApplicationContext;
import org.flossware.jplatform.api.ApplicationDescriptor;
import org.flossware.jplatform.api.NativeLibrary;
import org.flossware.jplatform.api.Platform;

public class ImageProcessingApp implements Application {
    
    // Build descriptor with platform-specific libraries
    public static ApplicationDescriptor buildDescriptor() {
        return ApplicationDescriptor.builder()
            .applicationId("image-processor")
            .mainClass("com.example.ImageProcessingApp")
            .addClasspathEntry("file:///apps/image-processor.jar")
            
            // OpenCV for Linux x86_64
            .addNativeLibrary(new NativeLibrary(
                "opencv_java460",
                Platform.LINUX_X64,
                "file:///libs/linux-x64/libopencv_java460.so"
            ))
            
            // OpenCV for Windows x86_64
            .addNativeLibrary(new NativeLibrary(
                "opencv_java460",
                Platform.WINDOWS_X64,
                "file:///libs/windows-x64/opencv_java460.dll"
            ))
            
            // OpenCV for macOS ARM64 (Apple Silicon)
            .addNativeLibrary(new NativeLibrary(
                "opencv_java460",
                Platform.MACOS_ARM64,
                "file:///libs/macos-arm64/libopencv_java460.dylib"
            ))
            
            .build();
    }
    
    @Override
    public void start(ApplicationContext context) throws Exception {
        // Load OpenCV native library
        System.loadLibrary("opencv_java460");
        
        context.getLogger().info("OpenCV loaded successfully");
        context.getLogger().info("Platform: {}", detectPlatform());
        
        // Use OpenCV
        org.opencv.core.Core.getBuildInformation();
    }
    
    @Override
    public void stop() throws Exception {
        // Cleanup
    }
    
    private String detectPlatform() {
        String os = System.getProperty("os.name");
        String arch = System.getProperty("os.arch");
        return os + " " + arch;
    }
}
```

## See Also

- [Application Descriptor](APPLICATION_DESCRIPTOR.md)
- [Platform API Reference](API_REFERENCE.md)
- [JNI Specification](https://docs.oracle.com/en/java/javase/11/docs/specs/jni/)
- [GraalVM Native Image](https://www.graalvm.org/latest/reference-manual/native-image/)
