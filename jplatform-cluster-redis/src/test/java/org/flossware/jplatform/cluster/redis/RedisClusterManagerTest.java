package org.flossware.jplatform.cluster.redis;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RedisClusterManagerTest {
    @Test
    void testBasicFunctionality() {
        RedisClusterManager manager = new RedisClusterManager();
        assertFalse(manager.isJoined());
    }
}
