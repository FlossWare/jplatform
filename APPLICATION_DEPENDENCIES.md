# Application Dependencies Guide

## Overview

JPlatform supports declarative application dependencies, allowing applications to specify which services they require from other applications. The platform automatically:

- Validates dependencies at deploy time
- Detects circular dependencies
- Computes ordered startup sequences
- Manages service lifecycle based on dependency graph

## Declaring Dependencies

### In Java (ApplicationDescriptor)

```java
import org.flossware.jplatform.api.ApplicationDependency;
import org.flossware.jplatform.api.ApplicationDependency.DependencyType;

ApplicationDescriptor descriptor = ApplicationDescriptor.builder()
    .applicationId("order-service")
    .mainClass("com.example.OrderService")
    .addClasspathEntry("file:///path/to/order-service.jar")
    
    // Required dependency - deployment fails if not available
    .addDependency(new ApplicationDependency(
        "com.example.DatabaseService",
        DependencyType.REQUIRED,
        "1.0.0"
    ))
    
    // Optional dependency - deployment succeeds even if not available
    .addDependency(new ApplicationDependency(
        "com.example.CacheService",
        DependencyType.OPTIONAL,
        "latest"
    ))
    
    .build();
```

### In YAML

```yaml
applicationId: order-service
mainClass: com.example.OrderService
classpathEntries:
  - file:///path/to/order-service.jar

dependencies:
  - serviceInterface: com.example.DatabaseService
    type: REQUIRED
    version: "1.0.0"
    
  - serviceInterface: com.example.CacheService
    type: OPTIONAL
    version: latest
```

### In JSON

```json
{
  "applicationId": "order-service",
  "mainClass": "com.example.OrderService",
  "classpathEntries": [
    "file:///path/to/order-service.jar"
  ],
  "dependencies": [
    {
      "serviceInterface": "com.example.DatabaseService",
      "type": "REQUIRED",
      "version": "1.0.0"
    },
    {
      "serviceInterface": "com.example.CacheService",
      "type": "OPTIONAL",
      "version": "latest"
    }
  ]
}
```

## Dependency Types

### REQUIRED

- Deployment **fails** if service is not registered in ServiceRegistry
- Application **cannot start** without this service
- Use for critical dependencies (database, authentication, etc.)

```java
.addDependency(new ApplicationDependency(
    "com.example.DatabaseService",
    DependencyType.REQUIRED,
    "1.0.0"
))
```

### OPTIONAL

- Deployment **succeeds** even if service is not available
- Application must handle null/absent service gracefully
- Use for non-critical dependencies (caching, metrics, etc.)

```java
.addDependency(new ApplicationDependency(
    "com.example.CacheService",
    DependencyType.OPTIONAL,
    "latest"
))
```

## Version Specification

### Semantic Versioning

Use semantic versioning (semver) format: `MAJOR.MINOR.PATCH`

```java
"1.0.0"     // Exact version 1.0.0
"1.2"       // Version 1.2.x (any patch)
"2"         // Version 2.x.x (any minor/patch)
```

### Latest Version

Use `"latest"` to accept any version:

```java
.addDependency(new ApplicationDependency(
    "com.example.CacheService",
    DependencyType.OPTIONAL,
    "latest"  // Accept any version
))
```

## Dependency Resolution

### Deploy-Time Validation

When you deploy an application, JPlatform:

1. **Validates all REQUIRED dependencies** exist in ServiceRegistry
2. **Checks version compatibility** (if specified)
3. **Detects circular dependencies** using graph algorithms
4. **Computes startup order** using topological sort

If validation fails, deployment is rejected with detailed error message.

### Example: Validation Success

```bash
$ java -jar jplatform-launcher.jar deploy --yaml order-service.yaml
INFO  Validating dependencies for order-service...
INFO  ✓ Found com.example.DatabaseService v1.0.0 (REQUIRED)
INFO  ✓ Found com.example.CacheService v2.1.0 (OPTIONAL)
INFO  ✓ No circular dependencies detected
INFO  Application deployed successfully
```

### Example: Validation Failure

```bash
$ java -jar jplatform-launcher.jar deploy --yaml order-service.yaml
ERROR Dependency validation failed for order-service
ERROR ✗ Required dependency not found: com.example.DatabaseService
ERROR Deployment aborted
```

## Ordered Startup

JPlatform automatically starts applications in dependency order.

### Example Dependency Graph

```
database-service (no dependencies)
    ↓
cache-service (depends on database-service)
    ↓
order-service (depends on cache-service and database-service)
```

### Startup Sequence

```bash
$ java -jar jplatform-launcher.jar start --app-id order-service
INFO  Computing startup order...
INFO  Startup sequence: [database-service, cache-service, order-service]
INFO  Starting database-service...
INFO  database-service is now RUNNING
INFO  Starting cache-service...
INFO  cache-service is now RUNNING
INFO  Starting order-service...
INFO  order-service is now RUNNING
```

## Circular Dependency Detection

JPlatform detects circular dependencies at deploy time:

### Example: Circular Dependency

```
app-a depends on app-b
app-b depends on app-c
app-c depends on app-a  ← circular!
```

### Error Message

```bash
ERROR Circular dependency detected: app-a → app-b → app-c → app-a
ERROR Deployment aborted
```

### Algorithm

JPlatform uses **Depth-First Search (DFS)** with recursion stack to detect cycles in O(V + E) time.

## Service Registry Integration

Dependencies are resolved against the **ServiceRegistry**:

### Registering a Service

```java
public class DatabaseService implements Application {
    
    @Override
    public void start(ApplicationContext context) throws Exception {
        // Register service implementation
        context.getServiceRegistry().ifPresent(registry -> {
            registry.registerService(
                DatabaseService.class,
                this,
                "1.0.0"  // Version
            );
        });
    }
    
    @Override
    public void stop() throws Exception {
        // Unregister on shutdown
        context.getServiceRegistry().ifPresent(registry -> {
            registry.unregisterService(DatabaseService.class, this);
        });
    }
}
```

### Looking Up a Service

```java
public class OrderService implements Application {
    
    private DatabaseService database;
    private CacheService cache;
    
    @Override
    public void start(ApplicationContext context) throws Exception {
        context.getServiceRegistry().ifPresent(registry -> {
            // Required dependency - throws if not found
            database = registry.getService(DatabaseService.class)
                .orElseThrow(() -> new IllegalStateException("Database service not available"));
            
            // Optional dependency - handle absence gracefully
            cache = registry.getService(CacheService.class)
                .orElse(new NoOpCacheService());
        });
    }
}
```

## Health Checks

Applications can implement `HealthCheck` to report service health:

### Implementing HealthCheck

```java
package com.example;

import org.flossware.jplatform.api.Application;
import org.flossware.jplatform.api.HealthCheck;
import org.flossware.jplatform.api.ApplicationContext;

public class DatabaseService implements Application, HealthCheck {
    
    private Connection connection;
    
    @Override
    public void start(ApplicationContext context) throws Exception {
        connection = DriverManager.getConnection("jdbc:...");
        
        // Register as service
        context.getServiceRegistry().ifPresent(registry -> {
            registry.registerService(DatabaseService.class, this, "1.0.0");
        });
    }
    
    @Override
    public boolean isHealthy() {
        try {
            return connection != null && !connection.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }
    
    @Override
    public String getHealthMessage() {
        if (isHealthy()) {
            return "Database connection is active";
        } else {
            return "Database connection is down";
        }
    }
}
```

### Health Check Polling (Future Enhancement)

Health checks can be polled periodically to notify dependent applications:

```java
// Future feature: automatic health monitoring
DependencyResolver resolver = new DependencyResolver(serviceRegistry);
resolver.startHealthMonitoring(Duration.ofSeconds(30));  // Check every 30s

resolver.addHealthListener((serviceInterface, healthy, message) -> {
    if (!healthy) {
        logger.warn("Service {} is unhealthy: {}", serviceInterface, message);
        // Notify dependent applications
    }
});
```

## Best Practices

### 1. Use REQUIRED for Critical Services

```java
// ✅ Good: database is critical
.addDependency(new ApplicationDependency(
    "com.example.DatabaseService",
    DependencyType.REQUIRED,
    "1.0.0"
))

// ❌ Bad: database marked as optional
.addDependency(new ApplicationDependency(
    "com.example.DatabaseService",
    DependencyType.OPTIONAL,  // App will fail at runtime!
    "1.0.0"
))
```

### 2. Handle Optional Dependencies Gracefully

```java
// ✅ Good: provide fallback for optional service
cache = registry.getService(CacheService.class)
    .orElse(new NoOpCacheService());

// ❌ Bad: throw exception for optional dependency
cache = registry.getService(CacheService.class)
    .orElseThrow();  // Defeats purpose of OPTIONAL!
```

### 3. Specify Versions for Stability

```java
// ✅ Good: pin to specific version for production
.addDependency(new ApplicationDependency(
    "com.example.DatabaseService",
    DependencyType.REQUIRED,
    "1.2.0"  // Exact version
))

// ⚠️ Acceptable: use "latest" for development
.addDependency(new ApplicationDependency(
    "com.example.CacheService",
    DependencyType.OPTIONAL,
    "latest"  // Any version OK
))
```

### 4. Avoid Circular Dependencies

```java
// ❌ Bad: circular dependency
// service-a depends on service-b
// service-b depends on service-a

// ✅ Good: extract shared logic to common service
// service-a depends on common-service
// service-b depends on common-service
```

### 5. Use ServiceRegistry for Loose Coupling

```java
// ✅ Good: depend on interface
.addDependency(new ApplicationDependency(
    "com.example.DatabaseService",  // Interface
    DependencyType.REQUIRED,
    "1.0.0"
))

// Multiple implementations can be registered
registry.registerService(DatabaseService.class, mysqlImpl, "1.0.0");
registry.registerService(DatabaseService.class, postgresImpl, "1.0.0");
```

## Example: Multi-Tier Application

### Tier 1: Database Service (No Dependencies)

```yaml
# database-service.yaml
applicationId: database-service
mainClass: com.example.DatabaseService
classpathEntries:
  - file:///services/database-service.jar
dependencies: []  # No dependencies
```

### Tier 2: Cache Service (Depends on Database)

```yaml
# cache-service.yaml
applicationId: cache-service
mainClass: com.example.CacheService
classpathEntries:
  - file:///services/cache-service.jar
dependencies:
  - serviceInterface: com.example.DatabaseService
    type: REQUIRED
    version: "1.0.0"
```

### Tier 3: Order Service (Depends on Cache and Database)

```yaml
# order-service.yaml
applicationId: order-service
mainClass: com.example.OrderService
classpathEntries:
  - file:///services/order-service.jar
dependencies:
  - serviceInterface: com.example.DatabaseService
    type: REQUIRED
    version: "1.0.0"
  - serviceInterface: com.example.CacheService
    type: REQUIRED
    version: "1.0.0"
```

### Deployment and Startup

```bash
# Deploy all services (order doesn't matter)
$ jplatform-launcher.jar deploy --yaml database-service.yaml
$ jplatform-launcher.jar deploy --yaml cache-service.yaml
$ jplatform-launcher.jar deploy --yaml order-service.yaml

# Start top-level service (dependencies auto-start)
$ jplatform-launcher.jar start --app-id order-service

# Output:
# INFO  Computing startup order...
# INFO  Startup sequence: [database-service, cache-service, order-service]
# INFO  Starting database-service...
# INFO  Starting cache-service...
# INFO  Starting order-service...
# INFO  All services started successfully
```

## Troubleshooting

### Error: "Required dependency not found"

**Cause**: Service interface not registered in ServiceRegistry.

**Solution**:
1. Verify provider application is deployed: `GET /api/applications`
2. Check provider calls `registry.registerService()` in `start()`
3. Ensure provider is started before dependent application

### Error: "Circular dependency detected"

**Cause**: Applications have circular dependency chain.

**Solution**:
1. Review dependency graph
2. Refactor to extract shared logic into separate service
3. Consider using MessageBus for loose coupling instead of direct dependencies

### Warning: "Service version mismatch"

**Cause**: Registered service version doesn't match required version.

**Solution**:
1. Update dependency to accept broader version: `"1"` instead of `"1.0.0"`
2. Deploy correct version of service
3. Use `"latest"` for version-agnostic dependencies (not recommended for production)

## API Reference

### ApplicationDependency

```java
public class ApplicationDependency {
    public ApplicationDependency(String serviceInterface, DependencyType type, String version);
    
    public String getServiceInterface();
    public DependencyType getType();
    public String getVersion();
    
    public enum DependencyType {
        REQUIRED,   // Deployment fails if not available
        OPTIONAL    // Deployment succeeds even if absent
    }
}
```

### DependencyResolver

```java
public class DependencyResolver {
    public void validateDependencies(String applicationId, List<ApplicationDependency> dependencies);
    public List<String> computeStartupOrder(String applicationId);
    public boolean hasCircularDependency(String applicationId);
}
```

### HealthCheck

```java
public interface HealthCheck {
    boolean isHealthy();
    String getHealthMessage();
}
```

## See Also

- [Service Registry](SERVICE_REGISTRY.md)
- [Application Lifecycle](LIFECYCLE.md)
- [Hot Reload](HOT_RELOAD.md)
- [REST API Reference](REST_API.md)
