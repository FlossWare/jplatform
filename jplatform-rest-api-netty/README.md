# JPlatform REST API - Netty

High-performance Netty-based REST API server implementation for JPlatform.

## Features

- **High Performance**: Built on Netty's non-blocking I/O framework
- **Route Registration**: Register custom request handlers
- **JSON Support**: Automatic JSON request/response handling
- **Keep-Alive**: Configurable HTTP keep-alive connections
- **Thread Pool**: Customizable boss and worker thread pools
- **Configurable**: Flexible server configuration options

## Dependencies

```xml
<dependency>
    <groupId>org.flossware.jplatform</groupId>
    <artifactId>jplatform-rest-api-netty</artifactId>
    <version>1.1</version>
</dependency>
```

## Usage

### Basic Server

```java
NettyApiServerConfig config = NettyApiServerConfig.builder()
    .host("0.0.0.0")
    .port(8080)
    .build();

try (NettyApiServer server = new NettyApiServer(config)) {
    // Register routes
    server.addRoute("/api/hello", input -> {
        return "{\"message\":\"Hello, World!\"}";
    });
    
    server.addRoute("/api/echo", input -> {
        return "{\"echo\":\"" + input + "\"}";
    });
    
    // Start server
    server.start();
    
    System.out.println("Server running on port " + server.getPort());
    
    // Server runs until stopped...
    Thread.sleep(60000);
}
```

### Production Configuration

```java
NettyApiServerConfig config = NettyApiServerConfig.builder()
    .host("0.0.0.0")
    .port(8080)
    .bossThreads(2)
    .workerThreads(16)
    .maxContentLength(131072)
    .keepAlive(true)
    .backlog(256)
    .build();

NettyApiServer server = new NettyApiServer(config);

// Register API endpoints
server.addRoute("/api/users", request -> {
    // Handle users endpoint
    return "{\"users\":[]}";
});

server.addRoute("/api/products", request -> {
    // Handle products endpoint
    return "{\"products\":[]}";
});

server.start();
```

### Dynamic Route Management

```java
NettyApiServerConfig config = NettyApiServerConfig.builder()
    .port(8080)
    .build();

NettyApiServer server = new NettyApiServer(config);
server.start();

// Add routes dynamically
server.addRoute("/api/status", input -> "{\"status\":\"running\"}");

// Update existing routes
server.addRoute("/api/status", input -> "{\"status\":\"healthy\"}");

// Remove routes
server.removeRoute("/api/status");
```

### Custom Request Processing

```java
NettyApiServer server = new NettyApiServer(
    NettyApiServerConfig.builder().port(8080).build()
);

server.addRoute("/api/process", requestBody -> {
    try {
        // Parse request
        Map<String, Object> request = parseJson(requestBody);
        
        // Process request
        Object result = processRequest(request);
        
        // Return JSON response
        return toJson(result);
    } catch (Exception e) {
        return "{\"error\":\"" + e.getMessage() + "\"}";
    }
});

server.start();
```

## Configuration Options

| Option | Default | Description |
|--------|---------|-------------|
| host | 0.0.0.0 | Server bind address |
| port | 8080 | Server port |
| bossThreads | 1 | Number of boss threads (accept connections) |
| workerThreads | 0 | Number of worker threads (0 = auto-detect) |
| maxContentLength | 65536 | Maximum request content length in bytes |
| keepAlive | true | Enable HTTP keep-alive connections |
| backlog | 128 | Server socket backlog size |

## Thread Safety

All operations are thread-safe. Multiple threads can safely register/unregister routes and start/stop the server.

## Performance Tuning

### Thread Pool Sizing

```java
int cores = Runtime.getRuntime().availableProcessors();

NettyApiServerConfig config = NettyApiServerConfig.builder()
    .bossThreads(1)  // Usually 1 is sufficient
    .workerThreads(cores * 2)  // 2x CPU cores is a good starting point
    .build();
```

### Content Length

```java
// For small JSON payloads
NettyApiServerConfig config = NettyApiServerConfig.builder()
    .maxContentLength(8192)  // 8KB
    .build();

// For larger file uploads
NettyApiServerConfig config = NettyApiServerConfig.builder()
    .maxContentLength(10485760)  // 10MB
    .build();
```

### Backlog

```java
// For high-traffic scenarios
NettyApiServerConfig config = NettyApiServerConfig.builder()
    .backlog(1024)  // Larger backlog for more concurrent connections
    .build();
```

## Error Handling

- Route not found returns 404 with JSON error
- Handler exceptions return 500 with error message
- All errors are logged with SLF4J

## Thread Safety

The server is thread-safe for:
- Route registration/removal
- Server start/stop operations
- Request handling (concurrent requests handled safely)
