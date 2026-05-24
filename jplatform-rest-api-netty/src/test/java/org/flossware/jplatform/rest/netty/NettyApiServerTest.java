package org.flossware.jplatform.rest.netty;

import org.flossware.jplatform.api.ServerShutdownException;
import org.junit.jupiter.api.Test;

import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NettyApiServerTest {

    @Test
    void testConstructorNullConfig() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            new NettyApiServer(null);
        });
        assertTrue(exception.getMessage().contains("Config"));
    }

    @Test
    void testGetPort() {
        NettyApiServerConfig config = NettyApiServerConfig.builder()
            .port(9090)
            .build();

        NettyApiServer server = new NettyApiServer(config);
        assertEquals(9090, server.getPort());
    }

    @Test
    void testIsRunningInitially() {
        NettyApiServerConfig config = NettyApiServerConfig.builder().build();

        NettyApiServer server = new NettyApiServer(config);
        assertFalse(server.isRunning());
    }

    @Test
    void testAddRoute() {
        NettyApiServerConfig config = NettyApiServerConfig.builder().build();
        NettyApiServer server = new NettyApiServer(config);

        Function<String, String> handler = input -> "{\"result\":\"ok\"}";
        server.addRoute("/api/test", handler);

        assertTrue(server.getRoutes().containsKey("/api/test"));
    }

    @Test
    void testRemoveRoute() {
        NettyApiServerConfig config = NettyApiServerConfig.builder().build();
        NettyApiServer server = new NettyApiServer(config);

        Function<String, String> handler = input -> "{\"result\":\"ok\"}";
        server.addRoute("/api/test", handler);
        server.removeRoute("/api/test");

        assertFalse(server.getRoutes().containsKey("/api/test"));
    }

    @Test
    void testGetConfig() {
        NettyApiServerConfig config = NettyApiServerConfig.builder()
            .port(9090)
            .build();

        NettyApiServer server = new NettyApiServer(config);
        assertSame(config, server.getConfig());
    }

    @Test
    void testMultipleRoutes() {
        NettyApiServerConfig config = NettyApiServerConfig.builder().build();
        NettyApiServer server = new NettyApiServer(config);

        server.addRoute("/api/users", input -> "{\"users\":[]}");
        server.addRoute("/api/products", input -> "{\"products\":[]}");
        server.addRoute("/api/orders", input -> "{\"orders\":[]}");

        assertEquals(3, server.getRoutes().size());
        assertTrue(server.getRoutes().containsKey("/api/users"));
        assertTrue(server.getRoutes().containsKey("/api/products"));
        assertTrue(server.getRoutes().containsKey("/api/orders"));
    }

    @Test
    void testRouteHandler() {
        NettyApiServerConfig config = NettyApiServerConfig.builder().build();
        NettyApiServer server = new NettyApiServer(config);

        Function<String, String> handler = input -> "{\"echo\":\"" + input + "\"}";
        server.addRoute("/api/echo", handler);

        Function<String, String> retrievedHandler = server.getRoutes().get("/api/echo");
        assertNotNull(retrievedHandler);
        assertEquals("{\"echo\":\"test\"}", retrievedHandler.apply("test"));
    }

    @Test
    void testReplaceRoute() {
        NettyApiServerConfig config = NettyApiServerConfig.builder().build();
        NettyApiServer server = new NettyApiServer(config);

        server.addRoute("/api/test", input -> "v1");
        server.addRoute("/api/test", input -> "v2");

        Function<String, String> handler = server.getRoutes().get("/api/test");
        assertEquals("v2", handler.apply(""));
    }

    @Test
    void testStopIdempotent() throws ServerShutdownException {
        NettyApiServerConfig config = NettyApiServerConfig.builder().build();
        NettyApiServer server = new NettyApiServer(config);

        server.stop();
        server.stop();

        assertFalse(server.isRunning());
    }

    @Test
    void testCloseStopsServer() throws Exception {
        NettyApiServerConfig config = NettyApiServerConfig.builder().build();
        NettyApiServer server = new NettyApiServer(config);

        server.close();
        assertFalse(server.isRunning());
    }

    @Test
    void testCustomHost() {
        NettyApiServerConfig config = NettyApiServerConfig.builder()
            .host("127.0.0.1")
            .build();

        NettyApiServer server = new NettyApiServer(config);
        assertEquals("127.0.0.1", server.getConfig().getHost());
    }

    @Test
    void testCustomMaxContentLength() {
        NettyApiServerConfig config = NettyApiServerConfig.builder()
            .maxContentLength(131072)
            .build();

        NettyApiServer server = new NettyApiServer(config);
        assertEquals(131072, server.getConfig().getMaxContentLength());
    }

    @Test
    void testCustomBossThreads() {
        NettyApiServerConfig config = NettyApiServerConfig.builder()
            .bossThreads(2)
            .build();

        NettyApiServer server = new NettyApiServer(config);
        assertEquals(2, server.getConfig().getBossThreads());
    }

    @Test
    void testCustomWorkerThreads() {
        NettyApiServerConfig config = NettyApiServerConfig.builder()
            .workerThreads(8)
            .build();

        NettyApiServer server = new NettyApiServer(config);
        assertEquals(8, server.getConfig().getWorkerThreads());
    }

    @Test
    void testKeepAliveDisabled() {
        NettyApiServerConfig config = NettyApiServerConfig.builder()
            .keepAlive(false)
            .build();

        NettyApiServer server = new NettyApiServer(config);
        assertFalse(server.getConfig().isKeepAlive());
    }

    @Test
    void testCustomBacklog() {
        NettyApiServerConfig config = NettyApiServerConfig.builder()
            .backlog(256)
            .build();

        NettyApiServer server = new NettyApiServer(config);
        assertEquals(256, server.getConfig().getBacklog());
    }

    @Test
    void testRemoveNonExistentRoute() {
        NettyApiServerConfig config = NettyApiServerConfig.builder().build();
        NettyApiServer server = new NettyApiServer(config);

        assertDoesNotThrow(() -> server.removeRoute("/api/nonexistent"));
    }

    @Test
    void testGetRoutesReturnsInternalMap() {
        NettyApiServerConfig config = NettyApiServerConfig.builder().build();
        NettyApiServer server = new NettyApiServer(config);

        server.addRoute("/test", input -> "response");
        assertTrue(server.getRoutes().containsKey("/test"));
    }
}
