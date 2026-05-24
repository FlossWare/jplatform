package org.flossware.jplatform.cluster.consul;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ConsulClusterManagerTest {
    @Test
    void testBasicFunctionality() {
        ConsulClusterManager manager = new ConsulClusterManager();
        assertFalse(manager.isJoined());
    }
}
