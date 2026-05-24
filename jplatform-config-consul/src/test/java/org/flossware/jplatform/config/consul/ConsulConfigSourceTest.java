package org.flossware.jplatform.config.consul;

import com.orbitz.consul.Consul;
import com.orbitz.consul.KeyValueClient;
import com.orbitz.consul.model.kv.Value;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ConsulConfigSourceTest {

    @Mock
    private Consul consul;

    @Mock
    private KeyValueClient kvClient;

    private ConsulConfigSourceConfig config;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        config = ConsulConfigSourceConfig.builder()
            .host("localhost")
            .port(8500)
            .watchEnabled(false)
            .build();

        when(consul.keyValueClient()).thenReturn(kvClient);
    }

    @Test
    void testConstructorNullConfig() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            new ConsulConfigSource(null);
        });
        assertTrue(exception.getMessage().contains("Config"));
    }

    @Test
    void testLoadConfig() {
        ConsulConfigSource source = new ConsulConfigSource(config, consul);

        when(kvClient.getValues(anyString())).thenReturn(Collections.emptyList());

        Map<String, String> loadedConfig = source.loadConfig();
        assertNotNull(loadedConfig);
    }

    @Test
    void testGetConfig() {
        ConsulConfigSource source = new ConsulConfigSource(config, consul);
        source.getConfigCache().put("test.key", "test-value");

        assertEquals("test-value", source.getConfig("test.key"));
    }

    @Test
    void testGetConfigNotFound() {
        ConsulConfigSource source = new ConsulConfigSource(config, consul);

        assertNull(source.getConfig("nonexistent"));
    }

    @Test
    void testSetConfig() {
        ConsulConfigSource source = new ConsulConfigSource(config, consul);

        source.setConfig("new.key", "new-value");

        verify(kvClient).putValue(anyString(), eq("new-value"));
        assertEquals("new-value", source.getConfig("new.key"));
    }

    @Test
    void testDeleteConfig() {
        ConsulConfigSource source = new ConsulConfigSource(config, consul);
        source.getConfigCache().put("delete.key", "value");

        source.deleteConfig("delete.key");

        verify(kvClient).deleteKey(anyString());
        assertNull(source.getConfig("delete.key"));
    }

    @Test
    void testSetConfigNotStarted() {
        ConsulConfigSource source = new ConsulConfigSource(config);

        Exception exception = assertThrows(IllegalStateException.class, () -> {
            source.setConfig("key", "value");
        });
        assertTrue(exception.getMessage().contains("not started"));
    }

    @Test
    void testDeleteConfigNotStarted() {
        ConsulConfigSource source = new ConsulConfigSource(config);

        Exception exception = assertThrows(IllegalStateException.class, () -> {
            source.deleteConfig("key");
        });
        assertTrue(exception.getMessage().contains("not started"));
    }

    @Test
    void testAddListener() {
        ConsulConfigSource source = new ConsulConfigSource(config, consul);

        List<Map<String, String>> receivedConfigs = new ArrayList<>();
        source.addListener("test-listener", receivedConfigs::add);

        // Trigger notification manually
        source.getConfigCache().put("key", "value");
    }

    @Test
    void testRemoveListener() {
        ConsulConfigSource source = new ConsulConfigSource(config, consul);

        source.addListener("test-listener", cfg -> {});
        source.removeListener("test-listener");
    }

    @Test
    void testClose() {
        ConsulConfigSource source = new ConsulConfigSource(config, consul);

        source.close();

        verify(consul).destroy();
        assertTrue(source.getConfigCache().isEmpty());
    }

    @Test
    void testGetConsul() {
        ConsulConfigSource source = new ConsulConfigSource(config, consul);

        assertSame(consul, source.getConsul());
    }

    @Test
    void testLoadConfigBeforeStart() {
        ConsulConfigSource source = new ConsulConfigSource(config);

        Map<String, String> loaded = source.loadConfig();
        assertTrue(loaded.isEmpty());
    }

    @Test
    void testStartIdempotent() {
        ConsulConfigSource source = new ConsulConfigSource(config, consul);

        when(kvClient.getValues(anyString())).thenReturn(Collections.emptyList());

        source.start();
        source.start();

        verify(kvClient, times(0)).getValues(anyString());
        source.close();
    }
}
