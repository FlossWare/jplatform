# FlossWare Platform Bill of Materials (BOM)

## Overview

The `platform-bom` module provides centralized dependency management for all FlossWare Platform modules and their external dependencies. This ensures version consistency across the entire multi-module project.

## Purpose

### Problems Solved

1. **Version Consistency**: All modules use the same versions of dependencies
2. **Simplified Dependency Declaration**: Child modules don't need to specify versions
3. **Easier Upgrades**: Update one place to upgrade across all modules
4. **Downstream Project Support**: External projects can import our BOM to match our dependency versions

## Usage

### For Platform Modules (Internal)

In any platform module's `pom.xml`:

```xml
<dependencies>
    <!-- No version needed - inherited from BOM -->
    <dependency>
        <groupId>org.flossware.jplatform</groupId>
        <artifactId>platform-api</artifactId>
    </dependency>
    
    <dependency>
        <groupId>org.slf4j</groupId>
        <artifactId>slf4j-api</artifactId>
    </dependency>
</dependencies>
```

### For Downstream Projects (External)

Projects that depend on FlossWare Platform can import the BOM to ensure version compatibility:

```xml
<dependencyManagement>
    <dependencies>
        <!-- Import FlossWare Platform BOM -->
        <dependency>
            <groupId>org.flossware.jplatform</groupId>
            <artifactId>platform-bom</artifactId>
            <version>1.0</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>

<dependencies>
    <!-- Now use platform modules without specifying versions -->
    <dependency>
        <groupId>org.flossware.jplatform</groupId>
        <artifactId>platform-core</artifactId>
    </dependency>
</dependencies>
```

## What's Included

### Internal Modules (39 total)

**Core Platform:**
- platform-api
- platform-core
- platform-classloader
- platform-threadpool
- platform-security
- platform-monitoring

**Messaging:**
- platform-messaging
- platform-messaging-jms

**Configuration:**
- platform-config
- platform-fs-watcher
- platform-deployment
- platform-storage

**Management Interfaces:**
- platform-rest-api
- platform-rest-api-netty
- platform-web-console
- platform-swing-ui
- platform-terminal-ui

**Metrics & Observability:**
- platform-metrics-jmx
- platform-metrics-prometheus
- platform-otel

**Clustering (5 implementations):**
- platform-cluster (base)
- platform-cluster-consul
- platform-cluster-etcd
- platform-cluster-redis
- platform-cluster-zookeeper

**Service Registry (3 implementations):**
- platform-registry-consul
- platform-registry-etcd
- platform-registry-eureka

**Configuration Sources (4 implementations):**
- platform-config-consul
- platform-config-etcd
- platform-config-vault
- platform-config-zookeeper

**Storage Backends (3 implementations):**
- platform-storage-s3
- platform-storage-database
- platform-storage-redis

**Advanced:**
- platform-vm-management
- platform-jvmti-agent
- platform-launcher
- platform-samples

### External Dependencies

**Logging:**
- SLF4J ${slf4j.version}
- Logback ${logback.version}

**Data Processing:**
- Jackson ${jackson.version}
- SnakeYAML ${snakeyaml.version}

**Observability:**
- OpenTelemetry ${opentelemetry.version}
- Prometheus 0.16.0

**Clustering:**
- Hazelcast 5.3.0

**Messaging:**
- Jakarta JMS 3.1.0

**Testing:**
- JUnit Jupiter ${junit.version}
- Mockito 5.5.0

## Benefits

### For Platform Developers

✅ **Single Source of Truth**: Update `pom.xml` in one place  
✅ **No Version Conflicts**: Guaranteed consistent versions  
✅ **Faster Module Creation**: Copy-paste dependency blocks without versions  
✅ **Easier Refactoring**: Rename/restructure without fixing versions everywhere

### For Downstream Projects

✅ **Version Compatibility**: Guaranteed compatible dependency versions  
✅ **Simplified Upgrades**: Import new BOM version to upgrade everything  
✅ **Conflict Resolution**: BOM overrides transitive dependency versions  
✅ **Documentation**: See all available modules in one place

## Maintenance

### Adding a New Module

1. Add module to parent `pom.xml` `<modules>` section
2. Add dependency declaration to `platform-bom/pom.xml`
3. Use version `${project.version}` for internal modules

### Updating External Dependencies

1. Update version property in parent `pom.xml` (e.g., `${jackson.version}`)
2. BOM automatically inherits via `<parent>`
3. No changes needed in BOM itself

### Version Numbering

BOM version matches parent project version (X.Y format):
- **1.0** - Initial release
- **1.1** - Minor updates
- **2.0** - Major version bump

## Example: Migrating to BOM

### Before (Without BOM)

```xml
<!-- platform-messaging/pom.xml -->
<dependencies>
    <dependency>
        <groupId>org.flossware.jplatform</groupId>
        <artifactId>platform-api</artifactId>
        <version>1.0</version>
    </dependency>
    <dependency>
        <groupId>org.slf4j</groupId>
        <artifactId>slf4j-api</artifactId>
        <version>2.0.9</version>
    </dependency>
</dependencies>
```

### After (With BOM)

```xml
<!-- platform-messaging/pom.xml -->
<dependencies>
    <dependency>
        <groupId>org.flossware.jplatform</groupId>
        <artifactId>platform-api</artifactId>
        <!-- Version inherited from BOM -->
    </dependency>
    <dependency>
        <groupId>org.slf4j</groupId>
        <artifactId>slf4j-api</artifactId>
        <!-- Version inherited from BOM -->
    </dependency>
</dependencies>
```

## Best Practices

1. **Always import BOM in downstream projects** - ensures compatibility
2. **Never override BOM versions** unless absolutely necessary
3. **Keep BOM up to date** with parent project version
4. **Document version overrides** in downstream project README
5. **Test BOM changes** across all modules before release

## Troubleshooting

### Dependency Version Conflicts

```bash
# Check effective POM to see resolved versions
mvn help:effective-pom

# Analyze dependency tree
mvn dependency:tree
```

### BOM Not Being Used

Ensure parent POM is set correctly:

```xml
<parent>
    <groupId>org.flossware.jplatform</groupId>
    <artifactId>platform-java</artifactId>
    <version>1.0</version>
</parent>
```

## References

- [Maven BOM Documentation](https://maven.apache.org/guides/introduction/introduction-to-dependency-mechanism.html#bill-of-materials-bom-poms)
- [FlossWare Platform Documentation](../README.md)
- [Issue #329](https://github.com/FlossWare/platform-java/issues/329)

---

**Module**: platform-bom  
**Version**: 1.0  
**Last Updated**: May 29, 2026
