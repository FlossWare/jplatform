package org.flossware.jplatform.rest.netty;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NettyApiServerConfigTest {

    @Test
    void testBuilderDefaults() {
        NettyApiServerConfig config = NettyApiServerConfig.builder().build();

        assertEquals("0.0.0.0", config.getHost());
        assertEquals(8080, config.getPort());
        assertEquals(1, config.getBossThreads());
        assertEquals(0, config.getWorkerThreads());
        assertEquals(65536, config.getMaxContentLength());
        assertTrue(config.isKeepAlive());
        assertEquals(128, config.getBacklog());
    }

    @Test
    void testBuilderCustomValues() {
        NettyApiServerConfig config = NettyApiServerConfig.builder()
            .host("127.0.0.1")
            .port(9090)
            .bossThreads(2)
            .workerThreads(8)
            .maxContentLength(131072)
            .keepAlive(false)
            .backlog(256)
            .build();

        assertEquals("127.0.0.1", config.getHost());
        assertEquals(9090, config.getPort());
        assertEquals(2, config.getBossThreads());
        assertEquals(8, config.getWorkerThreads());
        assertEquals(131072, config.getMaxContentLength());
        assertFalse(config.isKeepAlive());
        assertEquals(256, config.getBacklog());
    }

    @Test
    void testBuilderMissingHost() {
        Exception exception = assertThrows(IllegalStateException.class, () -> {
            NettyApiServerConfig.builder()
                .host("")
                .build();
        });
        assertTrue(exception.getMessage().contains("Host"));
    }

    @Test
    void testBuilderInvalidPort() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            NettyApiServerConfig.builder()
                .port(0)
                .build();
        });
        assertTrue(exception.getMessage().contains("between 1 and 65535"));
    }

    @Test
    void testBuilderInvalidPortTooHigh() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            NettyApiServerConfig.builder()
                .port(65536)
                .build();
        });
        assertTrue(exception.getMessage().contains("between 1 and 65535"));
    }

    @Test
    void testBuilderInvalidBossThreads() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            NettyApiServerConfig.builder()
                .bossThreads(0)
                .build();
        });
        assertTrue(exception.getMessage().contains("at least 1"));
    }

    @Test
    void testBuilderInvalidWorkerThreads() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            NettyApiServerConfig.builder()
                .workerThreads(-1)
                .build();
        });
        assertTrue(exception.getMessage().contains("at least 0"));
    }

    @Test
    void testBuilderInvalidMaxContentLength() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            NettyApiServerConfig.builder()
                .maxContentLength(512)
                .build();
        });
        assertTrue(exception.getMessage().contains("at least 1024 bytes"));
    }

    @Test
    void testBuilderInvalidBacklog() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            NettyApiServerConfig.builder()
                .backlog(0)
                .build();
        });
        assertTrue(exception.getMessage().contains("at least 1"));
    }

    @Test
    void testBuilderWithAutoDetectWorkers() {
        NettyApiServerConfig config = NettyApiServerConfig.builder()
            .workerThreads(0)
            .build();

        assertEquals(0, config.getWorkerThreads());
    }
}
