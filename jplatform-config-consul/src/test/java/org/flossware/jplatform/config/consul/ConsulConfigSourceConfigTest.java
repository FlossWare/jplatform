package org.flossware.jplatform.config.consul;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConsulConfigSourceConfigTest {

    @Test
    void testBuilderDefaults() {
        ConsulConfigSourceConfig config = ConsulConfigSourceConfig.builder().build();
        assertEquals("localhost", config.getHost());
        assertEquals(8500, config.getPort());
        assertNull(config.getToken());
        assertEquals("config", config.getKeyPrefix());
        assertTrue(config.isWatchEnabled());
        assertEquals(10, config.getWatchIntervalSeconds());
    }

    @Test
    void testBuilderWithAllFields() {
        ConsulConfigSourceConfig config = ConsulConfigSourceConfig.builder()
            .host("consul.example.com")
            .port(8501)
            .token("secret-token")
            .keyPrefix("myapp/config")
            .watchEnabled(false)
            .watchIntervalSeconds(30)
            .build();

        assertEquals("consul.example.com", config.getHost());
        assertEquals(8501, config.getPort());
        assertEquals("secret-token", config.getToken());
        assertEquals("myapp/config", config.getKeyPrefix());
        assertFalse(config.isWatchEnabled());
        assertEquals(30, config.getWatchIntervalSeconds());
    }

    @Test
    void testPortValidation() {
        assertThrows(IllegalArgumentException.class, () -> 
            ConsulConfigSourceConfig.builder().port(0).build());
        assertThrows(IllegalArgumentException.class, () -> 
            ConsulConfigSourceConfig.builder().port(65536).build());
    }

    @Test
    void testWatchIntervalValidation() {
        assertThrows(IllegalArgumentException.class, () -> 
            ConsulConfigSourceConfig.builder().watchIntervalSeconds(0).build());
    }

    @Test
    void testHostValidation() {
        assertThrows(IllegalStateException.class, () -> 
            ConsulConfigSourceConfig.builder().host(null).build());
        assertThrows(IllegalStateException.class, () -> 
            ConsulConfigSourceConfig.builder().host("  ").build());
    }

    @Test
    void testKeyPrefixValidation() {
        assertThrows(IllegalStateException.class, () -> 
            ConsulConfigSourceConfig.builder().keyPrefix(null).build());
    }
}
