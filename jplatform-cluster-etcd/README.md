# JPlatform Cluster - Etcd

Etcd-based clustering implementation for JPlatform using distributed etcd primitives for leader election and membership management.

## Features

- **Leader Election**: Etcd lease-based leader election with automatic expiration
- **Membership Tracking**: Etcd key-value store for tracking cluster nodes
- **State Storage**: Distributed state store using etcd KV with JSON serialization
- **Connection Management**: Reliable etcd client connections with configurable timeouts
- **Thread Safety**: All operations are thread-safe
- **Auto-renewal**: Periodic lease renewal to maintain leadership
- **Event Notifications**: ClusterEventListener support for state changes
- **Prefix Queries**: Efficient retrieval of all states using prefix scans

## Maven Dependency

```xml
<dependency>
    <groupId>org.flossware.jplatform</groupId>
    <artifactId>jplatform-cluster-etcd</artifactId>
    <version>1.1</version>
</dependency>
```

## Quick Start

### Basic Usage

```java
// Configure etcd connection
EtcdConfig config = EtcdConfig.builder()
    .addEndpoint("http://localhost:2379")
    .leaseTtl(10)
    .build();

// Create cluster manager
EtcdClusterManager clusterManager = new EtcdClusterManager(config);

// Join cluster
ClusterConfig clusterConfig = new ClusterConfig("my-cluster", "192.168.1.100", 8080);
clusterManager.join(clusterConfig);

// Check leadership
if (clusterManager.isLeader()) {
    System.out.println("I am the leader!");
}

// Get cluster nodes
Set<ClusterNode> nodes = clusterManager.getNodes();

// Leave cluster when done
clusterManager.leave();
```

### Multiple Endpoints

```java
EtcdConfig config = EtcdConfig.builder()
    .addEndpoint("http://etcd1.example.com:2379")
    .addEndpoint("http://etcd2.example.com:2379")
    .addEndpoint("http://etcd3.example.com:2379")
    .leaseTtl(15)
    .build();
```

### With Authentication

```java
EtcdConfig config = EtcdConfig.builder()
    .addEndpoint("https://etcd.example.com:2379")
    .username("admin")
    .password("secret")
    .namespace("jplatform")
    .leaseTtl(10)
    .build();
```

### State Store Usage

```java
// Create state store
Client etcdClient = Client.builder()
    .endpoints("http://localhost:2379")
    .build();
EtcdStateStore stateStore = new EtcdStateStore(etcdClient);

// Store application state
stateStore.putApplicationState("app1", ApplicationState.RUNNING);

// Retrieve application state
ApplicationState state = stateStore.getApplicationState("app1");

// Get all states
Map<String, ApplicationState> allStates = stateStore.getAllApplicationStates();

// Subscribe to state changes
stateStore.subscribe("app1", (id, newState) -> {
    System.out.println("State changed: " + id + " -> " + newState);
});
```

### Event Listeners

```java
clusterManager.addListener(new ClusterEventListener() {
    @Override
    public void onNodeJoined(ClusterNode node) {
        System.out.println("Node joined: " + node.getId());
    }

    @Override
    public void onNodeLeft(ClusterNode node) {
        System.out.println("Node left: " + node.getId());
    }

    @Override
    public void onLeaderChanged(ClusterNode newLeader) {
        System.out.println("New leader: " + newLeader.getId());
    }
});
```

## Configuration Options

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `endpoints` | List<String> | `["http://localhost:2379"]` | Etcd server endpoints |
| `leaseTtl` | long | `10` | Lease time-to-live in seconds (minimum 5) |
| `namespace` | String | `null` | Key namespace prefix |
| `username` | String | `null` | Authentication username |
| `password` | String | `null` | Authentication password |

## Architecture

### Leader Election

The etcd cluster manager uses etcd leases for leader election:

1. On join, creates a lease with configured TTL
2. Attempts to acquire leadership by writing to leader key with lease ID
3. If successful, becomes leader
4. Periodically renews lease to maintain leadership
5. If lease expires, leadership is automatically released

### State Storage

EtcdStateStore provides distributed state management:

- **Application States**: Stored as etcd keys under `/jplatform/states/{appId}`
- **Application Descriptors**: Stored as etcd keys under `/jplatform/descriptors/{appId}`
- **JSON Serialization**: Uses Jackson ObjectMapper for complex objects
- **Prefix Queries**: Efficient retrieval of all states using etcd prefix scans
- **Listeners**: ConcurrentHashMap-based listener management for state change notifications

### Thread Safety

All operations are thread-safe:
- Volatile flags for state management
- ConcurrentHashMap for listener storage
- etcd client handles concurrent requests

## Etcd Setup

### Docker

```bash
docker run -d \
  -p 2379:2379 \
  -p 2380:2380 \
  --name etcd \
  quay.io/coreos/etcd:latest \
  /usr/local/bin/etcd \
  --advertise-client-urls http://0.0.0.0:2379 \
  --listen-client-urls http://0.0.0.0:2379
```

### Kubernetes

```yaml
apiVersion: v1
kind: Service
metadata:
  name: etcd
spec:
  selector:
    app: etcd
  ports:
    - port: 2379
---
apiVersion: apps/v1
kind: StatefulSet
metadata:
  name: etcd
spec:
  serviceName: etcd
  replicas: 3
  selector:
    matchLabels:
      app: etcd
  template:
    metadata:
      labels:
        app: etcd
    spec:
      containers:
      - name: etcd
        image: quay.io/coreos/etcd:latest
        ports:
        - containerPort: 2379
```

## Comparison with Alternatives

| Feature | Etcd | Consul | Redis | ZooKeeper |
|---------|------|--------|-------|-----------|
| Leader Election | Lease-based | Session-based | SETNX-based | Ephemeral nodes |
| State Storage | KV store | KV store | Hashes | Znodes |
| Watch Support | Yes | Yes | Pub/Sub | Yes |
| Setup Complexity | Low | Medium | Very Low | Medium |
| Performance | High | High | Very High | High |
| Consistency | Strong | Strong | Eventual | Strong |

**Use Etcd when:**
- You need strong consistency guarantees
- You're using Kubernetes (etcd is native)
- You want simple setup with powerful features
- You need watch/notification capabilities

**Prefer alternatives when:**
- **Redis**: You need absolute maximum performance and can accept eventual consistency
- **Consul**: You need service mesh capabilities beyond clustering
- **ZooKeeper**: You have existing ZooKeeper infrastructure

## Dependencies

This module requires:

- `io.etcd:jetcd-core:0.7.5` - Etcd Java client
- `com.fasterxml.jackson.core:jackson-databind` - JSON serialization
- `org.slf4j:slf4j-api` - Logging facade

## Thread Safety

- All public methods are thread-safe
- Uses volatile boolean flags for state management
- CopyOnWriteArrayList for listener collections
- Etcd client handles concurrent requests internally

## Testing

Tests use mocked etcd client:

```bash
mvn test -pl jplatform-cluster-etcd
```

For integration tests with real etcd (optional):

```bash
# Start etcd
docker run -d -p 2379:2379 --name etcd-test quay.io/coreos/etcd:latest

# Run tests
mvn verify -pl jplatform-cluster-etcd

# Stop etcd
docker stop etcd-test && docker rm etcd-test
```

## License

Part of JPlatform - see main project for license details.
