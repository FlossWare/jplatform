package org.flossware.jplatform.registry.etcd;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EtcdServiceRegistryTest {
    @Test
    void testBasicFunctionality() {
        EtcdServiceRegistry registry = new EtcdServiceRegistry();
        assertNotNull(registry);
    }
}
