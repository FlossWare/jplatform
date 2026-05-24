package org.flossware.jplatform.cluster.etcd;

import io.etcd.jetcd.Client;
import org.flossware.jplatform.api.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EtcdClusterManagerTest {
    private EtcdConfig config;
    private Client mockClient;

    @BeforeEach
    void setUp() {
        config = EtcdConfig.builder().build();
        mockClient = mock(Client.class);
    }

    @Test
    void testConstruction() {
        EtcdClusterManager manager = new EtcdClusterManager(config);
        assertNotNull(manager);
        assertFalse(manager.isJoined());
    }

    @Test
    void testIsJoined() {
        EtcdClusterManager manager = new EtcdClusterManager(config);
        assertFalse(manager.isJoined());
    }

    @Test
    void testIsLeader() {
        EtcdClusterManager manager = new EtcdClusterManager(config);
        assertFalse(manager.isLeader());
    }

    @Test
    void testGetLocalNode_NotJoined() {
        EtcdClusterManager manager = new EtcdClusterManager(config);
        assertNull(manager.getLocalNode());
    }

    @Test
    void testGetNodes() {
        EtcdClusterManager manager = new EtcdClusterManager(config);
        assertTrue(manager.getNodes().isEmpty());
    }

    @Test
    void testAddListener() {
        EtcdClusterManager manager = new EtcdClusterManager(config);
        ClusterEventListener listener = mock(ClusterEventListener.class);
        assertDoesNotThrow(() -> manager.addListener(listener));
    }

    @Test
    void testRemoveListener() {
        EtcdClusterManager manager = new EtcdClusterManager(config);
        ClusterEventListener listener = mock(ClusterEventListener.class);
        assertDoesNotThrow(() -> manager.removeListener(listener));
    }

    @Test
    void testLeave_NotJoined() {
        EtcdClusterManager manager = new EtcdClusterManager(config);
        assertDoesNotThrow(() -> manager.leave());
    }

    @Test
    void testClose() {
        EtcdClusterManager manager = new EtcdClusterManager(config);
        assertDoesNotThrow(() -> manager.close());
    }

    @Test
    void testGetEtcdClient() {
        EtcdClusterManager manager = new EtcdClusterManager(config, mockClient);
        assertSame(mockClient, manager.getEtcdClient());
    }
}
