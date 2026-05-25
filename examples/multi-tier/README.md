# Multi-Tier Application Examples

This directory contains complete multi-tier application examples showcasing JPlatform's unified orchestration across VMs, containers, Java applications, and native binaries.

## Examples

### 1. [Three-Tier Web Application](./three-tier-webapp/)
Classic web application with database VM, application server, and web frontend.

**Architecture:**
```
┌─────────────────┐
│  NGINX (Container)  │  ← Web Tier (Reverse Proxy)
└─────────────────┘
         ↓
┌─────────────────┐
│  Spring Boot (Java) │  ← Application Tier (Business Logic)
└─────────────────┘
         ↓
┌─────────────────┐
│  PostgreSQL (VM)    │  ← Data Tier (Database)
└─────────────────┘
```

**Workload Types:**
- Database: PostgreSQL VM (libvirt/KVM)
- App Server: Spring Boot Java app (in-JVM isolation)
- Web Server: NGINX container (Docker/Podman)

**Key Features:**
- Cross-workload dependencies
- Automatic startup ordering
- Unified monitoring
- Resource quotas per tier

### 2. [Microservices Architecture](./microservices/)
Modern microservices deployment with service mesh, API gateway, and multiple services.

**Architecture:**
```
┌─────────────┐
│  API Gateway  │  ← Entry Point (Container)
└─────────────┘
       ↓
  ┌────┴────┬────────────┬──────────┐
  ↓         ↓            ↓          ↓
┌──────┐ ┌──────┐  ┌──────┐  ┌──────┐
│Auth  │ │User  │  │Order │  │Product│
│(Java)│ │(Java)│  │(VM)  │  │(Cont.)│
└──────┘ └──────┘  └──────┘  └──────┘
       ↓         ↓            ↓
    ┌──────────────────────┐
    │  Redis (Container)    │  ← Cache Layer
    └──────────────────────┘
```

**Workload Types:**
- API Gateway: Kong container
- Auth Service: Java microservice
- User Service: Java microservice
- Order Service: Node.js VM
- Product Service: Python container
- Cache: Redis container

**Key Features:**
- Service discovery
- Inter-service dependencies
- Shared message bus
- Distributed tracing

### 3. [Data Processing Pipeline](./data-pipeline/)
ETL pipeline with data ingestion, processing, and analytics.

**Architecture:**
```
┌───────────┐
│  Ingestion │  ← Data Sources (Native Binary)
└───────────┘
      ↓
┌───────────┐
│  Processing │  ← Spark Job (VM)
└───────────┘
      ↓
┌───────────┐
│  Storage    │  ← TimescaleDB (Container)
└───────────┘
      ↓
┌───────────┐
│  Analytics  │  ← Java Analytics App
└───────────┘
```

**Workload Types:**
- Ingestion: Kafka native binary
- Processing: Apache Spark on VM
- Storage: TimescaleDB container
- Analytics: Java application

**Key Features:**
- Pipeline orchestration
- Data flow dependencies
- Resource-intensive workloads
- Scheduled execution

### 4. [Hybrid Cloud Application](./hybrid-cloud/)
Application spanning on-premises VirtOS and cloud providers.

**Architecture:**
```
On-Premises (VirtOS)          Cloud (AWS/Azure/GCP)
┌─────────────────┐          ┌─────────────────┐
│  Legacy DB (VM) │ ←──────→ │  API Service    │
└─────────────────┘          │  (Cloud Function)│
                              └─────────────────┘
┌─────────────────┐          ┌─────────────────┐
│  Web App (Java) │ ←──────→ │  S3 Storage     │
└─────────────────┘          └─────────────────┘
```

**Workload Types:**
- On-Premises: VirtOS running JPlatform
  * Legacy database: VM with existing Oracle/SQL Server
  * Web application: Java Spring Boot
- Cloud: Managed services
  * API endpoints
  * Object storage
  * Message queues

**Key Features:**
- Hybrid deployment
- VirtOS federation
- Secure connectivity
- Cost optimization

## Quick Start

### Deploy Example 1: Three-Tier Web App

```bash
cd three-tier-webapp

# Deploy all tiers (JPlatform handles dependency order)
jplatform deploy database-tier.yaml
jplatform deploy app-tier.yaml
jplatform deploy web-tier.yaml

# Start the stack (JPlatform starts in dependency order)
jplatform start postgres-db    # Database starts first
jplatform start spring-app     # App waits for database
jplatform start nginx-web      # Web waits for app

# Check status
jplatform status

# View metrics for all tiers
jplatform metrics postgres-db
jplatform metrics spring-app
jplatform metrics nginx-web

# Access the application
curl http://localhost:8080
```

### Deploy Example 2: Microservices

```bash
cd microservices

# Deploy all services at once
for yaml in *.yaml; do
  jplatform deploy $yaml
done

# Start all services (JPlatform handles dependencies)
jplatform start-all

# View service mesh
jplatform status

# Test API
curl http://localhost:8000/api/v1/products
```

## Example Structure

Each example includes:

### YAML Descriptors
- One descriptor per workload
- Clearly defined dependencies
- Resource quotas
- Health checks

### Documentation
- Architecture diagram
- Deployment instructions
- Testing procedures
- Troubleshooting guide

### Scripts
- `deploy.sh` - Deploy the entire stack
- `start.sh` - Start all workloads in order
- `stop.sh` - Stop all workloads
- `test.sh` - Run integration tests
- `cleanup.sh` - Remove all workloads

## Dependencies Between Workload Types

JPlatform allows dependencies across all workload types:

```yaml
# Example: Java app depends on VM database and container cache
applicationId: my-java-app
mainClass: com.example.MyApp
dependencies:
  - postgres-vm        # VM (KVM/QEMU via libvirt)
  - redis-container    # Container (Docker/Podman)
  - auth-service      # Another Java app
```

**Startup Order:**
1. postgres-vm (VM starts first)
2. redis-container (Container starts second)
3. auth-service (Java app starts third)
4. my-java-app (Starts last, after all dependencies ready)

## Resource Quotas

Each tier can have different resource limits:

```yaml
# Database VM - High resources
resources:
  cpu: 8
  memory: 32768  # 32GB

# App Server - Medium resources
resources:
  cpu: 4
  memory: 8192   # 8GB

# Web Server - Low resources
resources:
  cpu: 2
  memory: 2048   # 2GB
```

## Monitoring

All tiers export metrics to Prometheus:

```bash
# Aggregate metrics across all tiers
curl http://localhost:9090/metrics | grep jplatform

# Metrics include:
# - jplatform_vm_cpu_time_seconds{vm="postgres-db"}
# - jplatform_container_cpu_usage{container="nginx-web"}
# - jplatform_app_heap_mb{app="spring-app"}
```

## Cross-Workload Communication

### Via Message Bus
```yaml
# Enable messaging in all workloads
properties:
  jplatform.messaging.enabled: "true"

# Publish events from any workload type
# - VMs can publish to Java apps
# - Containers can subscribe to VM events
# - Java apps can coordinate with native binaries
```

### Via Service Registry
```yaml
# Register services from any workload
properties:
  jplatform.service.register: "true"
  jplatform.service.name: "user-service"
  jplatform.service.port: "8080"

# Discover services from any other workload
# - Java apps can find VMs
# - Containers can discover Java services
# - VMs can lookup other VMs
```

## Testing

Each example includes integration tests:

```bash
# Run tests for an example
cd three-tier-webapp
./test.sh

# Tests verify:
# - All workloads start successfully
# - Dependencies are respected
# - Cross-workload communication works
# - Health checks pass
# - Application functionality
```

## Troubleshooting

### Check Dependency Order
```bash
jplatform startup-order
```

### View Workload Logs
```bash
jplatform logs <workload-id>
```

### Check Resource Usage
```bash
jplatform metrics <workload-id>
```

### Verify Dependencies
```bash
jplatform dependencies <workload-id>
```

## Further Reading

- [JPlatform Documentation](../../README.md)
- [VM Management](../../jplatform-vm-management/README.md)
- [Container Deployment](../../CONTAINER_DEPLOYMENT.md)
- [Application Dependencies](../../APPLICATION_DEPENDENCIES.md)
- [Resource Enforcement](../../RESOURCE_ENFORCEMENT.md)

## Contributing

Have an interesting multi-tier example? Contributions welcome!

1. Create example directory
2. Add descriptors and documentation
3. Include deployment and test scripts
4. Submit pull request

Examples should demonstrate:
- Multiple workload types
- Real-world architecture
- Production-ready configuration
- Clear documentation
