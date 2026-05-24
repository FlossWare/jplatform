package org.flossware.jplatform.config.etcd;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EtcdConfigSourceTest {
    @Test
    void testBasicFunctionality() {
        EtcdConfigSource source = new EtcdConfigSource();
        assertNotNull(source.loadConfig("/test"));
    }
}
