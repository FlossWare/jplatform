package org.flossware.jplatform.cluster.zookeeper;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ZookeeperClusterManagerTest {
    @Test
    void testBasicFunctionality() {
        ZookeeperClusterManager manager = new ZookeeperClusterManager();
        assertFalse(manager.isJoined());
    }
}
