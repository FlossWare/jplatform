# platform-java vs JEE Application Servers: A Detailed Comparison

## Executive Summary

platform-java and JEE Application Servers both provide platforms for running multiple Java applications with isolation and management capabilities, but they target different use cases and architectural philosophies.

**platform-java** is a lightweight, modern platform for running **any Java application** (batch jobs, message consumers, REST services, etc.) with strong isolation in a single JVM.

**JEE Application Servers** (WildFly, WebLogic, WebSphere, Payara) are heavyweight, standards-based platforms primarily for running **JEE web applications** (EARs/WARs) with full enterprise stack support.

---

## Quick Comparison Table

| Feature | platform-java 1.0 | JEE Application Servers |
|---------|---------------|-------------------------|
| **Primary Use Case** | Any Java application | JEE web applications (EARs/WARs) |
| **Application Type** | Any class with main() or Application interface | Servlets, EJBs, JSPs, REST services |
| **Deployment Unit** | JAR + YAML descriptor | WAR, EAR, RAR files |
| **Footprint** | ~50MB base + apps | 200MB - 2GB+ |
| **Startup Time** | < 2 seconds | 10-60+ seconds |
| **Standards** | None (custom APIs) | JEE/Jakarta EE specifications |
| **Complexity** | Low | High |
| **Vendor Lock-in** | None | Moderate to High |
| **Learning Curve** | Minimal | Steep |
| **Target Applications** | Microservices, batch, workers, any Java | Enterprise web apps, SOA, monoliths |

---

## Detailed Comparison

### 1. Application Model

#### platform-java
- **Any Java Application**: Deploy any JAR with a `main()` method or `Application` interface
- **Isolation**: ClassLoader, thread pool, security per application
- **Flexibility**: Run batch jobs, message consumers, REST APIs, scheduled tasks, etc.
- **Example Applications**:
  - Kafka consumer with custom processing logic
  - Scheduled batch job for data processing
  - gRPC microservice
  - WebSocket server
  - Any standalone Java application

**Deployment Example**:
```yaml
applicationId: kafka-consumer
mainClass: com.example.KafkaConsumerApp
classpathEntries:
  - file:///path/to/app.jar
  - file:///path/to/kafka-clients.jar
threadPool:
  corePoolSize: 4
  maxPoolSize: 20
```

#### JEE Application Servers
- **JEE Components Only**: Servlets, EJBs, JSPs, JMS, JAX-RS, JAX-WS
- **Container-Managed**: Applications must conform to JEE specifications
- **Web-Centric**: Primarily designed for HTTP-based applications
- **Example Applications**:
  - WAR file with Servlets and JSPs
  - EAR file with EJBs and web tier
  - JAX-RS REST API deployed as WAR
  - JMS message-driven beans

**Deployment Example**:
```xml
<!-- JEE deployment descriptor (web.xml) -->
<web-app>
  <servlet>
    <servlet-name>MyServlet</servlet-name>
    <servlet-class>com.example.MyServlet</servlet-class>
  </servlet>
  <servlet-mapping>
    <servlet-name>MyServlet</servlet-name>
    <url-pattern>/api/*</url-pattern>
  </servlet-mapping>
</web-app>
```

---

### 2. Deployment Methods

#### platform-java
- **6 Deployment Methods**:
  1. Interactive CLI (`platform-java> deploy`)
  2. YAML descriptor files (`deploy-yaml app.yaml`)
  3. JSON descriptor files (`deploy-json app.json`)
  4. REST API (`POST /api/applications`)
  5. Web console (browser-based)
  6. Filesystem watcher (drop YAML files in directory)

- **Deployment Time**: < 500ms per application
- **Hot Deployment**: Yes (all methods support it)
- **Declarative Configuration**: YAML/JSON descriptors with full validation

#### JEE Application Servers
- **Traditional Methods**:
  1. Admin console (web-based)
  2. Filesystem deployment directory
  3. CLI tools (jboss-cli, weblogic.Deployer)
  4. Maven plugins
  5. Vendor-specific REST APIs (varies)

- **Deployment Time**: 5-30 seconds per application (varies by size)
- **Hot Deployment**: Yes (most servers support it)
- **Declarative Configuration**: XML descriptors (web.xml, ejb-jar.xml, application.xml)

---

### 3. Isolation

#### platform-java
- **ClassLoader Isolation**: Parent-last delegation per application
  - Each app has isolated ClassLoader
  - Prevents version conflicts (e.g., app1 uses Jackson 2.10, app2 uses 2.15)
  - Platform APIs shared via parent ClassLoader

- **Thread Pool Isolation**: Managed ThreadPoolExecutor per application
  - Configurable core/max pool sizes
  - Named threads tagged with application ID
  - Resource monitoring per thread pool

- **Security Isolation**: Configurable permissions per application
  - File permissions
  - Socket permissions
  - Reflection control
  - Runtime permissions

- **Resource Monitoring**: Real-time CPU, memory, thread tracking per application

#### JEE Application Servers
- **ClassLoader Isolation**: Hierarchical ClassLoader per application
  - Similar parent-last delegation (varies by vendor)
  - EAR-level and WAR-level isolation
  - Shared libraries via server classpath

- **Thread Pool Isolation**: **None** - shared thread pool for all applications
  - Global thread pool configuration
  - No per-application limits
  - Monitoring available but not per-application

- **Security Isolation**: Role-based security (JAAS)
  - Container-managed security
  - Declarative security constraints
  - Security domains/realms

- **Resource Monitoring**: **Limited** - server-wide metrics, not per-application

**Key Difference**: platform-java provides **true per-application resource isolation and monitoring**, while JEE servers share thread pools and provide only ClassLoader isolation.

---

### 4. Inter-Application Communication

#### platform-java
- **Optional Message Bus**: Publish/subscribe event system
  - Applications opt-in to messaging
  - Topic-based routing
  - Asynchronous delivery
  - In-memory (no broker required)

- **Optional Service Registry**: Service lookup between applications
  - Register services by interface
  - Lookup services from other apps
  - Type-safe service discovery

- **Completely Optional**: Applications can be totally oblivious to messaging/registry

```java
// Publisher
context.getMessageBus().ifPresent(bus -> {
    bus.publish("events", Message.builder()
        .topic("events")
        .payload("Hello".getBytes())
        .build());
});

// Subscriber
context.getMessageBus().ifPresent(bus -> {
    bus.subscribe("events", message -> {
        System.out.println(new String(message.getPayload()));
    });
});
```

#### JEE Application Servers
- **JMS (Java Message Service)**: Enterprise messaging standard
  - Queue and topic semantics
  - Requires JMS broker (ActiveMQ, HornetQ, etc.)
  - Transaction support
  - Persistent/non-persistent messages

- **CDI Events**: Intra-application events (not inter-application)
  - Type-safe event system
  - Observer pattern
  - Only within same application context

- **JNDI Lookup**: Shared object registry
  - Global naming service
  - EJB remote lookup
  - DataSource sharing

**Key Difference**: platform-java's messaging is **in-memory and optional**, while JEE requires external JMS brokers and JNDI infrastructure.

---

### 5. Monitoring & Management

#### platform-java
- **Built-in Monitoring**:
  - JMX metrics exporter (port 9999)
  - Prometheus metrics exporter (port 9090)
  - REST API for metrics (`GET /api/applications/{id}/status`)
  - Web console with real-time charts

- **Per-Application Metrics**:
  - CPU time (nanoseconds)
  - Heap usage (estimated or JVMTI-precise)
  - Thread count
  - Active threads
  - Queued tasks
  - Completed tasks

- **Metrics Format**:
  ```prometheus
  platform-java_app_cpu_time_seconds{app_id="my-app"} 123.45
  platform-java_app_heap_used_bytes{app_id="my-app"} 134217728
  platform-java_app_thread_count{app_id="my-app"} 12
  ```

- **Web Console**: Modern browser UI with Chart.js visualization

#### JEE Application Servers
- **Built-in Monitoring**:
  - JMX MBeans (server-wide)
  - Vendor-specific admin consoles
  - CLI management tools
  - Optional Prometheus exporters (third-party)

- **Server-Wide Metrics**:
  - JVM heap, threads, CPU
  - Deployed applications count
  - HTTP connection pools
  - Transaction statistics
  - EJB invocation counts

- **Per-Application Metrics**: **Limited or Vendor-Specific**
  - Request counts (per WAR)
  - Session counts (per WAR)
  - No fine-grained CPU/memory per application

**Key Difference**: platform-java provides **granular per-application metrics** out-of-the-box, while JEE servers focus on server-wide monitoring.

---

### 6. Clustering & High Availability

#### platform-java
- **Clustering Support** (platform-java-cluster module):
  - Hazelcast-based distributed state
  - Leader election via CP subsystem
  - Automatic application rescheduling on node failure
  - Shared state across cluster nodes
  - Configurable via YAML

```yaml
cluster:
  enabled: true
  clusterName: platform-java-production
  bindAddress: 192.168.1.10
  bindPort: 5701
  seedNodes:
    - 192.168.1.10:5701
    - 192.168.1.11:5701
```

- **Failover**: Leader assigns applications to nodes; on failure, apps redeploy on surviving nodes

#### JEE Application Servers
- **Enterprise Clustering**:
  - Session replication
  - EJB clustering and failover
  - Load balancing (HTTP/EJB)
  - Distributed caches (Infinispan, Hazelcast)
  - Multi-node deployment

- **Vendor-Specific**: Clustering configuration varies widely (WildFly vs WebLogic vs WebSphere)

- **Complexity**: Requires additional infrastructure (load balancers, shared storage, database)

**Key Difference**: platform-java clustering is **application-level** (redeploy apps on failure), while JEE clustering is **session/state-level** (preserve user sessions across nodes).

---

### 7. Configuration & Management

#### platform-java
- **Configuration File** (`platform.yaml`):
  ```yaml
  api:
    enabled: true
    port: 8080
  metrics:
    jmx:
      enabled: true
      port: 9999
    prometheus:
      enabled: true
      port: 9090
  watcher:
    enabled: true
    watchDirectory: /var/platform-java/apps
  ```

- **Command-Line Overrides**:
  ```bash
  java -jar platform-java-launcher-1.0.jar --config platform.yaml --port 9000
  ```

- **Application Descriptors**: YAML or JSON
- **Management**: REST API, Web console, CLI

#### JEE Application Servers
- **Configuration Files**: XML-based (domain.xml, server.xml, standalone.xml)
  - Verbose XML syntax
  - Server-specific structure
  - Hundreds of configuration options

- **Management**:
  - Vendor admin consoles (web-based)
  - CLI tools (jboss-cli, asadmin, wlst)
  - JMX
  - REST APIs (vendor-specific)

**Key Difference**: platform-java uses **simple YAML** with command-line overrides, while JEE uses **complex XML** with vendor-specific tools.

---

### 8. Standards & Portability

#### platform-java
- **No Standards**: Custom APIs and interfaces
- **Vendor Lock-in**: None (open-source, no vendor)
- **Portability**: Applications are standard Java JARs
  - Can run standalone outside platform-java
  - Can migrate to containers (Docker) or Kubernetes
- **API Stability**: APIs defined in `platform-java-api` module

#### JEE Application Servers
- **JEE/Jakarta EE Standards**: Servlet, EJB, JPA, JMS, JAX-RS, JAX-WS, CDI, JSF
- **Vendor Lock-in**: Moderate
  - Vendor-specific extensions and features
  - Migration between servers can be complex
  - Application portability depends on standard compliance
- **Portability**: Applications should be portable across compliant servers
  - Reality: vendor-specific quirks and extensions
- **API Stability**: Governed by Jakarta EE specifications

**Key Difference**: JEE provides **industry standards** but with vendor lock-in, while platform-java is **non-standard but simpler** with no vendor dependency.

---

### 9. Performance & Resource Usage

#### platform-java
- **Lightweight**:
  - Base platform: ~50MB
  - Startup time: < 2 seconds
  - Memory overhead: ~50MB + per-app usage

- **Per-Application Overhead**: Minimal
  - ClassLoader: ~1MB
  - ThreadPoolExecutor: ~500KB
  - Monitoring: ~100KB

- **Isolation Overhead**: < 5% CPU overhead per app

- **Scalability**: Hundreds of applications per JVM (tested up to 100+)

#### JEE Application Servers
- **Heavyweight**:
  - Base server: 200MB - 2GB+
  - Startup time: 10-60+ seconds
  - Memory overhead: 500MB - 2GB+ (varies widely)

- **Per-Application Overhead**: Moderate to High
  - Web container: ~10-50MB per WAR
  - EJB container: ~20-100MB per EAR
  - Connection pools, caches, etc.

- **Scalability**: Dozens of applications per server (typically 10-50)

**Key Difference**: platform-java is **10x lighter** and **20x faster** to start than JEE servers.

---

### 10. Use Cases

#### platform-java - Best For:
1. **Microservices**: Run multiple isolated microservices in one JVM
2. **Batch Processing**: Deploy scheduled jobs with resource limits
3. **Message Consumers**: Kafka consumers, RabbitMQ workers
4. **Mixed Workloads**: Web services + batch jobs + message consumers in one platform
5. **Development**: Rapid prototyping with hot deployment
6. **Edge Computing**: Lightweight platform for resource-constrained environments
7. **Multi-Tenant SaaS**: Isolated applications per tenant
8. **Greenfield Projects**: Modern Java applications without JEE baggage

**Example**: Run 20 Kafka consumers processing different topics, each with isolated thread pools and memory limits, monitored via Prometheus.

#### JEE Application Servers - Best For:
1. **Enterprise Web Applications**: Traditional servlet/JSP applications
2. **SOA Architectures**: SOAP web services, EJBs
3. **Monolithic Applications**: Large EARs with multiple WARs and EJBs
4. **Standards Compliance**: Organizations requiring JEE certification
5. **Existing JEE Applications**: Migration from older servers
6. **Vendor Support**: Organizations requiring commercial support (Oracle, IBM, Red Hat)
7. **Full Stack**: Need complete JEE stack (JPA, JMS, JTA, etc.)
8. **Legacy Modernization**: Gradual migration of old applications

**Example**: Run a large e-commerce monolith with Servlets, EJBs, JPA, JMS, and JSF UI, with session clustering and transaction management.

---

### 11. Packaging & Distribution

#### platform-java
- **Single JAR**: `platform-java-launcher-1.0.jar` (~10MB)
- **No Installation**: Just run `java -jar`
- **Dependencies**: Bundled (no external dependencies)
- **Configuration**: Optional `platform.yaml` file
- **Deployment**: Drop YAML descriptors or use REST API

```bash
# Download and run
wget https://github.com/FlossWare/platform-java/releases/download/v1.0/platform-java-launcher-1.0.jar
java -jar platform-java-launcher-1.0.jar
```

#### JEE Application Servers
- **Full Distribution**: 200MB - 2GB download
- **Installation**: Extract, configure, run
- **Dependencies**: Many (vendor-specific)
- **Configuration**: Complex XML files, domain setup
- **Deployment**: Copy WARs/EARs or use admin console

```bash
# WildFly example
wget https://download.jboss.org/wildfly/27.0.0.Final/wildfly-27.0.0.Final.zip
unzip wildfly-27.0.0.Final.zip
cd wildfly-27.0.0.Final/bin
./standalone.sh
# Then deploy via admin console at http://localhost:9990
```

**Key Difference**: platform-java is **single JAR**, JEE servers require **full installation**.

---

### 12. Learning Curve

#### platform-java
- **Simple API**: ~20 interfaces in `platform-java-api`
- **Optional Features**: Only learn what you use
- **No Framework Lock-in**: Applications are plain Java
- **Documentation**: README, quickstart, examples
- **Time to First App**: 5-10 minutes

```java
// Minimal platform-java application
public class MyApp implements Application {
    @Override
    public void start(ApplicationContext context) {
        System.out.println("Started: " + context.getApplicationId());
    }

    @Override
    public void stop() {
        System.out.println("Stopped");
    }
}
```

#### JEE Application Servers
- **Complex Specifications**: 20+ JEE specifications to learn
- **Mandatory Framework**: Applications must use JEE APIs
- **Deep Integration**: Servlets, EJBs, JPA, CDI, JSF, etc.
- **Documentation**: Thousands of pages of specs
- **Time to First App**: Days to weeks (full understanding)

```java
// Minimal JEE servlet
@WebServlet("/hello")
public class HelloServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        resp.getWriter().write("Hello");
    }
}
```

**Key Difference**: platform-java has a **learning curve of hours**, JEE has a **learning curve of weeks/months**.

---

### 13. Testing

#### platform-java
- **Testability**: Excellent
  - Applications are POJOs (Plain Old Java Objects)
  - No container required for unit tests
  - Mock `ApplicationContext` easily
  - Integration tests use real platform

```java
// Unit test
@Test
void testApp() {
    ApplicationContext mockContext = mock(ApplicationContext.class);
    when(mockContext.getApplicationId()).thenReturn("test-app");

    MyApp app = new MyApp();
    app.start(mockContext);

    verify(mockContext).getApplicationId();
}
```

#### JEE Application Servers
- **Testability**: Challenging
  - Container-dependent code
  - Arquillian required for integration tests
  - Mocking EJB containers complex
  - Embedded containers (Weld, OpenEJB) for testing

```java
// Arquillian integration test
@RunWith(Arquillian.class)
public class MyEjbTest {
    @Deployment
    public static Archive<?> createDeployment() {
        return ShrinkWrap.create(JavaArchive.class)
            .addClasses(MyEjb.class)
            .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    @Inject
    MyEjb ejb;

    @Test
    public void test() {
        ejb.doSomething();
    }
}
```

**Key Difference**: platform-java applications are **easily testable POJOs**, JEE requires **Arquillian or embedded containers**.

---

### 14. Ecosystem & Community

#### platform-java
- **Maturity**: New (1.0 release)
- **Community**: Small (open-source project)
- **Third-Party Tools**: Limited
- **Commercial Support**: None
- **Documentation**: Growing
- **Plugins**: Minimal

#### JEE Application Servers
- **Maturity**: 20+ years (since JEE 1.0 in 1999)
- **Community**: Large (millions of developers)
- **Third-Party Tools**: Extensive (IDEs, APM, monitoring)
- **Commercial Support**: Available (Red Hat, Oracle, IBM)
- **Documentation**: Extensive (books, courses, tutorials)
- **Plugins**: Hundreds (Maven, Gradle, CI/CD)

**Key Difference**: JEE has a **mature ecosystem**, platform-java is **new and growing**.

---

### 15. Cost

#### platform-java
- **License**: Open-source (free)
- **Support**: Community (GitHub issues)
- **Training**: Self-service (documentation)
- **Infrastructure**: Minimal (runs on any JVM)
- **Total Cost**: **Near zero**

#### JEE Application Servers
- **License**:
  - Open-source: WildFly, Payara, TomEE (free)
  - Commercial: WebLogic, WebSphere ($$$$)

- **Support**:
  - Community (open-source)
  - Enterprise support (paid)

- **Training**: Expensive (courses, certifications)
- **Infrastructure**: Higher (more memory, CPU)
- **Total Cost**: **Low (open-source) to Very High (commercial)**

**Key Difference**: platform-java is **always free**, JEE can be **very expensive** (commercial servers + support).

---

## When to Choose platform-java

Choose **platform-java** if you:
- Want to run **any Java application** (not just web apps)
- Need **strong per-application isolation**
- Require **fine-grained resource monitoring**
- Value **simplicity over standards**
- Are building **microservices or modern architectures**
- Need **fast startup and low overhead**
- Want **easy deployment and management**
- Prefer **YAML over XML**
- Don't need full JEE stack
- Are starting a **greenfield project**

---

## When to Choose JEE Application Servers

Choose **JEE/Jakarta EE servers** if you:
- Are building **traditional enterprise web applications**
- Need **full JEE stack** (EJB, JPA, JMS, JTA, JSF)
- Require **industry standards compliance**
- Have **existing JEE applications** to maintain
- Need **commercial vendor support**
- Value **ecosystem maturity and tooling**
- Require **session clustering and state replication**
- Have **enterprise procurement processes** (prefer established vendors)
- Need **SOA/SOAP web services**
- Are **migrating legacy applications**

---

## Migration Path

### From JEE to platform-java

**Good Candidates**:
- Standalone JAR applications
- Batch jobs currently deployed as WARs
- Message consumers packaged as EJBs
- REST services without JEE dependencies

**Migration Steps**:
1. Extract business logic from JEE components
2. Create standalone Java application
3. Package as JAR
4. Create platform-java descriptor (YAML)
5. Deploy to platform-java

**Challenges**:
- Replace JEE APIs with plain Java (e.g., @EJB → constructor injection)
- Remove container-managed transactions (use programmatic or Spring)
- Replace JMS with platform-java messaging or external broker

### From platform-java to JEE

**Good Candidates**:
- Applications that outgrow simple isolation needs
- Applications requiring full JEE stack
- Applications needing vendor support

**Migration Steps**:
1. Package as WAR/EAR
2. Add JEE deployment descriptors (web.xml)
3. Replace platform-java APIs with JEE equivalents
4. Deploy to JEE server

**Challenges**:
- Rewrite to use JEE programming model
- Add container-managed configuration
- Lose fine-grained per-application monitoring

---

## Conclusion

**platform-java** and **JEE Application Servers** serve different purposes:

- **platform-java**: Modern, lightweight platform for running **any Java application** with strong isolation and simple management. Best for **microservices, batch jobs, and greenfield projects**.

- **JEE Servers**: Mature, standards-based platforms for **enterprise web applications** with full JEE stack. Best for **traditional enterprise apps and organizations requiring standards compliance**.

**The choice depends on your use case:**
- Building a new microservice? → **platform-java**
- Maintaining a legacy EJB application? → **JEE Server**
- Need lightweight platform for Kafka consumers? → **platform-java**
- Need full JEE stack with JPA, JMS, JTA? → **JEE Server**
- Want fast startup and low overhead? → **platform-java**
- Need vendor support and enterprise tooling? → **JEE Server**

Both platforms have their place in the Java ecosystem. platform-java fills the gap for **modern, isolated Java applications** that don't need the full JEE stack.

---

## Further Reading

- **platform-java Documentation**:
  - [README.md](README.md) - Architecture and features
  - [QUICKSTART.md](QUICKSTART.md) - 5-minute tutorial
  - [RELEASE_NOTES.md](RELEASE_NOTES.md) - Version 1.0 features

- **Jakarta EE Resources**:
  - [Jakarta EE Specification](https://jakarta.ee/)
  - [WildFly Documentation](https://www.wildfly.org/)
  - [Payara Documentation](https://www.payara.fish/)

---

**Version**: platform-java 1.0 vs JEE/Jakarta EE 10  
**Last Updated**: May 2026
