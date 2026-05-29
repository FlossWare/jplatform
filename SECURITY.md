# Security Guide

## Overview

platform-java provides application-level security through a modern enforcement system that replaces the deprecated `SecurityManager` API. The security system uses the StackWalker API (Java 9+) to enforce permissions based on the calling ClassLoader.

## Security Architecture

### Components

1. **SecurityPolicy** (interface) - Defines what permissions are granted to an application
2. **ApplicationSecurityPolicy** (implementation) - Default policy implementation
3. **SecurityEnforcer** (singleton) - Enforces security using StackWalker API
4. **SecurityConfig** (configuration) - Builder-based security configuration

### Why Not SecurityManager?

The `SecurityManager` API was deprecated in Java 17 and will be removed in future Java versions. platform-java's security system offers:

- ✅ **Future-proof** - Not deprecated, works with Java 17+
- ✅ **Better performance** - No global permission checks on every operation
- ✅ **More flexible** - Can be enabled/disabled per operation type
- ✅ **Cleaner code** - No deep SecurityManager call chains in stack traces

## Configuration

### Basic Security Configuration

```java
SecurityConfig securityConfig = SecurityConfig.builder()
    .allowReflection(false)
    .allowNativeCode(false)
    .addFilePermission(new FilePermission("/tmp/*", "read,write"))
    .addSocketPermission(new SocketPermission("example.com:80", "connect"))
    .build();

ApplicationDescriptor descriptor = ApplicationDescriptor.builder()
    .applicationId("my-app")
    .securityConfig(securityConfig)
    .build();
```

### YAML Configuration

```yaml
applicationId: my-app
security:
  allowReflection: false
  allowNativeCode: false
  filePermissions:
    - path: "/tmp/*"
      actions: "read,write"
    - path: "/var/myapp/data/*"
      actions: "read,write,delete"
  socketPermissions:
    - host: "example.com:80"
      actions: "connect"
    - host: "localhost:5432"
      actions: "connect,resolve"
  runtimePermissions:
    - "getenv.HOME"
    - "setProperty.my.property"
```

## Permission Types

### File Permissions

Control file system access:

```java
SecurityConfig.builder()
    .addFilePermission(new FilePermission("/tmp/*", "read"))         // Read only
    .addFilePermission(new FilePermission("/var/app/data/*", "read,write"))  // Read/write
    .addFilePermission(new FilePermission("/etc/config.yaml", "read"))  // Specific file
    .build();
```

**Actions**: `read`, `write`, `delete`, `execute`

### Socket Permissions

Control network access:

```java
SecurityConfig.builder()
    .addSocketPermission(new SocketPermission("*.example.com:80", "connect"))  // HTTP
    .addSocketPermission(new SocketPermission("*.example.com:443", "connect")) // HTTPS
    .addSocketPermission(new SocketPermission("localhost:5432", "connect"))    // Database
    .build();
```

**Actions**: `connect`, `accept`, `listen`, `resolve`

### Runtime Permissions

Control JVM operations:

```java
SecurityConfig.builder()
    .addRuntimePermission(new RuntimePermission("getenv.HOME"))          // Read env var
    .addRuntimePermission(new RuntimePermission("setProperty.my.prop")) // Set system property
    .build();
```

Common runtime permissions:
- `getenv.*` - Read environment variables
- `setProperty.*` - Set system properties
- `exitVM` - Terminate JVM (usually denied)

### Reflection

Control reflection access:

```java
SecurityConfig.builder()
    .allowReflection(true)  // Allow all reflection
    .build();
```

When `allowReflection=false`:
- Cannot use `Class.getDeclaredFields()`
- Cannot use `Field.setAccessible(true)`
- Cannot access private members

### Native Code

Control native library loading:

```java
SecurityConfig.builder()
    .allowNativeCode(true)  // Allow System.loadLibrary()
    .build();
```

When `allowNativeCode=false`:
- Cannot call `System.loadLibrary()`
- Cannot load `.so`, `.dll`, `.dylib` files

## Enforcement Modes

### Manual Enforcement (Current)

Applications must explicitly check permissions:

```java
public class MyApp implements Application {
    @Override
    public void start(ApplicationContext context) throws Exception {
        // Explicitly check permission
        context.getSecurityPolicy().enforce(
            new FilePermission("/tmp/file.txt", "read")
        );
        
        // If we reach here, permission was granted
        Files.readString(Path.of("/tmp/file.txt"));
    }
}
```

### Automatic Enforcement (via SecurityEnforcer)

The `SecurityEnforcer` provides StackWalker-based enforcement:

```java
import org.flossware.platform-java.security.SecurityEnforcer;

public class MyApp implements Application {
    @Override
    public void start(ApplicationContext context) throws Exception {
        SecurityEnforcer enforcer = SecurityEnforcer.getInstance();
        
        // Check file access (throws SecurityException if denied)
        enforcer.checkFileAccess("/tmp/file.txt", "read");
        Files.readString(Path.of("/tmp/file.txt"));
        
        // Check network access
        enforcer.checkSocketAccess("example.com", 80, "connect");
        URL url = new URL("http://example.com");
        url.openConnection();
        
        // Check reflection
        enforcer.checkReflectionAccess();
        Class<?> clazz = MyClass.class;
        clazz.getDeclaredFields();
    }
}
```

### Enable Global Enforcement

Enable automatic enforcement for all operations:

```bash
java -Dplatform-java.security.enforce=true -jar platform-java-launcher.jar
```

When enabled, the enforcer automatically checks permissions using the calling ClassLoader's security policy.

## Security Patterns

### Principle of Least Privilege

Grant only the minimum permissions needed:

```java
// ❌ Bad - overly permissive
SecurityConfig.builder()
    .addFilePermission(new FilePermission("*", "read,write,delete,execute"))
    .allowReflection(true)
    .build();

// ✅ Good - minimal permissions
SecurityConfig.builder()
    .addFilePermission(new FilePermission("/var/myapp/data/*", "read,write"))
    .allowReflection(false)
    .build();
```

### Separate Configs for Dev/Prod

```java
public class MyAppConfig {
    public static SecurityConfig forProduction() {
        return SecurityConfig.builder()
            .allowReflection(false)  // Strict
            .allowNativeCode(false)
            .addFilePermission(new FilePermission("/var/myapp/data/*", "read,write"))
            .build();
    }
    
    public static SecurityConfig forDevelopment() {
        return SecurityConfig.builder()
            .allowReflection(true)   // More permissive for debugging
            .allowNativeCode(true)
            .addFilePermission(new FilePermission("/tmp/*", "read,write"))
            .build();
    }
}
```

### Defense in Depth

Layer security with resource limits:

```yaml
applicationId: my-app
security:
  allowReflection: false
  filePermissions:
    - path: "/var/myapp/data/*"
      actions: "read,write"
resources:
  maxHeapMB: 512  # Resource limit
  memoryEnforcementAction: SHUTDOWN
```

## StackWalker-Based Enforcement

### How It Works

1. Application calls a security-sensitive method
2. SecurityEnforcer uses StackWalker to find the calling class
3. Gets the ClassLoader from the calling class
4. Looks up the SecurityPolicy for that ClassLoader
5. Checks if the policy allows the operation
6. Throws SecurityException if denied

### Example

```java
public class MyApp implements Application {
    @Override
    public void start(ApplicationContext context) {
        readFile("/tmp/secret.txt");  // Calls our method
    }
    
    private void readFile(String path) {
        // Check permission using StackWalker
        SecurityEnforcer.getInstance().checkFileAccess(path, "read");
        // ↑ StackWalker finds MyApp.class
        // → Gets MyApp's ClassLoader
        // → Looks up SecurityPolicy for that ClassLoader
        // → Checks if FilePermission("/tmp/secret.txt", "read") is granted
        
        Files.readString(Path.of(path));
    }
}
```

## Custom Security Policies

### Implementing SecurityPolicy

```java
public class CustomSecurityPolicy implements SecurityPolicy {
    
    @Override
    public boolean checkPermission(Permission permission) {
        // Custom logic
        if (permission instanceof FilePermission) {
            FilePermission fp = (FilePermission) permission;
            // Only allow read access during business hours
            if (fp.getActions().contains("write") && !isBusinessHours()) {
                return false;
            }
        }
        return true;
    }
    
    @Override
    public void enforce(Permission permission) throws SecurityException {
        if (!checkPermission(permission)) {
            throw new SecurityException("Permission denied: " + permission);
        }
    }
    
    @Override
    public Set<Permission> getGrantedPermissions() {
        // Return all granted permissions
        return Collections.emptySet();
    }
    
    private boolean isBusinessHours() {
        int hour = LocalTime.now().getHour();
        return hour >= 9 && hour < 17;
    }
}
```

### Register Custom Policy

```java
ApplicationDescriptor descriptor = ApplicationDescriptor.builder()
    .applicationId("my-app")
    // Use default config, will be overridden
    .securityConfig(SecurityConfig.builder().build())
    .build();

// After deploy, replace with custom policy
ApplicationContext context = manager.getApplicationContext("my-app");
CustomSecurityPolicy customPolicy = new CustomSecurityPolicy();

SecurityEnforcer.getInstance()
    .registerPolicy(context.getClassLoader(), customPolicy);
```

## Security Best Practices

### 1. Always Configure Security

```java
// ❌ Bad - no security config
ApplicationDescriptor.builder()
    .applicationId("my-app")
    .build();

// ✅ Good - explicit security config
ApplicationDescriptor.builder()
    .applicationId("my-app")
    .securityConfig(SecurityConfig.builder()
        .allowReflection(false)
        .addFilePermission(new FilePermission("/var/myapp/*", "read,write"))
        .build())
    .build();
```

### 2. Disable Reflection in Production

```java
SecurityConfig.builder()
    .allowReflection(false)  // ✅ Prevents malicious reflection attacks
    .build();
```

### 3. Use Wildcards Carefully

```java
// ❌ Bad - too permissive
new FilePermission("*", "read,write")

// ✅ Good - specific directory
new FilePermission("/var/myapp/data/*", "read,write")
```

### 4. Validate User Input

```java
public void processFile(String filename) {
    // ❌ Bad - path traversal attack possible
    SecurityEnforcer.getInstance().checkFileAccess(filename, "read");
    
    // ✅ Good - validate input first
    if (filename.contains("..")) {
        throw new IllegalArgumentException("Invalid filename");
    }
    SecurityEnforcer.getInstance().checkFileAccess(
        "/var/myapp/data/" + filename, "read"
    );
}
```

### 5. Log Security Violations

```java
try {
    enforcer.checkFileAccess(path, "write");
    writeFile(path);
} catch (SecurityException e) {
    logger.warn("Security violation: attempted to write to {}", path, e);
    throw e;
}
```

## Migration from SecurityManager

If you have existing code using SecurityManager:

### Before (SecurityManager)

```java
SecurityManager sm = System.getSecurityManager();
if (sm != null) {
    sm.checkPermission(new FilePermission("/tmp/file.txt", "read"));
}
Files.readString(Path.of("/tmp/file.txt"));
```

### After (SecurityEnforcer)

```java
SecurityEnforcer.getInstance()
    .checkFileAccess("/tmp/file.txt", "read");
Files.readString(Path.of("/tmp/file.txt"));
```

## Troubleshooting

### Security Check Always Passes

**Problem**: Security checks never deny access.

**Solution**: Enable enforcement:
```bash
java -Dplatform-java.security.enforce=true -jar platform-java-launcher.jar
```

Or programmatically:
```java
SecurityEnforcer.getInstance().setEnabled(true);
```

### SecurityException Not Expected

**Problem**: Getting SecurityException for allowed operations.

**Solution**: Check your SecurityConfig:
```java
// Verify permissions are granted
SecurityPolicy policy = enforcer.getPolicy(classLoader);
Set<Permission> granted = policy.getGrantedPermissions();
granted.forEach(p -> System.out.println("Granted: " + p));
```

### No Policy Registered

**Problem**: "No security policy for ClassLoader" warning.

**Solution**: Ensure policy is registered during deploy:
```java
// This should happen automatically in ApplicationManager.deploy()
SecurityEnforcer.getInstance()
    .registerPolicy(classLoader, securityPolicy);
```

## Future Enhancements

Planned improvements:

1. **Bytecode Instrumentation** - Transparent enforcement without manual calls
2. **Audit Logging** - Detailed security event logs
3. **Dynamic Policies** - Change policies at runtime
4. **Policy Templates** - Pre-configured policies for common scenarios
5. **Integration with External Policy Engines** - Apache Ranger, OPA

## See Also

- [Application Descriptor](APPLICATION_DESCRIPTOR.md)
- [Best Practices](CLASSLOADER_BEST_PRACTICES.md)
- [Resource Enforcement](RESOURCE_ENFORCEMENT.md)
