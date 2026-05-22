# JPlatform JVMTI Agent

Native JVMTI-based heap profiler for precise memory tracking by ClassLoader.

## Overview

The jplatform-jvmti-agent module provides a native JVMTI (JVM Tool Interface) agent that enables precise heap profiling for JPlatform applications. Unlike estimation-based approaches, this agent directly iterates through the JVM heap to measure exact memory usage for objects loaded by specific ClassLoaders.

## Requirements

### Build Requirements

- **GCC** (GNU Compiler Collection) - for compiling C code
- **JDK with headers** - JDK installation must include native headers (`$JAVA_HOME/include`)
- **Maven** - for building the project
- **Linux x64** - currently targets Linux; can be adapted for other platforms

### Runtime Requirements

- **JVM startup flag** - The agent must be loaded at JVM startup
- **Native library** - The compiled `.so` file must be accessible

## Building

### With Native Compilation

If you have GCC and JDK headers installed:

```bash
cd /path/to/jplatform
mvn clean install -pl jplatform-jvmti-agent
```

The compiled native library will be placed in:
- `target/libjplatform-agent.so`
- `target/classes/native/linux-x64/libjplatform-agent.so` (packaged in JAR)

### Skip Native Compilation

If native build tools are not available, you can build without native compilation:

```bash
mvn clean install -pl jplatform-jvmti-agent -DskipNative=true
```

This will build the Java wrapper classes but skip native library compilation. The profiler will not be functional but won't break the build.

## Usage

### Loading the Agent

The JVMTI agent must be loaded when the JVM starts. There are two approaches:

#### Option 1: Command Line Argument

```bash
java -agentpath:/path/to/libjplatform-agent.so -jar your-application.jar
```

#### Option 2: Environment Variable

```bash
export JAVA_TOOL_OPTIONS="-agentpath:/path/to/libjplatform-agent.so"
java -jar your-application.jar
```

### Java Code

```java
import org.flossware.jplatform.jvmti.JvmtiHeapProfiler;
import org.flossware.jplatform.monitoring.HeapProfiler;

// Check if JVMTI agent is available
if (JvmtiHeapProfiler.isAvailable()) {
    HeapProfiler profiler = new JvmtiHeapProfiler();
    profiler.enableProfiling("my-application");
    
    ClassLoader appLoader = getApplicationClassLoader();
    
    // Get total heap usage
    long heapBytes = profiler.getHeapUsageBytes(appLoader);
    System.out.println("Heap usage: " + heapBytes + " bytes");
    
    // Get breakdown by class
    Map<String, Long> byClass = profiler.getHeapByClass(appLoader);
    byClass.forEach((className, bytes) -> {
        System.out.println(className + ": " + bytes + " bytes");
    });
    
    profiler.disableProfiling("my-application");
} else {
    System.err.println("JVMTI agent not available, using fallback approach");
    // Use estimation-based profiling instead
}
```

### Integration with JPlatform

The ApplicationResourceMonitor can automatically detect and use JVMTI profiling:

```java
ApplicationResourceMonitor monitor = new ApplicationResourceMonitor(
    applicationId,
    classLoader,
    config
);

// If JVMTI is available, it will be used automatically
// Otherwise, estimation-based profiling is used
```

## Performance Considerations

- **Heap iteration cost**: Iterating through the heap can be expensive on large heaps (100+ GB)
- **Recommended pattern**: Cache results and update periodically rather than calling on every request
- **Minimal overhead**: When not actively profiling, the agent adds negligible overhead
- **No heap dumps**: Uses efficient JVMTI callbacks, not full heap dumps

## Architecture

```
jplatform-jvmti-agent/
├── pom.xml                          # Maven build configuration
├── src/main/java/
│   └── org/flossware/jplatform/jvmti/
│       └── JvmtiHeapProfiler.java   # Java wrapper for native calls
└── src/main/c/
    └── jplatform_agent.c            # Native JVMTI implementation
```

## Troubleshooting

### Native Library Not Found

```
java.lang.UnsatisfiedLinkError: no jplatform-agent in java.library.path
```

**Solution**: Ensure the agent is loaded at startup with `-agentpath`, not via `System.loadLibrary()`.

### JVMTI Capabilities Error

```
ERROR: Unable to add JVMTI capabilities
```

**Solution**: Ensure the JVM supports JVMTI (most modern JVMs do). Try a different JVM vendor if issues persist.

### Compilation Errors

```
fatal error: jni.h: No such file or directory
```

**Solution**: Install JDK with headers, ensure `$JAVA_HOME` is set correctly, or use `-DskipNative=true`.

## Platform Support

Currently supports:
- **Linux x64**

Can be adapted for:
- **macOS** (change `.so` to `.dylib`, update include paths)
- **Windows** (change `.so` to `.dll`, use MSVC compiler)

## License

Part of the JPlatform project. See parent LICENSE file.

## See Also

- [jplatform-monitoring](../jplatform-monitoring) - Monitoring interfaces and estimation-based profiling
- [jplatform-api](../jplatform-api) - Core JPlatform API
- [JVMTI Documentation](https://docs.oracle.com/en/java/javase/17/docs/specs/jvmti.html)
