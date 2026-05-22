# Resource Limits Enforcement

## Overview

JPlatform 2.0 introduces **automatic resource enforcement**, allowing the platform to take action when applications exceed their configured resource quotas. This feature extends the existing resource monitoring capabilities with configurable enforcement policies.

## Key Concepts

### Enforcement Actions

When an application exceeds a resource quota, the platform can automatically take one of four actions:

- **NOTIFY** (default): Log the violation and notify listeners, but take no enforcement action. This is the existing behavior from JPlatform 1.0.
- **THROTTLE**: Slow down application execution by introducing delays. Reduces CPU and memory pressure without stopping the application.
- **SHUTDOWN**: Gracefully stop the application by calling `ApplicationManager.stop()`. The application's `stop()` method is invoked, allowing cleanup.
- **KILL**: Immediately terminate the application without graceful shutdown. Calls `shutdownNow()` on the thread pool and sets state to FAILED.

### Grace Periods

To prevent transient resource spikes from triggering enforcement, the platform uses **grace periods**:

- Default: 3 consecutive violations
- Configurable per application via `violationGracePeriod`
- Violation count is tracked per resource type (CPU, memory, threads)
- Violations are cleared when resource usage returns to normal

### Per-Resource Configuration

Each resource type (CPU, memory, threads) can have its own enforcement policy:

```java
ResourceConfig.builder()
    .maxHeapMB(512)
    .memoryEnforcementAction(EnforcementAction.SHUTDOWN)  // OOM → shutdown
    .maxCpuTimeSeconds(300)
    .cpuEnforcementAction(EnforcementAction.THROTTLE)     // High CPU → throttle
    .maxThreads(50)
    .threadEnforcementAction(EnforcementAction.SHUTDOWN)  // Thread leak → shutdown
    .violationGracePeriod(3)  // 3 consecutive violations before action
    .build();
```

## Configuration Examples

### Example 1: Strict Memory Enforcement

Prevent applications from consuming excessive memory by automatically shutting them down:

```java
ApplicationDescriptor descriptor = ApplicationDescriptor.builder()
    .applicationId("memory-constrained-app")
    .mainClass("com.example.MyApp")
    .resourceConfig(ResourceConfig.builder()
        .maxHeapMB(256)  // 256MB limit
        .memoryEnforcementAction(EnforcementAction.SHUTDOWN)
        .violationGracePeriod(2)  // Shutdown after 2 violations (10 seconds)
        .build())
    .build();
```

### Example 2: CPU Throttling

Slow down CPU-intensive applications without stopping them:

```java
ApplicationDescriptor descriptor = ApplicationDescriptor.builder()
    .applicationId("cpu-intensive-app")
    .mainClass("com.example.DataProcessor")
    .resourceConfig(ResourceConfig.builder()
        .maxCpuTimeSeconds(60)  // 60 seconds CPU time
        .cpuEnforcementAction(EnforcementAction.THROTTLE)
        .violationGracePeriod(5)  // Throttle after 5 violations (25 seconds)
        .build())
    .build();
```

### Example 3: Mixed Enforcement

Different enforcement actions for different resource types:

```java
ResourceConfig config = ResourceConfig.builder()
    // Memory: aggressive enforcement
    .maxHeapMB(512)
    .memoryEnforcementAction(EnforcementAction.SHUTDOWN)
    
    // CPU: throttle to reduce load
    .maxCpuTimeSeconds(300)
    .cpuEnforcementAction(EnforcementAction.THROTTLE)
    
    // Threads: kill if thread leak detected
    .maxThreads(100)
    .threadEnforcementAction(EnforcementAction.KILL)
    
    .violationGracePeriod(3)
    .build();
```

### Example 4: Monitoring Only (No Enforcement)

Use NOTIFY action to log violations without taking action (default behavior):

```java
ResourceConfig config = ResourceConfig.builder()
    .maxHeapMB(512)
    .memoryEnforcementAction(EnforcementAction.NOTIFY)  // Log only
    .maxCpuTimeSeconds(300)
    .cpuEnforcementAction(EnforcementAction.NOTIFY)
    .build();
```

## YAML Configuration

Enforcement actions can be configured in YAML descriptors:

```yaml
applicationId: my-app
mainClass: com.example.MyApp
resourceConfig:
  maxHeapMB: 512
  memoryEnforcementAction: SHUTDOWN
  maxCpuTimeSeconds: 300
  cpuEnforcementAction: THROTTLE
  maxThreads: 50
  threadEnforcementAction: SHUTDOWN
  violationGracePeriod: 3
```

## How It Works

### Resource Monitoring Cycle

1. **ApplicationResourceMonitor** polls metrics every 5 seconds
2. If resource usage exceeds quota, **ResourceQuota.enforce()** throws **ResourceQuotaExceededException**
3. Exception is caught and passed to **ResourceEnforcer**
4. **EnforcementPolicy** checks violation count against grace period
5. If grace period exceeded, **ResourceEnforcer** executes the configured action

### Violation Tracking

```java
// EnforcementPolicy tracks violations per resource type
violationCounts = {
    "heap": 3,      // 3 consecutive violations
    "cpu": 1,       // 1 violation
    "threads": 0    // No violations
}

// When grace period (3) is reached:
if (violationCounts.get("heap") >= gracePeriod) {
    executeAction(EnforcementAction.SHUTDOWN);
}

// Violations are cleared when resource usage returns to normal
clearViolation("cpu");  // CPU usage dropped below quota
```

### Enforcement Actions Implementation

**NOTIFY**:
```java
// Already handled by ResourceQuotaExceededException
// Logged and ResourceEventListeners are notified
```

**THROTTLE**:
```java
// Future implementation: Thread.sleep() on application threads
// Throttle percentage based on overage
// Example: 120% of quota → sleep 20% of time
```

**SHUTDOWN**:
```java
// Calls ApplicationManager.stop(applicationId)
// Application.stop() is invoked
// Thread pool is shutdown gracefully
// State transitions: RUNNING → STOPPING → STOPPED
```

**KILL**:
```java
// Calls ApplicationManager.forceKill(applicationId)
// ThreadPool.shutdownNow() is called
// No graceful cleanup
// State transitions: RUNNING → FAILED
```

## Best Practices

### 1. Set Appropriate Grace Periods

- **Too low**: Transient spikes trigger enforcement unnecessarily
- **Too high**: Application can consume excessive resources before action is taken
- **Recommendation**: 3-5 violations (15-25 seconds with 5-second polling)

### 2. Choose the Right Action

- **NOTIFY**: Development/testing, or when you want manual intervention
- **THROTTLE**: CPU-intensive batch jobs that can tolerate delays
- **SHUTDOWN**: Memory leaks, runaway applications (allows cleanup)
- **KILL**: Thread leaks, unresponsive applications (last resort)

### 3. Monitor Enforcement Events

Listen for enforcement events to track platform health:

```java
resourceMonitor.addListener(new ResourceEventListener() {
    @Override
    public void onQuotaExceeded(String applicationId, ResourceSnapshot snapshot) {
        logger.warn("App {} exceeded quota: heap={}, cpu={}, threads={}",
            applicationId, snapshot.getHeapUsedBytes(), 
            snapshot.getCpuTimeNanos(), snapshot.getThreadCount());
    }
});
```

### 4. Test Enforcement Policies

Create test applications that intentionally exceed quotas to verify enforcement:

```java
// Test memory enforcement
public class MemoryHogApp implements Application {
    @Override
    public void start(ApplicationContext context) {
        List<byte[]> leak = new ArrayList<>();
        while (true) {
            leak.add(new byte[1024 * 1024]);  // Allocate 1MB per iteration
            Thread.sleep(1000);
        }
    }
}
```

### 5. Combine with Alerts

Integrate enforcement events with monitoring systems:

```java
resourceMonitor.addListener(new ResourceEventListener() {
    @Override
    public void onQuotaExceeded(String applicationId, ResourceSnapshot snapshot) {
        // Send alert to PagerDuty, Slack, etc.
        alertingService.sendAlert("App " + applicationId + " exceeded quota");
    }
});
```

## Architecture

### Components

1. **EnforcementAction** (enum in `jplatform-api`):
   - Defines the 4 enforcement actions
   - Helper methods: `isDestructive()`, `isGraceful()`

2. **ResourceConfig** (in `jplatform-api`):
   - Extended with enforcement action fields
   - Per-resource-type enforcement configuration
   - Grace period configuration

3. **EnforcementPolicy** (in `jplatform-monitoring`):
   - Tracks violation counts per application
   - Implements grace period logic
   - Thread-safe using ConcurrentHashMap

4. **ResourceEnforcer** (in `jplatform-monitoring`):
   - Receives quota exceeded events
   - Consults EnforcementPolicy for grace period
   - Executes configured enforcement actions
   - Delegates shutdown/kill to ApplicationManager

5. **ApplicationResourceMonitor** (in `jplatform-monitoring`):
   - Enhanced to call ResourceEnforcer on quota violations
   - Wired up during application deployment

6. **ApplicationManager** (in `jplatform-core`):
   - Creates ResourceEnforcer during deployment
   - Provides `forceKill()` method for KILL action
   - Existing `stop()` method used for SHUTDOWN action

### Data Flow

```
ApplicationResourceMonitor (5s polling)
  ↓
ResourceQuota.enforce(snapshot)
  ↓
ResourceQuotaExceededException thrown
  ↓
ResourceEnforcer.enforceQuota()
  ↓
EnforcementPolicy.recordViolation()
  ↓
If grace period exceeded:
  ↓
ResourceEnforcer.executeAction()
  ↓
ApplicationManager.stop() or forceKill()
```

## API Reference

### EnforcementAction Enum

```java
package org.flossware.jplatform.api;

public enum EnforcementAction {
    NOTIFY,     // Log and notify only
    THROTTLE,   // Slow down execution
    SHUTDOWN,   // Graceful stop
    KILL;       // Immediate termination
    
    public boolean isDestructive();  // true for SHUTDOWN/KILL
    public boolean isGraceful();     // true for all except KILL
}
```

### ResourceConfig Extensions

```java
package org.flossware.jplatform.api;

public class ResourceConfig {
    // New fields (v2.0)
    private final EnforcementAction memoryEnforcementAction;
    private final EnforcementAction cpuEnforcementAction;
    private final EnforcementAction threadEnforcementAction;
    private final int violationGracePeriod;
    
    public static class Builder {
        public Builder memoryEnforcementAction(EnforcementAction action);
        public Builder cpuEnforcementAction(EnforcementAction action);
        public Builder threadEnforcementAction(EnforcementAction action);
        public Builder violationGracePeriod(int violations);
    }
}
```

### ResourceEnforcer

```java
package org.flossware.jplatform.monitoring;

public class ResourceEnforcer {
    public ResourceEnforcer(
        String applicationId,
        ResourceConfig config,
        Consumer<String> shutdownAction,
        Consumer<String> killAction
    );
    
    public void enforceQuota(ResourceQuota quota, ResourceSnapshot snapshot);
}
```

## Migration from JPlatform 1.0

Applications using JPlatform 1.0 resource monitoring continue to work without changes:

- Default enforcement action is **NOTIFY** (existing behavior)
- No configuration changes required
- Opt-in to enforcement by adding enforcement action fields to ResourceConfig

### Before (JPlatform 1.0):

```java
ResourceConfig config = ResourceConfig.builder()
    .maxHeapMB(512)
    .maxCpuTimeSeconds(300)
    .build();
// Violations are logged only
```

### After (JPlatform 2.0):

```java
ResourceConfig config = ResourceConfig.builder()
    .maxHeapMB(512)
    .memoryEnforcementAction(EnforcementAction.SHUTDOWN)  // NEW
    .maxCpuTimeSeconds(300)
    .cpuEnforcementAction(EnforcementAction.THROTTLE)     // NEW
    .violationGracePeriod(3)                              // NEW
    .build();
// Violations trigger automatic enforcement
```

## Troubleshooting

### Application Keeps Getting Shutdown

**Cause**: Enforcement action is too aggressive or quotas are too low.

**Solution**:
1. Review application resource usage in metrics
2. Increase resource quotas
3. Increase grace period
4. Change action to THROTTLE or NOTIFY

### Enforcement Not Triggering

**Cause**: Grace period not exceeded or enforcement action is NOTIFY.

**Solution**:
1. Check violation count in logs
2. Reduce grace period for testing
3. Verify enforcement action is not NOTIFY
4. Check that quotas are actually being exceeded

### Application Killed Too Aggressively

**Cause**: KILL action used instead of SHUTDOWN.

**Solution**:
1. Change enforcement action to SHUTDOWN
2. Increase grace period
3. Review why graceful shutdown is not working

## See Also

- [Resource Monitoring](RESOURCE_MONITORING.md) - Resource tracking and quotas
- [Application Lifecycle](LIFECYCLE.md) - Application states and transitions
- [Persistent Volumes](VOLUMES.md) - Data persistence across restarts
