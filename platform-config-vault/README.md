# platform-java Config - Vault

HashiCorp Vault-based configuration source implementation for platform-java.

## Features

- **Secure Configuration**: Store sensitive configuration in HashiCorp Vault
- **Token Authentication**: Support for Vault token-based authentication
- **Dynamic Loading**: Load configuration from Vault secrets engine
- **Configuration Refresh**: Manually refresh configuration from Vault
- **Thread-Safe**: Concurrent access to configuration
- **Enterprise Support**: Optional namespace support for Vault Enterprise

## Dependencies

```xml
<dependency>
    <groupId>org.flossware.platform-java</groupId>
    <artifactId>platform-java-config-vault</artifactId>
    <version>1.1</version>
</dependency>
```

## Usage

### Basic Configuration

```java
VaultConfigSourceConfig config = VaultConfigSourceConfig.builder()
    .address("http://localhost:8200")
    .token("s.1234567890abcdef")
    .secretPath("secret/myapp")
    .build();

try (VaultConfigSource configSource = new VaultConfigSource(config)) {
    configSource.start();
    
    // Set configuration
    configSource.setConfig("database.password", "secret123");
    configSource.setConfig("api.key", "key-xyz");
    
    // Get configuration
    String dbPassword = configSource.getConfig("database.password");
    
    // Load all configuration
    Map<String, String> allConfig = configSource.loadConfig();
}
```

### Production Setup with HTTPS

```java
VaultConfigSourceConfig config = VaultConfigSourceConfig.builder()
    .address("https://vault.example.com:8200")
    .token(System.getenv("VAULT_TOKEN"))
    .secretPath("secret/production/myapp")
    .maxRetries(5)
    .retryIntervalMs(2000)
    .openTimeout(10)
    .readTimeout(60)
    .build();

try (VaultConfigSource configSource = new VaultConfigSource(config)) {
    configSource.start();
    // Use configuration...
}
```

### With Vault Enterprise Namespace

```java
VaultConfigSourceConfig config = VaultConfigSourceConfig.builder()
    .address("https://vault.example.com:8200")
    .token("s.1234567890abcdef")
    .namespace("production")
    .secretPath("secret/myapp")
    .build();

try (VaultConfigSource configSource = new VaultConfigSource(config)) {
    configSource.start();
    // Use configuration...
}
```

### Refreshing Configuration

```java
VaultConfigSourceConfig config = VaultConfigSourceConfig.builder()
    .address("http://localhost:8200")
    .token("s.1234567890abcdef")
    .secretPath("secret/myapp")
    .build();

try (VaultConfigSource configSource = new VaultConfigSource(config)) {
    configSource.start();
    
    // Load initial configuration
    Map<String, String> config1 = configSource.loadConfig();
    
    // Update secrets in Vault...
    
    // Refresh configuration from Vault
    configSource.refresh();
    
    // Get updated configuration
    Map<String, String> config2 = configSource.loadConfig();
}
```

## Configuration Options

| Option | Default | Description |
|--------|---------|-------------|
| address | http://localhost:8200 | Vault server address |
| token | (required) | Vault authentication token |
| secretPath | secret/config | Path to secrets in Vault |
| namespace | null | Vault Enterprise namespace (optional) |
| maxRetries | 3 | Maximum number of connection retries |
| retryIntervalMs | 1000 | Delay between retries in milliseconds |
| openTimeout | 5 | Connection open timeout in seconds |
| readTimeout | 30 | Read timeout in seconds |

## Thread Safety

All operations are thread-safe. Multiple threads can safely read and write configuration concurrently.

## Security Considerations

- Store Vault tokens securely (environment variables, secret managers)
- Use HTTPS in production environments
- Rotate Vault tokens regularly
- Use appropriate Vault policies to limit secret access
- Consider using Vault's dynamic secrets for enhanced security

## Error Handling

Failed operations throw `RuntimeException` with descriptive messages. Connection failures are retried according to `maxRetries` and `retryIntervalMs` settings.

## Testing

### Test Coverage: 87%

This module has excellent unit test coverage of all business logic and API methods. The small amount of uncovered code consists of infrastructure initialization.

**What IS Tested:**
- ✅ All configuration builder validation
- ✅ Get/set/delete configuration operations
- ✅ Configuration refresh functionality
- ✅ Configuration caching
- ✅ Error handling and exception paths
- ✅ Thread safety (concurrent operations)
- ✅ Edge cases and null checks
- ✅ Vault exception handling

**What is NOT Tested (and why):**
- ❌ **Vault client connection/bootstrap** - Requires real Vault server or complex mocking of the Vault client library internals
- ❌ **VaultConfig builder execution** - Configuration object creation within the Vault client library

**Why Not 100%?**

This module integrates with HashiCorp Vault, an external secrets management service. The small amount of untested code involves:
1. Creating and configuring the Vault client connection
2. Setting timeout and retry parameters in the VaultConfig builder
3. Initial connection establishment to Vault server

Testing these paths would require:
- Integration tests with TestContainers running real Vault
- Complex mocking of third-party library internals (anti-pattern)

At 87% coverage, this module has excellent test quality. The untested paths are minimal and consist only of framework initialization code that is better validated through integration testing. All business logic, error handling, and API methods are thoroughly tested.
