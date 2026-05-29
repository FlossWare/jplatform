# Hot Code Reload Guide

## Overview

platform-java supports hot code reload, allowing you to update an application's code without requiring a full platform restart or losing application state. This enables zero-downtime deployments and faster development iteration cycles.

## How It Works

Hot code reload works by:

1. Creating a new isolated classloader with the updated JAR
2. Capturing application state via the `ReloadableApplication` interface
3. Gracefully stopping the old application instance
4. Swapping the classloader reference atomically
5. Starting a new application instance with the same ApplicationContext
6. Restoring the captured state to the new instance

The old classloader is kept in memory temporarily with reference counting to ensure safe garbage collection after all threads have finished using it.

## Configuration

### Enable Hot Reload in Descriptor

```java
ApplicationDescriptor descriptor = ApplicationDescriptor.builder()
    .applicationId("my-app")
    .mainClass("com.example.MyApp")
    .addClasspathEntry("file:///path/to/app.jar")
    .hotReloadEnabled(true)      // Enable hot reload capability
    .preserveState(true)          // Enable state preservation
    .build();
```

### YAML Configuration

```yaml
applicationId: my-app
mainClass: com.example.MyApp
classpathEntries:
  - file:///path/to/app.jar
hotReloadEnabled: true
preserveState: true
```

## Implementing ReloadableApplication

To support state preservation during reload, your application class must implement the `ReloadableApplication` interface:

```java
package com.example;

import org.flossware.platform-java.api.Application;
import org.flossware.platform-java.api.ReloadableApplication;
import org.flossware.platform-java.api.ApplicationContext;

import java.util.HashMap;
import java.util.Map;

public class MyApp implements ReloadableApplication {
    
    private Map<String, String> cache;
    private int requestCount;
    
    @Override
    public void start(ApplicationContext context) throws Exception {
        // Normal startup
        cache = new HashMap<>();
        requestCount = 0;
    }
    
    @Override
    public void stop() throws Exception {
        // Normal shutdown
        cache.clear();
    }
    
    /**
     * Called before reload to capture state.
     * Return a Map containing all state that should be preserved.
     */
    @Override
    public Map<String, Object> beforeReload() throws Exception {
        Map<String, Object> state = new HashMap<>();
        state.put("cache", cache);
        state.put("requestCount", requestCount);
        return state;
    }
    
    /**
     * Called after reload to restore state.
     * Receives the same ApplicationContext and the state from beforeReload().
     */
    @Override
    public void afterReload(ApplicationContext context, Map<String, Object> state) throws Exception {
        this.cache = (Map<String, String>) state.get("cache");
        this.requestCount = (Integer) state.get("requestCount");
    }
}
```

## Performing a Hot Reload

### Via ApplicationManager API

```java
ApplicationManager manager = // ... get manager instance
ApplicationDescriptor newDescriptor = // ... load updated descriptor

manager.reload("my-app", newDescriptor);
```

### Via REST API

```bash
# Upload new descriptor
curl -X POST http://localhost:8080/api/applications/my-app/reload \
  -H "Content-Type: application/json" \
  -d @updated-app.json
```

### Via PlatformLauncher CLI

```bash
java -jar platform-java-launcher.jar reload --app-id my-app --yaml updated-app.yaml
```

## State Preservation Best Practices

### What to Preserve

**DO preserve**:
- Application configuration
- In-memory caches
- Request counters and statistics
- Connection pools (if they can be serialized/recreated)
- Session data

**DON'T preserve**:
- Thread instances (threads belong to the old classloader)
- Open file handles (close and reopen them)
- Network sockets (close and reopen them)
- Native resources (release and reacquire them)
- Class instances from the old classloader (serialize to primitives/collections)

### Safe State Serialization

Only put serializable data into the state map:

```java
@Override
public Map<String, Object> beforeReload() throws Exception {
    Map<String, Object> state = new HashMap<>();
    
    // ✅ Good: primitives and standard collections
    state.put("count", requestCount);
    state.put("cache", new HashMap<>(cache));
    
    // ❌ Bad: custom class instances (may have old classloader references)
    state.put("myObject", someCustomObject);
    
    // ✅ Good: serialize custom objects to JSON or primitives
    state.put("myObjectJson", objectMapper.writeValueAsString(someCustomObject));
    
    return state;
}

@Override
public void afterReload(ApplicationContext context, Map<String, Object> state) throws Exception {
    requestCount = (Integer) state.get("count");
    cache = (Map<String, String>) state.get("cache");
    
    // Deserialize custom objects
    String json = (String) state.get("myObjectJson");
    someCustomObject = objectMapper.readValue(json, MyCustomClass.class);
}
```

## Rollback on Failure

If reload fails (e.g., new version throws exception during `start()`), platform-java automatically rolls back:

1. The new classloader is discarded
2. The old classloader is kept active
3. The old application instance continues running
4. An exception is thrown to the caller

No state is lost during a failed reload.

## Thread Safety

The reload process is fully synchronized:

- Only one reload can occur at a time per application
- All application lifecycle operations (start, stop, reload) are mutually exclusive
- State capture and restore happen atomically

## Limitations

### Not Reloadable

Hot reload does NOT work for:

- **Native image applications** (nativeImage=true) - these run as external processes
- **Applications not implementing ReloadableApplication** - reload will work but state is lost
- **Running threads** - threads from old classloader are stopped, new threads are started

### Class Compatibility

Hot reload works best when:

- Public API signatures remain compatible (method names, parameters)
- Serialized state format is forward-compatible
- Database schema changes are backward-compatible

Breaking changes may require migration logic in `afterReload()`.

## Performance Considerations

### Reload Time

Typical reload takes:
- **Classloader creation**: 50-200ms (depends on JAR size)
- **State capture**: 10-100ms (depends on state size)
- **Stop old instance**: 100-500ms (depends on application cleanup)
- **Start new instance**: 100-1000ms (depends on application initialization)
- **State restore**: 10-100ms (depends on state size)

**Total**: 270ms - 2 seconds for most applications

### Memory Usage

During reload:
- Both old and new classloaders are in memory simultaneously
- Old classloader is eligible for GC after all threads finish
- Typical memory overhead: 1.5x - 2x application JAR size

### Garbage Collection

Old classloaders are garbage collected when:
- All threads using the old classloader have stopped
- No objects from the old classloader are reachable
- Reference count reaches zero

Monitor with: `-XX:+TraceClassLoading -XX:+TraceClassUnloading`

## Monitoring and Metrics

### Reload Events

Monitor reload events via:

```java
ApplicationContext context = manager.getApplicationContext("my-app");
ClassLoaderVersion version = context.getClassLoaderVersion();

System.out.println("Current version: " + version.getVersion());
System.out.println("Reference count: " + version.getReferenceCount());
```

### Logs

Reload operations are logged:

```
INFO  [main] o.f.j.c.ApplicationReloader - [my-app] Starting hot reload
INFO  [main] o.f.j.c.ApplicationReloader - [my-app] Captured state with 3 entries
INFO  [main] o.f.j.c.ApplicationReloader - [my-app] Stopped old instance (version 1)
INFO  [main] o.f.j.c.ApplicationReloader - [my-app] Created new classloader (version 2)
INFO  [main] o.f.j.c.ApplicationReloader - [my-app] Started new instance
INFO  [main] o.f.j.c.ApplicationReloader - [my-app] Restored state with 3 entries
INFO  [main] o.f.j.c.ApplicationReloader - [my-app] Hot reload completed successfully in 450ms
```

## Example: Stateful Web Service

```java
public class WebServiceApp implements ReloadableApplication {
    
    private HttpServer server;
    private Map<String, Session> sessions;
    private AtomicLong requestCounter;
    
    @Override
    public void start(ApplicationContext context) throws Exception {
        sessions = new ConcurrentHashMap<>();
        requestCounter = new AtomicLong(0);
        
        server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/api", this::handleRequest);
        server.start();
    }
    
    @Override
    public void stop() throws Exception {
        server.stop(5);  // 5 second graceful shutdown
    }
    
    @Override
    public Map<String, Object> beforeReload() throws Exception {
        Map<String, Object> state = new HashMap<>();
        
        // Serialize sessions to JSON
        Map<String, String> serializedSessions = new HashMap<>();
        sessions.forEach((id, session) -> {
            serializedSessions.put(id, session.toJson());
        });
        
        state.put("sessions", serializedSessions);
        state.put("requestCount", requestCounter.get());
        
        return state;
    }
    
    @Override
    public void afterReload(ApplicationContext context, Map<String, Object> state) throws Exception {
        // Restore request counter
        long count = (Long) state.get("requestCount");
        requestCounter = new AtomicLong(count);
        
        // Deserialize sessions
        sessions = new ConcurrentHashMap<>();
        Map<String, String> serializedSessions = (Map<String, String>) state.get("sessions");
        serializedSessions.forEach((id, json) -> {
            sessions.put(id, Session.fromJson(json));
        });
        
        logger.info("Restored {} sessions and {} total requests", 
                    sessions.size(), requestCounter.get());
    }
    
    private void handleRequest(HttpExchange exchange) throws IOException {
        requestCounter.incrementAndGet();
        // ... handle request
    }
}
```

## Troubleshooting

### Reload Fails with ClassCastException

**Cause**: Trying to cast objects from old classloader to classes in new classloader.

**Solution**: Serialize objects to primitives/JSON in `beforeReload()`, deserialize in `afterReload()`:

```java
// ❌ Bad
state.put("user", userObject);  // userObject uses old classloader

// ✅ Good
state.put("userJson", objectMapper.writeValueAsString(userObject));
```

### Old Classloader Not Garbage Collected

**Cause**: References to old classloader still exist (threads, static fields, etc.)

**Solution**: 
- Ensure all threads from old application are stopped
- Avoid static fields holding application state
- Use `-XX:+TraceClassUnloading` to verify GC

### State Not Restored After Reload

**Cause**: `beforeReload()` not called or returned null/empty map.

**Solution**:
- Verify `implements ReloadableApplication`
- Check logs for "Captured state with N entries"
- Ensure `preserveState=true` in descriptor

## See Also

- [Application Lifecycle](LIFECYCLE.md)
- [Application Dependencies](APPLICATION_DEPENDENCIES.md)
- [REST API Reference](REST_API.md)
