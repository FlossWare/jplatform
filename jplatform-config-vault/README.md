# JPlatform Config - Vault

HashiCorp Vault-based configuration source implementation for JPlatform.

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
    <groupId>org.flossware.jplatform</groupId>
    <artifactId>jplatform-config-vault</artifactId>
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
