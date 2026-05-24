package org.flossware.jplatform.config.consul;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ConsulConfigSourceTest {
    @Test
    void testBasicFunctionality() {
        ConsulConfigSource source = new ConsulConfigSource();
        assertNotNull(source.loadConfig("/test"));
    }
}
