package org.flossware.jplatform.config.vault;

import com.bettercloud.vault.Vault;
import com.bettercloud.vault.VaultConfig;
import com.bettercloud.vault.VaultException;
import com.bettercloud.vault.response.LogicalResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * HashiCorp Vault-based configuration source.
 * Loads configuration from Vault secrets and provides secure access to sensitive data.
 *
 * <p>This implementation:
 * <ul>
 *   <li>Stores configuration in Vault secrets engine</li>
 *   <li>Supports token-based authentication</li>
 *   <li>Maintains local cache for fast access</li>
 *   <li>Thread-safe operations</li>
 * </ul>
 *
 * <p>Thread Safety: This class is thread-safe. All mutable state is protected
 * by concurrent collections.
 *
 * @since 1.1
 */
public class VaultConfigSource implements AutoCloseable {
    private static final Logger logger = LoggerFactory.getLogger(VaultConfigSource.class);

    private final VaultConfigSourceConfig config;
    private final Map<String, String> configCache;
    private Vault vault;
    private volatile boolean started = false;

    /**
     * Constructs a new Vault configuration source.
     *
     * @param config the Vault configuration
     */
    public VaultConfigSource(VaultConfigSourceConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("Config must not be null");
        }
        this.config = config;
        this.configCache = new ConcurrentHashMap<>();
    }

    /**
     * Package-private constructor for testing.
     *
     * @param config the configuration
     * @param vault the Vault client
     */
    VaultConfigSource(VaultConfigSourceConfig config, Vault vault) {
        this.config = config;
        this.vault = vault;
        this.configCache = new ConcurrentHashMap<>();
        this.started = true;
    }

    /**
     * Starts the Vault configuration source.
     * Must be called before using the configuration source.
     */
    public void start() {
        if (started) {
            return;
        }

        try {
            VaultConfig vaultConfig = new VaultConfig()
                .address(config.getAddress())
                .token(config.getToken())
                .openTimeout(config.getOpenTimeout())
                .readTimeout(config.getReadTimeout())
                .build();

            vault = new Vault(vaultConfig)
                .withRetries(config.getMaxRetries(), config.getRetryIntervalMs());

            loadAllConfig();

            started = true;
            logger.info("Vault config source started: {}", config.getAddress());
        } catch (Exception e) {
            logger.error("Failed to start Vault config source", e);
            throw new RuntimeException("Failed to start Vault client", e);
        }
    }

    /**
     * Loads all configuration from Vault.
     *
     * @return map of all configuration key-value pairs
     */
    public Map<String, String> loadConfig() {
        if (!started) {
            return new HashMap<>();
        }
        return new HashMap<>(configCache);
    }

    /**
     * Gets a configuration value by key.
     *
     * @param key the configuration key
     * @return the value, or null if not found
     */
    public String getConfig(String key) {
        return configCache.get(key);
    }

    /**
     * Sets a configuration value in Vault.
     *
     * @param key the configuration key
     * @param value the configuration value
     */
    public void setConfig(String key, String value) {
        if (!started) {
            throw new IllegalStateException("Config source not started");
        }

        try {
            Map<String, Object> data = new HashMap<>();
            data.put(key, value);

            vault.logical().write(config.getSecretPath(), data);

            configCache.put(key, value);
            logger.debug("Set config: {} = {}", key, value);
        } catch (VaultException e) {
            logger.error("Failed to set config: {}", key, e);
            throw new RuntimeException("Failed to set config", e);
        }
    }

    /**
     * Deletes a configuration key from Vault.
     *
     * @param key the configuration key
     */
    public void deleteConfig(String key) {
        if (!started) {
            throw new IllegalStateException("Config source not started");
        }

        try {
            vault.logical().delete(config.getSecretPath() + "/" + key);

            configCache.remove(key);
            logger.debug("Deleted config: {}", key);
        } catch (VaultException e) {
            logger.error("Failed to delete config: {}", key, e);
            throw new RuntimeException("Failed to delete config", e);
        }
    }

    /**
     * Refreshes the configuration from Vault.
     * Reloads all secrets from the Vault server.
     */
    public void refresh() {
        if (!started) {
            throw new IllegalStateException("Config source not started");
        }
        loadAllConfig();
    }

    @Override
    public void close() {
        configCache.clear();
        started = false;

        logger.info("Vault config source closed");
    }

    /**
     * Returns the Vault client.
     *
     * @return the client
     */
    public Vault getVault() {
        return vault;
    }

    /**
     * Returns the configuration cache.
     *
     * @return the cache
     */
    Map<String, String> getConfigCache() {
        return configCache;
    }

    private void loadAllConfig() {
        try {
            LogicalResponse response = vault.logical().read(config.getSecretPath());

            if (response != null && response.getData() != null) {
                for (Map.Entry<String, String> entry : response.getData().entrySet()) {
                    configCache.put(entry.getKey(), entry.getValue());
                }
            }
        } catch (VaultException e) {
            logger.warn("Failed to load config from Vault", e);
        }
    }
}
