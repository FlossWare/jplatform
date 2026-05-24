package org.flossware.jplatform.registry.consul;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ConsulServiceRegistryTest {
    @Test
    void testBasicFunctionality() {
        ConsulServiceRegistry registry = new ConsulServiceRegistry();
        assertNotNull(registry);
    }
}
