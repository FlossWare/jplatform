# ClassLoader Best Practices - Avoiding Memory Leaks

## Overview

JPlatform uses isolated ClassLoaders to provide application isolation. While powerful, ClassLoaders can cause memory leaks if applications don't follow best practices. This document outlines common leak sources and how to avoid them.

## What is a ClassLoader Leak?

A ClassLoader leak occurs when the JVM cannot garbage collect a ClassLoader after an application is undeployed because:
- Some code still holds a reference to a class loaded by that ClassLoader
- That class holds a reference back to its ClassLoader
- The ClassLoader holds references to all classes it loaded
- This prevents garbage collection of the entire application

**Result**: Memory leak - the application's memory is never released even after undeploy.

## Common Leak Sources

### 1. ThreadLocal Variables ⚠️ HIGH RISK

**Problem**: ThreadLocals store per-thread data but are never automatically cleaned up.

**Bad Example**:
```java
public class MyApp implements Application {
    private static ThreadLocal<Connection> connectionHolder = new ThreadLocal<>();
    
    @Override
    public void start(ApplicationContext context) {
        connectionHolder.set(createConnection());  // Creates leak!
    }
    
    @Override
    public void stop() {
        // ThreadLocal not cleaned up!
    }
}
```

**Good Example**:
```java
public class MyApp implements Application {
    private static ThreadLocal<Connection> connectionHolder = new ThreadLocal<>();
    
    @Override
    public void start(ApplicationContext context) {
        connectionHolder.set(createConnection());
    }
    
    @Override
    public void stop() {
        // Clean up ThreadLocal before stopping
        Connection conn = connectionHolder.get();
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                // Log error
            }
        }
        connectionHolder.remove();  // ✅ Remove ThreadLocal value
    }
}
```

**Best Practice**: Always call `threadLocal.remove()` in your `stop()` method.

---

### 2. Static Fields ⚠️ HIGH RISK

**Problem**: Static fields are stored in the Class object, which is loaded by your ClassLoader. They prevent the ClassLoader from being garbage collected.

**Bad Example**:
```java
public class MyApp implements Application {
    private static List<String> globalCache = new ArrayList<>();  // Leak!
    private static final Logger LOG = LoggerFactory.getLogger(MyApp.class);  // OK
    
    @Override
    public void start(ApplicationContext context) {
        globalCache.add("data");
    }
}
```

**Good Example**:
```java
public class MyApp implements Application {
    // ✅ Use instance fields instead of static
    private List<String> cache = new ArrayList<>();
    
    // ✅ Static constants for primitives/Strings are OK
    private static final String APP_NAME = "MyApp";
    
    // ✅ Static loggers are OK (SLF4J is in parent classloader)
    private static final Logger LOG = LoggerFactory.getLogger(MyApp.class);
    
    @Override
    public void start(ApplicationContext context) {
        cache.add("data");
    }
    
    @Override
    public void stop() {
        cache.clear();  // Cleanup
    }
}
```

**Best Practice**: 
- Avoid static fields holding mutable data
- Use instance fields instead
- Static fields for primitives, Strings, and platform classes are OK

---

### 3. JDBC Drivers ⚠️ HIGH RISK

**Problem**: JDBC drivers registered with `DriverManager` are never automatically deregistered.

**Bad Example**:
```java
public class MyApp implements Application {
    @Override
    public void start(ApplicationContext context) {
        // Driver auto-registers with DriverManager on class load
        Class.forName("org.postgresql.Driver");  // Leak!
        Connection conn = DriverManager.getConnection("jdbc:postgresql://...");
    }
}
```

**Good Example**:
```java
public class MyApp implements Application {
    private Driver driver;
    
    @Override
    public void start(ApplicationContext context) throws Exception {
        // Manually register driver so we can deregister it
        driver = new org.postgresql.Driver();
        DriverManager.registerDriver(driver);
        
        Connection conn = DriverManager.getConnection("jdbc:postgresql://...");
    }
    
    @Override
    public void stop() throws Exception {
        // ✅ Deregister driver
        if (driver != null) {
            DriverManager.deregisterDriver(driver);
        }
    }
}
```

**Note**: JPlatform's `ClassLoaderCleanupUtil` automatically deregisters JDBC drivers, but it's still best practice to do it manually.

**Best Practice**: Explicitly register and deregister JDBC drivers.

---

### 4. Thread Creation ⚠️ MEDIUM RISK

**Problem**: Threads hold references to their context ClassLoader.

**Bad Example**:
```java
public class MyApp implements Application {
    @Override
    public void start(ApplicationContext context) {
        // Creates thread that runs forever
        new Thread(() -> {
            while (true) {
                // Do work
            }
        }).start();  // Leak - thread never stops!
    }
}
```

**Good Example**:
```java
public class MyApp implements Application {
    private volatile boolean running = true;
    private Thread worker;
    
    @Override
    public void start(ApplicationContext context) {
        worker = new Thread(() -> {
            while (running) {
                // Do work
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        worker.start();
    }
    
    @Override
    public void stop() {
        // ✅ Stop thread gracefully
        running = false;
        if (worker != null) {
            worker.interrupt();
            try {
                worker.join(5000);  // Wait up to 5 seconds
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
```

**Better**: Use the platform's thread pool instead:
```java
public class MyApp implements Application {
    @Override
    public void start(ApplicationContext context) {
        // ✅ Use platform thread pool (managed by JPlatform)
        context.getThreadPool().submit(() -> {
            // Do work
        });
    }
}
```

**Best Practice**: Use platform thread pools or ensure threads are stopped in `stop()`.

---

### 5. Shutdown Hooks ⚠️ MEDIUM RISK

**Problem**: Shutdown hooks registered with `Runtime.addShutdownHook()` hold thread references.

**Bad Example**:
```java
public class MyApp implements Application {
    @Override
    public void start(ApplicationContext context) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            // Cleanup
        }));  // Leak if not removed!
    }
}
```

**Good Example**:
```java
public class MyApp implements Application {
    private Thread shutdownHook;
    
    @Override
    public void start(ApplicationContext context) {
        shutdownHook = new Thread(() -> {
            // Cleanup
        });
        Runtime.getRuntime().addShutdownHook(shutdownHook);
    }
    
    @Override
    public void stop() {
        // ✅ Remove shutdown hook
        if (shutdownHook != null) {
            try {
                Runtime.getRuntime().removeShutdownHook(shutdownHook);
            } catch (IllegalStateException e) {
                // Shutdown in progress, ignore
            }
        }
    }
}
```

**Best Practice**: Remove shutdown hooks in `stop()` method.

---

### 6. JMX MBeans ⚠️ MEDIUM RISK

**Problem**: MBeans registered with MBeanServer hold references.

**Bad Example**:
```java
public class MyApp implements Application {
    @Override
    public void start(ApplicationContext context) throws Exception {
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        ObjectName name = new ObjectName("com.myapp:type=Stats");
        mbs.registerMBean(new MyStats(), name);  // Leak if not unregistered!
    }
}
```

**Good Example**:
```java
public class MyApp implements Application {
    private ObjectName mbeanName;
    
    @Override
    public void start(ApplicationContext context) throws Exception {
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        mbeanName = new ObjectName("com.myapp:type=Stats");
        mbs.registerMBean(new MyStats(), mbeanName);
    }
    
    @Override
    public void stop() throws Exception {
        // ✅ Unregister MBean
        if (mbeanName != null) {
            MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
            if (mbs.isRegistered(mbeanName)) {
                mbs.unregisterMBean(mbeanName);
            }
        }
    }
}
```

**Best Practice**: Unregister MBeans in `stop()` method.

---

### 7. Logging Framework Context ⚠️ LOW RISK

**Problem**: Some logging frameworks (Logback, Log4j) cache loggers by ClassLoader.

**Best Practice**: Use SLF4J API (provided by platform). JPlatform handles the cleanup automatically.

```java
// ✅ Good - use SLF4J
private static final Logger logger = LoggerFactory.getLogger(MyApp.class);

// ❌ Bad - don't use Logback/Log4j directly
private static final ch.qos.logback.classic.Logger logger = ...;
```

---

## Testing for Leaks

### Enable Leak Detection

Run JPlatform with leak detection enabled:

```bash
java -Djplatform.debug.detectLeaks=true -jar jplatform-launcher.jar
```

When you undeploy an application, JPlatform will:
1. Trigger garbage collection
2. Check if the ClassLoader was collected
3. Log a warning if a leak is detected

### Manual Testing

```java
// In your application's stop() method, verify cleanup
@Override
public void stop() {
    // Your cleanup code
    
    // Verify no threads are running
    Thread.getAllStackTraces().keySet().stream()
        .filter(t -> t.getName().contains("myapp"))
        .forEach(t -> {
            System.err.println("WARNING: Thread still running: " + t.getName());
        });
}
```

### Heap Profiler

Use VisualVM, JProfiler, or YourKit to find ClassLoader leaks:

1. Deploy and start your application
2. Take a heap dump
3. Undeploy your application
4. Trigger GC (`System.gc()`)
5. Take another heap dump
6. Compare dumps - your application's ClassLoader should be gone

If it's still present, the profiler will show the reference chain keeping it alive.

---

## Checklist for Application Developers

Before deploying your application to JPlatform, verify:

- [ ] No static fields holding mutable data
- [ ] All ThreadLocals are cleaned up in `stop()`
- [ ] All threads are stopped or use platform thread pool
- [ ] JDBC drivers are deregistered in `stop()`
- [ ] Shutdown hooks are removed in `stop()`
- [ ] JMX MBeans are unregistered in `stop()`
- [ ] No infinite loops in background threads
- [ ] All resources (files, sockets) are closed in `stop()`
- [ ] Tested with leak detection enabled

---

## Summary of Best Practices

| Pattern | Risk Level | Solution |
|---------|-----------|----------|
| Static mutable fields | HIGH | Use instance fields |
| ThreadLocal | HIGH | Call `.remove()` in `stop()` |
| JDBC drivers | HIGH | Deregister in `stop()` |
| Custom threads | MEDIUM | Stop gracefully or use platform pool |
| Shutdown hooks | MEDIUM | Remove in `stop()` |
| JMX MBeans | MEDIUM | Unregister in `stop()` |
| Logging | LOW | Use SLF4J API |

**Golden Rule**: If you allocate it in `start()`, clean it up in `stop()`.

---

## See Also

- [Application Lifecycle](LIFECYCLE.md)
- [Hot Reload](HOT_RELOAD.md)
- [Resource Monitoring](RESOURCE_MONITORING.md)
