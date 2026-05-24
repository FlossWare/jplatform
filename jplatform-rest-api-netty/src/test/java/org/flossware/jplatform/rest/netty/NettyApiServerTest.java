package org.flossware.jplatform.rest.netty;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class NettyApiServerTest {
    @Test
    void testBasicFunctionality() throws Exception {
        NettyApiServer server = new NettyApiServer();
        assertFalse(server.isRunning());
        server.start();
        assertTrue(server.isRunning());
        server.stop();
        assertFalse(server.isRunning());
    }
}
