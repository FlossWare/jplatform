# Three-Tier Web Application Example

Complete three-tier web application demonstrating JPlatform's unified orchestration across VMs, Java applications, and containers.

## Architecture

```
        ┌─────────────────────────┐
        │  Users / Browsers       │
        └────────────┬────────────┘
                     │ HTTP :8080
        ┌────────────▼────────────┐
        │    NGINX (Container)     │  ← Web Tier
        │  - Reverse Proxy         │    Docker/Podman
        │  - Static Content        │    Resource: 2 CPU, 2GB RAM
        └────────────┬────────────┘
                     │ HTTP :8081
        ┌────────────▼────────────┐
        │  Spring Boot (Java)      │  ← Application Tier
        │  - REST API              │    In-JVM Isolation
        │  - Business Logic        │    Resource: 4 CPU, 8GB RAM
        └────────────┬────────────┘
                     │ JDBC :5432
        ┌────────────▼────────────┐
        │  PostgreSQL (VM)         │  ← Data Tier
        │  - Database              │    KVM/QEMU via libvirt
        │  - Persistent Storage    │    Resource: 8 CPU, 32GB RAM
        └─────────────────────────┘
```

## Components

### 1. Database Tier (PostgreSQL VM)
- **Type**: Virtual Machine (KVM/QEMU)
- **OS**: Ubuntu Server 22.04
- **Resources**: 8 vCPUs, 32GB RAM, 500GB disk
- **Port**: 5432
- **Dependencies**: None (foundation layer)

### 2. Application Tier (Spring Boot)
- **Type**: Java Application (in-JVM isolation)
- **Framework**: Spring Boot 3.2
- **Resources**: 4 CPUs, 8GB heap, 200 threads
- **Port**: 8081
- **Dependencies**: postgres-db
- **Features**:
  - REST API
  - Database connection pooling
  - Health checks
  - Prometheus metrics

### 3. Web Tier (NGINX)
- **Type**: Container (Docker/Podman)
- **Image**: nginx:alpine
- **Resources**: 2 CPUs, 2GB RAM, 10GB disk
- **Ports**: 8080 (HTTP), 8443 (HTTPS)
- **Dependencies**: spring-app
- **Features**:
  - Reverse proxy to app tier
  - Static content serving
  - Load balancing ready

## Prerequisites

### System Requirements
- Linux with KVM support
- Docker or Podman
- libvirt and QEMU
- Java 21+ (for Spring Boot)
- JPlatform 1.1+

### Verification
```bash
# Check KVM
lsmod | grep kvm

# Check libvirt
virsh list --all

# Check container runtime
docker --version || podman --version

# Check JPlatform
jplatform --version
```

## Quick Start

### 1. Prepare Infrastructure

```bash
# Create VM disk image
sudo mkdir -p /var/lib/jplatform/vms
sudo qemu-img create -f qcow2 /var/lib/jplatform/vms/postgres-db.qcow2 500G

# Or use Ubuntu cloud image
wget https://cloud-images.ubuntu.com/releases/22.04/release/ubuntu-22.04-server-cloudimg-amd64.img
sudo cp ubuntu-22.04-server-cloudimg-amd64.img /var/lib/jplatform/vms/postgres-db.qcow2
sudo qemu-img resize /var/lib/jplatform/vms/postgres-db.qcow2 500G

# Create application directories
sudo mkdir -p /var/lib/jplatform/apps/spring-app
sudo mkdir -p /var/lib/jplatform/apps/nginx-web/html

# Build Spring Boot app (if you have the source)
# cd your-spring-boot-app
# mvn clean package
# sudo cp target/app.jar /var/lib/jplatform/apps/spring-app/

# Create NGINX config
sudo tee /var/lib/jplatform/apps/nginx-web/nginx.conf > /dev/null << 'NGINX_CONF'
events {
    worker_connections 1024;
}
http {
    upstream app_server {
        server host.docker.internal:8081;
    }
    server {
        listen 80;
        location / {
            root /usr/share/nginx/html;
            index index.html;
        }
        location /api/ {
            proxy_pass http://app_server;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
        }
    }
}
NGINX_CONF

# Create static content
sudo tee /var/lib/jplatform/apps/nginx-web/html/index.html > /dev/null << 'HTML'
<!DOCTYPE html>
<html>
<head>
    <title>Three-Tier App</title>
</head>
<body>
    <h1>Three-Tier Web Application</h1>
    <p>Powered by JPlatform</p>
    <ul>
        <li>Database: PostgreSQL VM</li>
        <li>Application: Spring Boot Java</li>
        <li>Web: NGINX Container</li>
    </ul>
    <p><a href="/api/users">API Endpoint</a></p>
</body>
</html>
HTML
```

### 2. Deploy

```bash
# Option A: Use the deploy script
./deploy.sh

# Option B: Deploy manually
jplatform deploy 1-database-tier.yaml
jplatform deploy 2-app-tier.yaml
jplatform deploy 3-web-tier.yaml
```

### 3. Start

```bash
# Option A: Use the start script
./start.sh

# Option B: Start manually (JPlatform handles dependency order)
jplatform start postgres-db
jplatform start spring-app  # Waits for postgres-db
jplatform start nginx-web   # Waits for spring-app

# Check status
jplatform status
```

### 4. Test

```bash
# Run automated tests
./test.sh

# Manual tests
curl http://localhost:8080/                           # Static content
curl http://localhost:8080/api/users                  # API via proxy
curl http://localhost:8081/actuator/health            # App health
```

### 5. Monitor

```bash
# View metrics for each tier
jplatform metrics postgres-db
jplatform metrics spring-app
jplatform metrics nginx-web

# View logs
jplatform logs postgres-db
jplatform logs spring-app
jplatform logs nginx-web

# Prometheus metrics
curl http://localhost:9090/metrics
```

## Dependency Management

JPlatform automatically manages startup order based on dependencies:

```
postgres-db (VM)
    ↓ waits for
spring-app (Java)
    ↓ waits for
nginx-web (Container)
```

**Startup sequence:**
1. postgres-db starts first (no dependencies)
2. spring-app waits until postgres-db is RUNNING
3. nginx-web waits until spring-app is RUNNING

**Shutdown sequence** (reverse):
1. nginx-web stops first
2. spring-app stops second
3. postgres-db stops last

## Resource Allocation

| Tier | Type | CPU | Memory | Disk |
|------|------|-----|--------|------|
| Database | VM | 8 vCPUs | 32GB | 500GB |
| Application | Java | 4 CPUs | 8GB | - |
| Web | Container | 2 CPUs | 2GB | 10GB |
| **Total** | - | **14 CPUs** | **42GB** | **510GB** |

JPlatform enforces these limits and monitors usage in real-time.

## Troubleshooting

### Database VM won't start
```bash
# Check KVM is available
lsmod | grep kvm

# Check disk image exists
ls -lh /var/lib/jplatform/vms/postgres-db.qcow2

# View VM console
virsh vncdisplay postgres-prod
vncviewer localhost:5900

# Check libvirt logs
sudo virsh list --all
```

### Application tier can't connect to database
```bash
# Get VM IP address
virsh domifaddr postgres-prod

# Update 2-app-tier.yaml with correct IP
# spring.datasource.url: "jdbc:postgresql://[VM_IP]:5432/appdb"

# Test connectivity
nc -zv [VM_IP] 5432
```

### Web tier can't reach app tier
```bash
# Check app is listening
netstat -tlnp | grep 8081

# Test from container network
docker exec nginx-web curl http://host.docker.internal:8081/actuator/health
```

### View dependency graph
```bash
jplatform startup-order
jplatform dependencies spring-app
```

## Cleanup

```bash
# Option A: Use cleanup script
./cleanup.sh

# Option B: Manual cleanup
./stop.sh
jplatform undeploy nginx-web
jplatform undeploy spring-app
jplatform undeploy postgres-db

# Remove data (optional)
sudo rm -rf /var/lib/jplatform/vms/postgres-db.qcow2
sudo rm -rf /var/lib/jplatform/apps/spring-app
sudo rm -rf /var/lib/jplatform/apps/nginx-web
```

## Customization

### Adjust Resources

Edit the YAML files to change resource allocations:

```yaml
# 1-database-tier.yaml
resources:
  cpu: 16        # Increase to 16 vCPUs
  memory: 65536  # Increase to 64GB
```

### Add More Tiers

Add additional tiers by creating new YAML descriptors:

```yaml
# 4-cache-tier.yaml
applicationId: redis-cache
properties:
  container.image: "redis:alpine"
dependencies:
  - postgres-db
```

### Enable Features

```yaml
# 2-app-tier.yaml
properties:
  # Enable messaging
  jplatform.messaging.enabled: "true"
  
  # Register service
  jplatform.service.register: "true"
  jplatform.service.name: "app-server"
```

## Production Considerations

### High Availability
- Deploy multiple instances of each tier
- Use load balancer in front of web tier
- Configure database replication
- Enable health checks

### Security
- Use HTTPS (port 8443)
- Configure firewall rules
- Enable VNC authentication
- Use secrets management for passwords

### Monitoring
- Enable Prometheus metrics export
- Set up Grafana dashboards
- Configure alerting
- Log aggregation

### Backup
- VM snapshots for database
- Application code in version control
- Container images in registry
- Configuration backups

## Further Reading

- [JPlatform Documentation](../../../README.md)
- [VM Management Guide](../../../jplatform-vm-management/README.md)
- [Container Deployment Guide](../../../CONTAINER_DEPLOYMENT.md)
- [Resource Management](../../../RESOURCE_ENFORCEMENT.md)
- [Application Dependencies](../../../APPLICATION_DEPENDENCIES.md)

## Support

For issues or questions:
- [GitHub Issues](https://github.com/FlossWare/jplatform/issues)
- [Documentation](https://github.com/FlossWare/jplatform)
