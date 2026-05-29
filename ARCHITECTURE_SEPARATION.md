# Architecture Separation: classloader-java vs platform-java-classloader

## Clean Separation of Concerns

```
┌─────────────────────────────────────────────────────────────┐
│                    platform-java-classloader                    │
│                    (Platform-Specific)                       │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  • IsolatedClassLoader                                      │
│  • ApplicationDescriptor → ClassSource translation          │
│  • Platform API isolation (org.flossware.platform-java.api.*)   │
│  • ApplicationManager integration                           │
│  • Platform logging/metrics integration                     │
│  • Platform-specific cache directories                      │
│  • Application lifecycle coordination                       │
│                                                             │
│  Dependencies: platform-java-api, classloader-java                  │
│                                                             │
└────────────────────┬────────────────────────────────────────┘
                     │ uses
                     ▼
┌─────────────────────────────────────────────────────────────┐
│                      classloader-java                           │
│                  (General-Purpose, Reusable)                │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  Core Class Loading:                                        │
│  • JClassLoader                                             │
│  • ClassSource interface + 20+ implementations              │
│  • ClassCache (filesystem, in-memory)                       │
│  • AuthConfig (Basic, Bearer)                               │
│                                                             │
│  NEW - Delegation Strategies:                               │
│  • DelegationStrategy interface                             │
│  • ParentFirstDelegation (default)                          │
│  • ParentLastDelegation (isolation)                         │
│  • CustomDelegation (custom rules)                          │
│                                                             │
│  NEW - Lifecycle Hooks:                                     │
│  • ClassLoaderLifecycleListener interface                   │
│  • ResourceTrackingListener (cleanup)                       │
│  • LoggingListener (debugging)                              │
│  • ClassLoadEvent                                           │
│                                                             │
│  Dependencies: None (pure Java + optional integrations)     │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## What Goes Where

### classloader-java (Reusable Library)

**Question to ask**: "Could any project needing custom class loading use this?"

✅ **YES - Put in classloader-java:**
- Delegation strategies (parent-first, parent-last, custom)
- Lifecycle hooks and listeners
- Resource tracking utilities
- All ClassSource implementations
- Caching mechanisms
- Authentication support

❌ **NO - Don't put in classloader-java:**
- ApplicationDescriptor knowledge
- platform-java-specific API isolation rules
- Platform service integration
- Application lifecycle management

### platform-java-classloader (Platform-Specific)

**Question to ask**: "Is this specific to platform-java's architecture?"

✅ **YES - Put in platform-java-classloader:**
- IsolatedClassLoader (wrapper around JClassLoader)
- ApplicationDescriptor → JClassLoader configuration
- Platform API sharing (`org.flossware.platform-java.api.*`)
- Integration with ApplicationManager
- Platform logging/metrics hooks
- Platform-specific caching strategies

❌ **NO - Don't put in platform-java-classloader:**
- Generic class loading mechanics
- ClassSource implementations
- Delegation strategy logic
- General-purpose lifecycle hooks

## Code Comparison

### Example 1: Parent-Last Delegation

**❌ WRONG - In platform-java-classloader:**
```java
// NO - This is reusable, not platform-specific!
public class ParentLastDelegation {
    // Generic parent-last logic
}
```

**✅ RIGHT - In classloader-java:**
```java
// YES - Generic delegation strategy
public class ParentLastDelegation implements DelegationStrategy {
    private final Set<String> alwaysParentPrefixes;
    // Generic parent-last logic usable by ANY project
}
```

**✅ RIGHT - In platform-java-classloader (uses it):**
```java
// Platform-specific: Configure with platform API prefix
JClassLoader.builder()
    .parentLast("org.flossware.platform-java.api.")  // Platform-specific prefix
    .build();
```

### Example 2: Resource Tracking

**❌ WRONG - In platform-java-classloader:**
```java
// NO - Resource tracking is generally useful!
public class ResourceTracker {
    Set<String> loadedClasses;
    List<AutoCloseable> resources;
}
```

**✅ RIGHT - In classloader-java:**
```java
// YES - Generic resource tracking listener
public class ResourceTrackingListener implements ClassLoaderLifecycleListener {
    // Generic resource tracking usable by ANY project
}
```

**✅ RIGHT - In platform-java-classloader (uses it):**
```java
// Platform-specific: Use tracker for application cleanup
ResourceTrackingListener tracker = new ResourceTrackingListener();
JClassLoader.builder()
    .addListener(tracker)
    .build();

// Later, on undeploy:
tracker.closeAllResources();  // Platform-specific cleanup timing
```

### Example 3: ClassSource from Descriptor

**❌ WRONG - In classloader-java:**
```java
// NO - classloader-java shouldn't know about ApplicationDescriptor!
public void addFromDescriptor(ApplicationDescriptor desc) {
    // ...
}
```

**✅ RIGHT - In platform-java-classloader:**
```java
// YES - Platform-specific translation
private static void addClassSourcesFromDescriptor(
        JClassLoader.Builder builder, 
        ApplicationDescriptor descriptor) {
    for (URI entry : descriptor.getClasspathEntries()) {
        // Platform-specific: Parse descriptor format
        if (entry.getScheme().equals("maven")) {
            builder.addMavenCentral(entry.getSchemeSpecificPart());
        }
        // ...
    }
}
```

## Dependency Graph

```
platform-java-launcher
    ↓
platform-java-core
    ↓
platform-java-classloader ─────→ classloader-java
    ↓                             ↓
platform-java-api              (no dependencies)
```

**Key Points:**
- classloader-java has NO dependency on platform-java
- platform-java-classloader depends on BOTH classloader-java and platform-java-api
- classloader-java remains reusable by other projects

## Use Cases

### classloader-java can be used by:
1. ✅ platform-java (application server)
2. ✅ Plugin systems (isolated plugins)
3. ✅ OSGi containers (module isolation)
4. ✅ Testing frameworks (test isolation)
5. ✅ Multi-tenant applications (tenant isolation)
6. ✅ Hot reload systems (resource cleanup)
7. ✅ Any project needing custom class loading

### platform-java-classloader is used by:
1. ✅ platform-java only
2. ❌ Not reusable outside platform-java

## Benefits

### For classloader-java Project:
- ✅ Becomes more powerful (delegation strategies, hooks)
- ✅ Remains general-purpose and reusable
- ✅ Attracts wider user base
- ✅ No platform-specific coupling

### For platform-java Project:
- ✅ Doesn't reinvent class loading
- ✅ Gets 20+ class sources for free
- ✅ Thin wrapper focused on platform concerns
- ✅ Easier to maintain

### For Other Projects:
- ✅ Can use classloader-java for custom class loading needs
- ✅ Don't need platform-java to get isolation features
- ✅ Proven, tested class loading library

## Next Steps

1. **Enhance classloader-java** (in classloader-java repo):
   - Add `DelegationStrategy` interface and implementations
   - Add `ClassLoaderLifecycleListener` interface
   - Add `ResourceTrackingListener` implementation
   - Update `JClassLoader` to support strategies and listeners
   - Add tests
   - Release new version (e.g., 2.0)

2. **Implement platform-java-classloader** (in platform-java repo):
   - Depend on classloader-java 2.0+
   - Create `IsolatedClassLoader` wrapper
   - Implement platform-specific configuration
   - Integrate with `ApplicationDescriptor`
   - Add platform-specific listeners
   - Add tests

3. **Documentation**:
   - Update classloader-java README with new features
   - Document platform-java-classloader usage
   - Create migration guide if needed

## Summary

**classloader-java = The Engine (reusable)**
- How to load class bytes from anywhere
- How to delegate (parent-first, parent-last, custom)
- How to track resources
- How to emit lifecycle events

**platform-java-classloader = The Steering Wheel (platform-specific)**
- Which classes to isolate (platform API vs application)
- When to create/destroy class loaders (application lifecycle)
- Where to cache (platform cache directories)
- What to log/monitor (platform metrics)

Perfect separation of concerns! 🎯
