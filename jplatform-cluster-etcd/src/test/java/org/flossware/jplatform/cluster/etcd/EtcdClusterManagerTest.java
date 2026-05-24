package org.flossware.jplatform.cluster.etcd;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EtcdClusterManagerTest {
    @Test
    void testBasicFunctionality() {
        EtcdClusterManager manager = new EtcdClusterManager();
        assertFalse(manager.isJoined());
    }
}
