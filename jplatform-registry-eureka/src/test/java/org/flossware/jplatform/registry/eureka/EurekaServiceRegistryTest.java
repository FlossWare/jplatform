package org.flossware.jplatform.registry.eureka;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EurekaServiceRegistryTest {
    @Test
    void testBasicFunctionality() {
        EurekaServiceRegistry registry = new EurekaServiceRegistry();
        assertNotNull(registry);
    }
}
