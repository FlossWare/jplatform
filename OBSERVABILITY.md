# Observability Guide

## Overview

platform-java provides comprehensive observability through multiple mechanisms:

- **OpenTelemetry** - Distributed tracing and metrics export via OTLP
- **JMX** - Java Management Extensions for monitoring and management
- **Prometheus** - Time-series metrics in Prometheus text format
- **Structured Logging** - Per-application logs with context (app_id, trace_id, span_id)
- **Resource Monitoring** - CPU, memory, thread tracking with history

## OpenTelemetry Integration

### Configuration

#### In platform.yaml

```yaml
metrics:
  opentelemetry:
    enabled: true
    endpoint: "http://localhost:4317"  # OTLP gRPC endpoint
```

#### Programmatic Configuration

```java
import org.flossware.platform-java.otel.OpenTelemetryMetricsExporter;

OpenTelemetryMetricsExporter exporter = new OpenTelemetryMetricsExporter("http://localhost:4317");
exporter.start();

// Register application
exporter.registerApplication("my-app", context);
```

### Exported Metrics

platform-java exports the following metrics to OpenTelemetry Collector:

| Metric Name | Type | Description | Unit | Labels |
|-------------|------|-------------|------|--------|
| `platform-java.app.cpu_time_seconds` | Counter | Total CPU time consumed | seconds | app_id |
| `platform-java.app.heap_used_bytes` | Gauge | Current heap memory usage | bytes | app_id |
| `platform-java.app.thread_count` | Gauge | Current thread count | threads | app_id |

#### Example Metrics Output

```
platform-java.app.cpu_time_seconds{app_id="order-service"} 45.2
platform-java.app.heap_used_bytes{app_id="order-service"} 134217728
platform-java.app.thread_count{app_id="order-service"} 12
```

### Export Interval

Metrics are exported every **60 seconds** to the configured OTLP endpoint.

### Service Name

All metrics are tagged with service name: `platform-java`

### Resource Attributes

```
service.name = platform-java
```

## OpenTelemetry Collector Setup

### Docker Compose Example

```yaml
version: '3'
services:
  otel-collector:
    image: otel/opentelemetry-collector:latest
    command: ["--config=/etc/otel-collector-config.yaml"]
    volumes:
      - ./otel-collector-config.yaml:/etc/otel-collector-config.yaml
    ports:
      - "4317:4317"   # OTLP gRPC
      - "4318:4318"   # OTLP HTTP
      - "55681:55681" # Collector metrics
```

### Collector Configuration

```yaml
# otel-collector-config.yaml
receivers:
  otlp:
    protocols:
      grpc:
        endpoint: 0.0.0.0:4317
      http:
        endpoint: 0.0.0.0:4318

processors:
  batch:
    timeout: 10s

exporters:
  prometheus:
    endpoint: "0.0.0.0:8889"
  
  jaeger:
    endpoint: jaeger:14250
    tls:
      insecure: true
  
  logging:
    loglevel: debug

service:
  pipelines:
    metrics:
      receivers: [otlp]
      processors: [batch]
      exporters: [prometheus, logging]
    
    traces:
      receivers: [otlp]
      processors: [batch]
      exporters: [jaeger, logging]
```

## JMX Monitoring

### Configuration

#### In platform.yaml

```yaml
metrics:
  jmx:
    enabled: true
    port: 9999
    domain: "org.flossware.platform-java"
```

#### Command-Line

```bash
java -jar platform-java-launcher.jar --jmx-port 9999
```

### MBean Attributes

Each application is registered as an MBean with ObjectName:

```
org.flossware.platform-java:type=Application,id={applicationId}
```

**Attributes**:
- `ApplicationId` (String) - Application identifier
- `State` (String) - Current state (RUNNING, STOPPED, etc.)
- `CpuTimeNanos` (long) - Total CPU time in nanoseconds
- `HeapUsedBytes` (long) - Current heap usage
- `ThreadCount` (int) - Active thread count
- `ThreadPoolSize` (int) - Thread pool size
- `ThreadPoolActive` (int) - Active threads in pool
- `ThreadPoolQueued` (int) - Queued tasks
- `ThreadPoolCompleted` (long) - Completed tasks
- `Uptime` (long) - Milliseconds since start

**Operations**:
- `start()` - Start the application
- `stop()` - Stop the application
- `getResourceHistory(int minutes)` - Get resource snapshots

### JConsole Access

```bash
$ jconsole localhost:9999
```

Navigate to MBeans tab → `org.flossware.platform-java` → `Application` → `{app-id}`

### Remote JMX (Optional)

For remote access, configure RMI registry:

```bash
java -Dcom.sun.management.jmxremote \
     -Dcom.sun.management.jmxremote.port=9999 \
     -Dcom.sun.management.jmxremote.authenticate=false \
     -Dcom.sun.management.jmxremote.ssl=false \
     -jar platform-java-launcher.jar
```

## Prometheus Metrics

### Configuration

#### In platform.yaml

```yaml
metrics:
  prometheus:
    enabled: true
    port: 9090
    path: "/metrics"
```

#### Command-Line

```bash
java -jar platform-java-launcher.jar --prometheus --prometheus-port 9090
```

### Metrics Endpoint

Access metrics at: `http://localhost:9090/metrics`

### Exported Metrics

```
# HELP platform-java_app_cpu_time_seconds Total CPU time consumed by the application
# TYPE platform-java_app_cpu_time_seconds counter
platform-java_app_cpu_time_seconds{app_id="order-service"} 45.2

# HELP platform-java_app_heap_used_bytes Current heap memory usage
# TYPE platform-java_app_heap_used_bytes gauge
platform-java_app_heap_used_bytes{app_id="order-service"} 134217728

# HELP platform-java_app_thread_count Current thread count
# TYPE platform-java_app_thread_count gauge
platform-java_app_thread_count{app_id="order-service"} 12

# HELP platform-java_app_state Application state (0=STOPPED, 1=RUNNING, 2=FAILED)
# TYPE platform-java_app_state gauge
platform-java_app_state{app_id="order-service"} 1

# HELP platform-java_app_threadpool_active Active threads in thread pool
# TYPE platform-java_app_threadpool_active gauge
platform-java_app_threadpool_active{app_id="order-service"} 5

# HELP platform-java_app_threadpool_queued Queued tasks in thread pool
# TYPE platform-java_app_threadpool_queued gauge
platform-java_app_threadpool_queued{app_id="order-service"} 0

# HELP platform-java_app_threadpool_completed Completed tasks in thread pool
# TYPE platform-java_app_threadpool_completed counter
platform-java_app_threadpool_completed{app_id="order-service"} 1024
```

### Prometheus Scrape Configuration

```yaml
# prometheus.yml
scrape_configs:
  - job_name: 'platform-java'
    scrape_interval: 15s
    static_configs:
      - targets: ['localhost:9090']
```

## Structured Logging (Future Enhancement)

### Log Format

Per-application logs with structured JSON format:

```json
{
  "timestamp": "2026-05-22T10:30:45.123Z",
  "level": "INFO",
  "logger": "com.example.OrderService",
  "message": "Order processed successfully",
  "app_id": "order-service",
  "trace_id": "4bf92f3577b34da6a3ce929d0e0e4736",
  "span_id": "00f067aa0ba902b7",
  "thread": "order-service-worker-3"
}
```

### Log Aggregation

Logs are written to application-specific files:

```
/var/platform-java/logs/
  ├── order-service.log
  ├── payment-service.log
  └── inventory-service.log
```

### MDC (Mapped Diagnostic Context)

Applications can add context to logs:

```java
import org.slf4j.MDC;

public class OrderService implements Application {
    
    @Override
    public void start(ApplicationContext context) {
        // app_id is automatically set by platform-java
        
        // Add custom context
        MDC.put("customer_id", "12345");
        MDC.put("order_id", "ORD-9876");
        
        logger.info("Processing order");  // Includes customer_id and order_id
        
        MDC.clear();
    }
}
```

## Resource Monitoring

### History Tracking

platform-java tracks resource usage history:

- **Interval**: 5 seconds
- **Retention**: 1 hour (720 snapshots)
- **Metrics**: CPU time, heap usage, thread count

### Accessing History

#### Via REST API

```bash
# Get last 10 minutes of metrics
curl http://localhost:8080/api/applications/my-app/metrics?minutes=10
```

Response:

```json
{
  "applicationId": "my-app",
  "snapshots": [
    {
      "timestamp": "2026-05-22T10:25:00.000Z",
      "cpuTimeNanos": 45200000000,
      "heapUsedBytes": 134217728,
      "threadCount": 12
    },
    ...
  ]
}
```

#### Via JMX

```java
MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
ObjectName name = new ObjectName("org.flossware.platform-java:type=Application,id=my-app");

String history = (String) mbs.invoke(name, "getResourceHistory", 
    new Object[]{10}, new String[]{"int"});
System.out.println(history);
```

## Distributed Tracing (Future Enhancement)

### Trace Context Propagation

platform-java will support trace context propagation across applications using OpenTelemetry:

```java
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;

public class OrderService implements Application {
    
    private Tracer tracer;
    
    @Override
    public void start(ApplicationContext context) {
        // Get tracer from context
        tracer = context.getTracer().orElseThrow();
        
        // Create span
        Span span = tracer.spanBuilder("processOrder").startSpan();
        try {
            // Business logic
            span.setAttribute("order.id", "ORD-9876");
            span.setAttribute("customer.id", "12345");
        } finally {
            span.end();
        }
    }
}
```

### Inter-Application Tracing

Trace context automatically propagates through MessageBus:

```java
// Service A: publish message
messageBus.publish("orders", order);  // Trace context attached

// Service B: receive message
messageBus.subscribe("orders", Order.class, (order) -> {
    // Trace context restored, span linked
    Span span = tracer.spanBuilder("handleOrder")
        .setParent(Context.current())  // Links to Service A's span
        .startSpan();
    // ...
    span.end();
});
```

## Grafana Dashboards

### Example Dashboard

```json
{
  "dashboard": {
    "title": "platform-java Applications",
    "panels": [
      {
        "title": "CPU Usage",
        "targets": [
          {
            "expr": "rate(platform-java_app_cpu_time_seconds[5m])",
            "legendFormat": "{{app_id}}"
          }
        ]
      },
      {
        "title": "Heap Usage",
        "targets": [
          {
            "expr": "platform-java_app_heap_used_bytes",
            "legendFormat": "{{app_id}}"
          }
        ]
      },
      {
        "title": "Thread Count",
        "targets": [
          {
            "expr": "platform-java_app_thread_count",
            "legendFormat": "{{app_id}}"
          }
        ]
      }
    ]
  }
}
```

### PromQL Queries

**CPU usage rate (last 5 minutes)**:
```promql
rate(platform-java_app_cpu_time_seconds[5m])
```

**Top 5 applications by heap usage**:
```promql
topk(5, platform-java_app_heap_used_bytes)
```

**Applications with thread count > 50**:
```promql
platform-java_app_thread_count > 50
```

**Thread pool utilization**:
```promql
platform-java_app_threadpool_active / platform-java_app_threadpool_size * 100
```

## Alerting

### Prometheus Alerts

```yaml
# alerts.yml
groups:
  - name: platform-java
    rules:
      - alert: HighHeapUsage
        expr: platform-java_app_heap_used_bytes > 500000000  # 500MB
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "High heap usage for {{ $labels.app_id }}"
          description: "Heap usage is {{ $value | humanize }}B"
      
      - alert: HighThreadCount
        expr: platform-java_app_thread_count > 100
        for: 2m
        labels:
          severity: warning
        annotations:
          summary: "High thread count for {{ $labels.app_id }}"
          description: "Thread count is {{ $value }}"
      
      - alert: ApplicationDown
        expr: up{job="platform-java"} == 0
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "platform-java is down"
          description: "platform-java has been down for 1 minute"
```

### Resource Enforcement Alerts

platform-java can automatically enforce resource limits:

```yaml
# platform.yaml
applications:
  - applicationId: my-app
    resources:
      maxHeapMB: 512
      memoryEnforcementAction: SHUTDOWN  # Auto-shutdown on quota violation
      
      maxCpuTimeSeconds: 300
      cpuEnforcementAction: THROTTLE  # Slow down on quota violation
      
      maxThreads: 50
      threadEnforcementAction: NOTIFY  # Only log/notify
```

See [Resource Enforcement](RESOURCE_ENFORCEMENT.md) for details.

## Best Practices

### 1. Enable All Metrics Exporters

```yaml
metrics:
  jmx:
    enabled: true
    port: 9999
  
  prometheus:
    enabled: true
    port: 9090
  
  opentelemetry:
    enabled: true
    endpoint: "http://otel-collector:4317"
```

### 2. Use Structured Logging

```java
// ✅ Good: structured logging
logger.info("Order processed: orderId={}, customerId={}, amount={}", 
    orderId, customerId, amount);

// ❌ Bad: string concatenation
logger.info("Order processed: " + orderId + ", customer: " + customerId);
```

### 3. Add Meaningful Labels

```java
// ✅ Good: add business context to spans
span.setAttribute("order.id", orderId);
span.setAttribute("order.amount", amount);
span.setAttribute("customer.tier", customerTier);

// ❌ Bad: no context
span.setAttribute("data", jsonString);  // Opaque data
```

### 4. Monitor Resource Quotas

Set up alerts for applications approaching quota limits:

```promql
# Alert when heap usage > 90% of quota
(platform-java_app_heap_used_bytes / 536870912) * 100 > 90
```

### 5. Correlate Logs and Traces

Use trace IDs in log messages for correlation:

```java
Span span = tracer.getCurrentSpan();
MDC.put("trace_id", span.getSpanContext().getTraceId());
logger.info("Processing request");  // Log includes trace_id
```

## Troubleshooting

### OpenTelemetry Not Exporting

**Check endpoint connectivity**:
```bash
$ curl http://localhost:4317
# Should respond (even with error is OK, means endpoint is reachable)
```

**Check logs**:
```
INFO  OpenTelemetryMetricsExporter initialized with endpoint: http://localhost:4317
INFO  OpenTelemetry metrics exporter started
```

**Verify metrics are being collected**:
```bash
$ curl http://localhost:55681/metrics  # Collector's own metrics
```

### JMX Connection Refused

**Firewall**:
```bash
$ sudo firewall-cmd --add-port=9999/tcp --permanent
$ sudo firewall-cmd --reload
```

**Authentication** (if enabled):
```bash
$ jconsole localhost:9999
# Enter username/password from jmxremote.password file
```

### Prometheus Scrape Failures

**Check endpoint**:
```bash
$ curl http://localhost:9090/metrics
# Should return Prometheus text format
```

**Check scrape config**:
```yaml
scrape_configs:
  - job_name: 'platform-java'
    static_configs:
      - targets: ['localhost:9090']  # Correct host:port
```

**Check Prometheus targets**:
- Open Prometheus UI: http://localhost:9090/targets
- Verify "platform-java" job is UP

## Complete Observability Stack

### Docker Compose

```yaml
version: '3'
services:
  platform-java:
    build: .
    ports:
      - "8080:8080"   # REST API
      - "9090:9090"   # Prometheus metrics
      - "9999:9999"   # JMX
    environment:
      OTEL_EXPORTER_OTLP_ENDPOINT: http://otel-collector:4317
  
  otel-collector:
    image: otel/opentelemetry-collector:latest
    volumes:
      - ./otel-config.yaml:/etc/otel-collector-config.yaml
    ports:
      - "4317:4317"
  
  prometheus:
    image: prom/prometheus:latest
    volumes:
      - ./prometheus.yml:/etc/prometheus/prometheus.yml
    ports:
      - "9091:9090"
  
  grafana:
    image: grafana/grafana:latest
    ports:
      - "3000:3000"
    environment:
      GF_SECURITY_ADMIN_PASSWORD: admin
  
  jaeger:
    image: jaegertracing/all-in-one:latest
    ports:
      - "16686:16686"  # Jaeger UI
      - "14250:14250"  # Collector
```

### Access URLs

- platform-java REST API: http://localhost:8080
- Prometheus Metrics: http://localhost:9090/metrics
- Prometheus UI: http://localhost:9091
- Grafana: http://localhost:3000
- Jaeger UI: http://localhost:16686

## See Also

- [Resource Enforcement](RESOURCE_ENFORCEMENT.md)
- [REST API Reference](REST_API.md)
- [Application Lifecycle](LIFECYCLE.md)
- [OpenTelemetry Documentation](https://opentelemetry.io/docs/)
- [Prometheus Documentation](https://prometheus.io/docs/)
