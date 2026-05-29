# platform-java Quick Reference

## Commands

### Deployment
```bash
platform-java deploy <app.yaml>          # Deploy application
platform-java deploy <app.yaml> --dry-run # Validate without deploying
```

### Lifecycle
```bash
platform-java start <app-id>             # Start application
platform-java stop <app-id>              # Stop application
platform-java restart <app-id>           # Restart application
platform-java start-all                  # Start all deployed apps
platform-java stop-all                   # Stop all running apps
```

### Status & Monitoring
```bash
platform-java status                     # List all applications
platform-java status <app-id>            # Detailed status
platform-java metrics <app-id>           # Resource usage
platform-java logs <app-id>              # View logs
platform-java logs <app-id> --follow     # Stream logs
```

### Management
```bash
platform-java undeploy <app-id>          # Remove application
platform-java reload <app-id>            # Hot reload (Java apps)
platform-java dependencies <app-id>      # Show dependencies
platform-java startup-order              # Show startup sequence
```

## YAML Descriptor Templates

### Virtual Machine
```yaml
applicationId: my-vm
name: My Virtual Machine
properties:
  vm.vcpu: "4"
  vm.memory: "8192"
  vm.disk: "/var/lib/platform-java/vms/my-vm.qcow2"
  vm.network: "bridge"
  vm.vnc.enabled: "true"
resources:
  cpu: 4
  memory: 8192
dependencies:
  - other-vm
```

### Container
```yaml
applicationId: my-container
name: My Container
properties:
  container.image: "nginx:alpine"
  container.runtime: "docker"
  container.ports: "8080:80"
  container.volumes: "/host/path:/container/path:ro"
resources:
  cpu: 2
  memory: 2048
dependencies:
  - database-vm
```

### Java Application
```yaml
applicationId: my-app
name: My Java Application
mainClass: "com.example.Main"
classpath:
  - "/var/lib/platform-java/apps/my-app/app.jar"
properties:
  server.port: "8080"
resources:
  cpu: 4
  memory: 8192
  maxThreads: 200
dependencies:
  - postgres-db
```

### Native Binary
```yaml
applicationId: my-binary
name: My Native Binary
nativeImage: true
properties:
  native.binary: "/var/lib/platform-java/apps/my-binary/app"
  native.args: "--config /etc/app.conf"
  native.workdir: "/var/lib/platform-java/apps/my-binary"
resources:
  cpu: 2
  memory: 2048
```

## Resource Quotas

```yaml
resources:
  cpu: 8              # Max CPUs/vCPUs
  memory: 32768       # Max RAM in MB
  disk: 524288        # Max disk in MB (500GB)
  maxThreads: 200     # Max threads (Java only)
```

## Dependency Patterns

### Simple Chain
```yaml
# A depends on nothing
applicationId: A
dependencies: []

# B depends on A
applicationId: B
dependencies:
  - A

# C depends on B
applicationId: C
dependencies:
  - B
```

### Multiple Dependencies
```yaml
# App depends on VM, container, and Java app
applicationId: my-app
dependencies:
  - postgres-vm      # VM
  - redis-container  # Container
  - auth-service     # Java app
```

### Cross-Tier Dependencies
```yaml
# Web tier depends on app tier
applicationId: nginx-web
dependencies:
  - spring-app

# App tier depends on data tier
applicationId: spring-app
dependencies:
  - postgres-db
```

## Health Checks

```yaml
healthCheck:
  enabled: true
  interval: 30      # Seconds between checks
  timeout: 5        # Seconds before timeout
  retries: 3        # Failed attempts before marking unhealthy
```

## Monitoring Configuration

```yaml
monitoring:
  enabled: true
  prometheus: true
  interval: 30
```

## VM-Specific Properties

```yaml
properties:
  vm.name: "my-vm-name"
  vm.vcpu: "8"
  vm.memory: "32768"
  vm.disk: "/path/to/disk.qcow2"
  vm.disk.format: "qcow2"
  vm.network: "bridge"  # or "nat", "none"
  vm.bridge: "virbr0"
  vm.vnc.enabled: "true"
  vm.vnc.port: "5900"
```

## Container-Specific Properties

```yaml
properties:
  container.image: "nginx:alpine"
  container.runtime: "docker"  # or "podman", "lxc"
  container.ports: "8080:80,8443:443"
  container.volumes: "/host:/container:ro,/data:/var/data"
  container.env: "VAR1=value1,VAR2=value2"
  container.network: "bridge"  # or "host", "none"
```

## Java App Properties

```yaml
properties:
  # Application
  server.port: "8080"
  server.address: "0.0.0.0"
  
  # Database
  spring.datasource.url: "jdbc:postgresql://localhost:5432/db"
  spring.datasource.username: "user"
  spring.datasource.password: "${DB_PASSWORD}"
  
  # platform-java
  platform-java.messaging.enabled: "true"
  platform-java.service.register: "true"
  platform-java.service.name: "my-service"
```

## Common Workflows

### Deploy Three-Tier App
```bash
# Deploy database (VM)
platform-java deploy database.yaml

# Deploy app server (Java)
platform-java deploy app-server.yaml

# Deploy web server (Container)
platform-java deploy web-server.yaml

# Start all (platform-java handles order)
platform-java start database
platform-java start app-server  # Waits for database
platform-java start web-server  # Waits for app-server
```

### Update Running App
```bash
# For Java apps - hot reload
mvn clean package
cp target/app.jar /var/lib/platform-java/apps/my-app/
platform-java reload my-app

# For VMs/containers - stop, redeploy, start
platform-java stop my-app
platform-java undeploy my-app
platform-java deploy my-app-v2.yaml
platform-java start my-app
```

### Debug Application
```bash
# Check status
platform-java status my-app

# View logs
platform-java logs my-app --follow

# Check resource usage
platform-java metrics my-app

# Check dependencies
platform-java dependencies my-app

# For VMs - connect to console
virsh vncdisplay my-vm-name
vncviewer localhost:5900
```

### Cleanup
```bash
# Stop application
platform-java stop my-app

# Remove from platform
platform-java undeploy my-app

# Clean up data (manual)
sudo rm -rf /var/lib/platform-java/apps/my-app
sudo rm -f /var/lib/platform-java/vms/my-vm.qcow2
```

## Environment Variables

```bash
export JPLATFORM_HOME=/usr/local/lib/platform-java
export JPLATFORM_DATA=/var/lib/platform-java
export JPLATFORM_LOG_LEVEL=DEBUG
```

## Prometheus Metrics

Access metrics:
```bash
curl http://localhost:9090/metrics
```

Key metrics:
```
platform-java_vm_cpu_time_seconds{vm="my-vm"}
platform-java_vm_memory_mb{vm="my-vm"}
platform-java_container_cpu_usage{container="my-container"}
platform-java_app_heap_mb{app="my-app"}
platform-java_app_thread_count{app="my-app"}
```

## Common Ports

- `8080` - Default REST API
- `9090` - Prometheus metrics
- `5900-5999` - VNC consoles (VMs)
- `8081-8099` - Application ports

## File Locations

```
/usr/local/lib/platform-java/          # platform-java binaries
/var/lib/platform-java/apps/           # Application data
/var/lib/platform-java/vms/            # VM disk images
/var/lib/platform-java/volumes/        # Persistent volumes
/etc/platform-java/                    # Configuration
/var/log/platform-java/                # Logs (if configured)
```

## Troubleshooting Quick Checks

```bash
# Platform health
platform-java status
systemctl status platform-java  # If running as service

# VM issues
systemctl status libvirtd
virsh list --all
lsmod | grep kvm

# Container issues
docker ps -a  # or podman ps -a
systemctl status docker

# Resource issues
free -h
df -h
nproc
```

## Getting Help

```bash
platform-java help
platform-java help deploy
platform-java version
```

Documentation:
- [Full Documentation](../README.md)
- [Architecture](ARCHITECTURE.md)
- [Troubleshooting](TROUBLESHOOTING.md)
- [VM Management](../platform-java-vm-management/README.md)
- [Examples](../examples/multi-tier/)
