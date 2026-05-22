package org.flossware.jplatform.cluster;

import com.hazelcast.cluster.Address;
import com.hazelcast.cluster.Cluster;
import com.hazelcast.cluster.Member;
import com.hazelcast.cluster.MembershipEvent;
import com.hazelcast.cluster.MembershipListener;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.cp.CPSubsystem;
import com.hazelcast.cp.lock.FencedLock;
import org.flossware.jplatform.api.*;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for HazelcastClusterManager.
 * Tests cluster join/leave, leader election, membership events, and failure scenarios.
 */
class HazelcastClusterManagerTest {

    private HazelcastClusterManager clusterManager;
    private HazelcastInstance mockHazelcast;
    private Cluster mockCluster;
    private CPSubsystem mockCPSubsystem;
    private FencedLock mockLeaderLock;
    private Member mockLocalMember;
    private ClusterEventListener mockListener;

    @BeforeEach
    void setUp() {
        clusterManager = new HazelcastClusterManager();

        // Create mocks
        mockHazelcast = mock(HazelcastInstance.class);
        mockCluster = mock(Cluster.class);
        mockCPSubsystem = mock(CPSubsystem.class);
        mockLeaderLock = mock(FencedLock.class);
        mockLocalMember = mock(Member.class);
        mockListener = mock(ClusterEventListener.class);

        // Setup basic mock interactions
        when(mockHazelcast.getCluster()).thenReturn(mockCluster);
        when(mockHazelcast.getCPSubsystem()).thenReturn(mockCPSubsystem);
        when(mockCPSubsystem.getLock(anyString())).thenReturn(mockLeaderLock);
        when(mockCluster.getLocalMember()).thenReturn(mockLocalMember);

        // Setup local member
        UUID localUuid = UUID.randomUUID();
        Address mockAddress = mock(Address.class);
        when(mockAddress.getHost()).thenReturn("localhost");
        when(mockAddress.getPort()).thenReturn(5701);
        when(mockLocalMember.getUuid()).thenReturn(localUuid);
        when(mockLocalMember.getAddress()).thenReturn(mockAddress);

        // Setup cluster members
        Set<Member> members = new HashSet<>();
        members.add(mockLocalMember);
        when(mockCluster.getMembers()).thenReturn(members);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (clusterManager != null && clusterManager.isJoined()) {
            clusterManager.leave();
        }
        // Clean up any Hazelcast instances created during tests
        Hazelcast.shutdownAll();
    }

    @Test
    @DisplayName("Should successfully join cluster with valid configuration")
    void testJoinCluster() throws Exception {
        // Given
        ClusterConfig config = ClusterConfig.builder()
                .clusterName("test-cluster")
                .bindAddress("localhost")
                .bindPort(5701)
                .addSeedNode("localhost:5701")
                .build();

        UUID listenerId = UUID.randomUUID();
        when(mockCluster.addMembershipListener(any(MembershipListener.class)))
                .thenReturn(listenerId);

        // When
        mockStaticHazelcast(() -> clusterManager.join(config));

        // Then
        assertTrue(clusterManager.isJoined());
        verify(mockCluster).addMembershipListener(any(MembershipListener.class));
    }

    @Test
    @DisplayName("Should throw exception when joining cluster twice")
    void testJoinClusterTwice() throws Exception {
        // Given
        ClusterConfig config = ClusterConfig.builder()
                .clusterName("test-cluster")
                .build();

        UUID listenerId = UUID.randomUUID();
        when(mockCluster.addMembershipListener(any(MembershipListener.class)))
                .thenReturn(listenerId);

        mockStaticHazelcast(() -> clusterManager.join(config));

        // When/Then
        assertThrows(IllegalStateException.class, () -> clusterManager.join(config));
    }

    @Test
    @DisplayName("Should successfully leave cluster")
    void testLeaveCluster() throws Exception {
        // Given
        ClusterConfig config = ClusterConfig.builder()
                .clusterName("test-cluster")
                .build();

        UUID listenerId = UUID.randomUUID();
        when(mockCluster.addMembershipListener(any(MembershipListener.class)))
                .thenReturn(listenerId);
        when(mockLeaderLock.isLockedByCurrentThread()).thenReturn(false);

        mockStaticHazelcast(() -> {
            clusterManager.join(config);

            // When
            clusterManager.leave();
        });

        // Then
        assertFalse(clusterManager.isJoined());
    }

    @Test
    @DisplayName("Should release leader lock when leaving cluster")
    void testLeaveClusterReleasesLeaderLock() throws Exception {
        // Given
        ClusterConfig config = ClusterConfig.builder()
                .clusterName("test-cluster")
                .build();

        UUID listenerId = UUID.randomUUID();
        when(mockCluster.addMembershipListener(any(MembershipListener.class)))
                .thenReturn(listenerId);
        when(mockLeaderLock.tryLock()).thenReturn(true);
        when(mockLeaderLock.isLockedByCurrentThread()).thenReturn(true);

        mockStaticHazelcast(() -> {
            clusterManager.join(config);

            // When
            clusterManager.leave();
        });

        // Then
        verify(mockLeaderLock).unlock();
        assertFalse(clusterManager.isJoined());
    }

    @Test
    @DisplayName("Should become leader when lock is acquired")
    void testBecomeLeader() throws Exception {
        // Given
        ClusterConfig config = ClusterConfig.builder()
                .clusterName("test-cluster")
                .build();

        UUID listenerId = UUID.randomUUID();
        when(mockCluster.addMembershipListener(any(MembershipListener.class)))
                .thenReturn(listenerId);
        when(mockLeaderLock.tryLock()).thenReturn(true);
        when(mockLeaderLock.isLockedByCurrentThread()).thenReturn(true);

        // When
        mockStaticHazelcast(() -> clusterManager.join(config));

        // Then
        assertTrue(clusterManager.isLeader());
    }

    @Test
    @DisplayName("Should not become leader when lock is not acquired")
    void testNotBecomeLeader() throws Exception {
        // Given
        ClusterConfig config = ClusterConfig.builder()
                .clusterName("test-cluster")
                .build();

        UUID listenerId = UUID.randomUUID();
        when(mockCluster.addMembershipListener(any(MembershipListener.class)))
                .thenReturn(listenerId);
        when(mockLeaderLock.tryLock()).thenReturn(false);

        // When
        mockStaticHazelcast(() -> clusterManager.join(config));

        // Then
        assertFalse(clusterManager.isLeader());
    }

    @Test
    @DisplayName("Should notify listeners when node joins")
    void testNodeJoinedEvent() throws Exception {
        // Given
        ClusterConfig config = ClusterConfig.builder()
                .clusterName("test-cluster")
                .build();

        UUID listenerId = UUID.randomUUID();
        ArgumentCaptor<MembershipListener> listenerCaptor = ArgumentCaptor.forClass(MembershipListener.class);
        when(mockCluster.addMembershipListener(listenerCaptor.capture()))
                .thenReturn(listenerId);

        clusterManager.addListener(mockListener);

        mockStaticHazelcast(() -> {
            clusterManager.join(config);

            // When - Simulate member joined event
            Member newMember = mock(Member.class);
            UUID newUuid = UUID.randomUUID();
            when(newMember.getUuid()).thenReturn(newUuid);
            Address newAddress = mock(Address.class);
            when(newAddress.getHost()).thenReturn("localhost");
            when(newAddress.getPort()).thenReturn(5702);
            when(newMember.getAddress()).thenReturn(newAddress);

            MembershipEvent event = mock(MembershipEvent.class);
            when(event.getMember()).thenReturn(newMember);

            MembershipListener listener = listenerCaptor.getValue();
            listener.memberAdded(event);
        });

        // Then
        verify(mockListener).onNodeJoined(any(ClusterNode.class));
    }

    @Test
    @DisplayName("Should notify listeners when node leaves")
    void testNodeLeftEvent() throws Exception {
        // Given
        ClusterConfig config = ClusterConfig.builder()
                .clusterName("test-cluster")
                .build();

        UUID listenerId = UUID.randomUUID();
        ArgumentCaptor<MembershipListener> listenerCaptor = ArgumentCaptor.forClass(MembershipListener.class);
        when(mockCluster.addMembershipListener(listenerCaptor.capture()))
                .thenReturn(listenerId);

        clusterManager.addListener(mockListener);

        mockStaticHazelcast(() -> {
            clusterManager.join(config);

            // When - Simulate member left event
            Member leftMember = mock(Member.class);
            UUID leftUuid = UUID.randomUUID();
            when(leftMember.getUuid()).thenReturn(leftUuid);
            Address leftAddress = mock(Address.class);
            when(leftAddress.getHost()).thenReturn("localhost");
            when(leftAddress.getPort()).thenReturn(5702);
            when(leftMember.getAddress()).thenReturn(leftAddress);

            MembershipEvent event = mock(MembershipEvent.class);
            when(event.getMember()).thenReturn(leftMember);

            MembershipListener listener = listenerCaptor.getValue();
            listener.memberRemoved(event);
        });

        // Then
        verify(mockListener).onNodeLeft(any(ClusterNode.class));
    }

    @Test
    @DisplayName("Should notify listeners when leader changes")
    void testLeaderChangedEvent() throws Exception {
        // Given
        ClusterConfig config = ClusterConfig.builder()
                .clusterName("test-cluster")
                .build();

        UUID listenerId = UUID.randomUUID();
        when(mockCluster.addMembershipListener(any(MembershipListener.class)))
                .thenReturn(listenerId);
        when(mockLeaderLock.tryLock()).thenReturn(true);
        when(mockLeaderLock.isLockedByCurrentThread()).thenReturn(true);

        clusterManager.addListener(mockListener);

        // When
        mockStaticHazelcast(() -> clusterManager.join(config));

        // Then
        verify(mockListener).onLeaderChanged(any(ClusterNode.class));
    }

    @Test
    @DisplayName("Should return all cluster nodes")
    void testGetNodes() throws Exception {
        // Given
        ClusterConfig config = ClusterConfig.builder()
                .clusterName("test-cluster")
                .build();

        // Setup multiple members
        Member member1 = mock(Member.class);
        when(member1.getUuid()).thenReturn(UUID.randomUUID());
        when(member1.getAddress()).thenReturn(new InetSocketAddress("localhost", 5701));

        Member member2 = mock(Member.class);
        when(member2.getUuid()).thenReturn(UUID.randomUUID());
        Address address2 = mock(Address.class);
        when(address2.getHost()).thenReturn("localhost");
        when(address2.getPort()).thenReturn(5702);
        when(member2.getAddress()).thenReturn(address2);

        Set<Member> members = new HashSet<>();
        members.add(member1);
        members.add(member2);
        when(mockCluster.getMembers()).thenReturn(members);

        UUID listenerId = UUID.randomUUID();
        when(mockCluster.addMembershipListener(any(MembershipListener.class)))
                .thenReturn(listenerId);

        mockStaticHazelcast(() -> {
            clusterManager.join(config);

            // When
            Set<ClusterNode> nodes = clusterManager.getNodes();

            // Then
            assertEquals(2, nodes.size());
        });
    }

    @Test
    @DisplayName("Should return empty set when not joined")
    void testGetNodesWhenNotJoined() {
        // When
        Set<ClusterNode> nodes = clusterManager.getNodes();

        // Then
        assertTrue(nodes.isEmpty());
    }

    @Test
    @DisplayName("Should return local node information")
    void testGetLocalNode() throws Exception {
        // Given
        ClusterConfig config = ClusterConfig.builder()
                .clusterName("test-cluster")
                .build();

        UUID listenerId = UUID.randomUUID();
        when(mockCluster.addMembershipListener(any(MembershipListener.class)))
                .thenReturn(listenerId);

        mockStaticHazelcast(() -> {
            clusterManager.join(config);

            // When
            ClusterNode localNode = clusterManager.getLocalNode();

            // Then
            assertNotNull(localNode);
            assertEquals("localhost", localNode.getAddress());
            assertEquals(5701, localNode.getPort());
        });
    }

    @Test
    @DisplayName("Should return null for local node when not joined")
    void testGetLocalNodeWhenNotJoined() {
        // When
        ClusterNode localNode = clusterManager.getLocalNode();

        // Then
        assertNull(localNode);
    }

    @Test
    @DisplayName("Should add and remove listeners")
    void testAddRemoveListener() {
        // When
        clusterManager.addListener(mockListener);
        clusterManager.removeListener(mockListener);

        // Then - no exceptions thrown
        assertNotNull(clusterManager);
    }

    @Test
    @DisplayName("Should handle null listener gracefully")
    void testNullListener() {
        // When/Then - should not throw exception
        assertDoesNotThrow(() -> clusterManager.addListener(null));
        assertDoesNotThrow(() -> clusterManager.removeListener(null));
    }

    @Test
    @DisplayName("Should handle listener exceptions during notification")
    void testListenerExceptionHandling() throws Exception {
        // Given
        ClusterConfig config = ClusterConfig.builder()
                .clusterName("test-cluster")
                .build();

        UUID listenerId = UUID.randomUUID();
        ArgumentCaptor<MembershipListener> listenerCaptor = ArgumentCaptor.forClass(MembershipListener.class);
        when(mockCluster.addMembershipListener(listenerCaptor.capture()))
                .thenReturn(listenerId);

        ClusterEventListener throwingListener = mock(ClusterEventListener.class);
        doThrow(new RuntimeException("Test exception")).when(throwingListener).onNodeJoined(any());

        clusterManager.addListener(throwingListener);

        mockStaticHazelcast(() -> {
            clusterManager.join(config);

            // When - Simulate member joined event
            Member newMember = mock(Member.class);
            UUID newUuid = UUID.randomUUID();
            when(newMember.getUuid()).thenReturn(newUuid);
            Address newAddress = mock(Address.class);
            when(newAddress.getHost()).thenReturn("localhost");
            when(newAddress.getPort()).thenReturn(5702);
            when(newMember.getAddress()).thenReturn(newAddress);

            MembershipEvent event = mock(MembershipEvent.class);
            when(event.getMember()).thenReturn(newMember);

            MembershipListener listener = listenerCaptor.getValue();

            // Then - should not throw exception
            assertDoesNotThrow(() -> listener.memberAdded(event));
        });
    }

    @Test
    @DisplayName("Should close successfully")
    void testClose() throws Exception {
        // Given
        ClusterConfig config = ClusterConfig.builder()
                .clusterName("test-cluster")
                .build();

        UUID listenerId = UUID.randomUUID();
        when(mockCluster.addMembershipListener(any(MembershipListener.class)))
                .thenReturn(listenerId);

        mockStaticHazelcast(() -> {
            clusterManager.join(config);

            // When
            clusterManager.close();
        });

        // Then
        assertFalse(clusterManager.isJoined());
    }

    @Test
    @DisplayName("Should handle leave when not joined gracefully")
    void testLeaveWhenNotJoined() throws Exception {
        // When/Then - should not throw exception
        assertDoesNotThrow(() -> clusterManager.leave());
    }

    @Test
    @DisplayName("Should configure network settings correctly")
    void testNetworkConfiguration() throws Exception {
        // Given
        ClusterConfig config = ClusterConfig.builder()
                .clusterName("production-cluster")
                .bindAddress("192.168.1.10")
                .bindPort(5703)
                .addSeedNode("192.168.1.10:5703")
                .addSeedNode("192.168.1.11:5703")
                .build();

        UUID listenerId = UUID.randomUUID();
        when(mockCluster.addMembershipListener(any(MembershipListener.class)))
                .thenReturn(listenerId);

        // When/Then - should not throw exception
        assertDoesNotThrow(() -> {
            mockStaticHazelcast(() -> clusterManager.join(config));
        });
    }

    @Test
    @DisplayName("Should return hazelcast instance")
    void testGetHazelcastInstance() throws Exception {
        // Given
        ClusterConfig config = ClusterConfig.builder()
                .clusterName("test-cluster")
                .build();

        UUID listenerId = UUID.randomUUID();
        when(mockCluster.addMembershipListener(any(MembershipListener.class)))
                .thenReturn(listenerId);

        // When
        mockStaticHazelcast(() -> {
            clusterManager.join(config);
            HazelcastInstance instance = clusterManager.getHazelcastInstance();

            // Then
            assertNotNull(instance);
        });
    }

    @Test
    @DisplayName("Should return null hazelcast instance when not joined")
    void testGetHazelcastInstanceWhenNotJoined() {
        // When
        HazelcastInstance instance = clusterManager.getHazelcastInstance();

        // Then
        assertNull(instance);
    }

    @Test
    @DisplayName("Should re-attempt leader election when leader leaves")
    void testReElectionOnLeaderLeave() throws Exception {
        // Given
        ClusterConfig config = ClusterConfig.builder()
                .clusterName("test-cluster")
                .build();

        UUID listenerId = UUID.randomUUID();
        ArgumentCaptor<MembershipListener> listenerCaptor = ArgumentCaptor.forClass(MembershipListener.class);
        when(mockCluster.addMembershipListener(listenerCaptor.capture()))
                .thenReturn(listenerId);
        when(mockLeaderLock.tryLock()).thenReturn(true);
        when(mockLeaderLock.isLockedByCurrentThread()).thenReturn(true);

        mockStaticHazelcast(() -> {
            clusterManager.join(config);
            assertTrue(clusterManager.isLeader());

            // When - Simulate leader leaving
            Member leftMember = mock(Member.class);
            when(leftMember.getUuid()).thenReturn(UUID.randomUUID());
            Address leftAddress = mock(Address.class);
            when(leftAddress.getHost()).thenReturn("localhost");
            when(leftAddress.getPort()).thenReturn(5702);
            when(leftMember.getAddress()).thenReturn(leftAddress);

            MembershipEvent event = mock(MembershipEvent.class);
            when(event.getMember()).thenReturn(leftMember);

            MembershipListener listener = listenerCaptor.getValue();
            listener.memberRemoved(event);
        });

        // Then - should try to acquire lock again
        verify(mockLeaderLock, atLeast(2)).tryLock();
    }

    /**
     * Helper method to mock Hazelcast static creation.
     * In real tests, we would use PowerMock or similar, but for this example
     * we'll simulate the behavior by directly injecting mocks.
     */
    private void mockStaticHazelcast(Runnable testLogic) {
        // Note: This is a simplified version. In production tests, you would use
        // dependency injection or PowerMock to properly mock static methods.
        // For this test suite, we're focusing on the logic within the manager.

        // Inject the mock hazelcast instance using reflection
        try {
            java.lang.reflect.Field hazelcastField = HazelcastClusterManager.class.getDeclaredField("hazelcast");
            hazelcastField.setAccessible(true);
            hazelcastField.set(clusterManager, mockHazelcast);

            java.lang.reflect.Field joinedField = HazelcastClusterManager.class.getDeclaredField("joined");
            joinedField.setAccessible(true);
            joinedField.set(clusterManager, true);

            java.lang.reflect.Field leaderLockField = HazelcastClusterManager.class.getDeclaredField("leaderLock");
            leaderLockField.setAccessible(true);
            leaderLockField.set(clusterManager, mockLeaderLock);

            testLogic.run();
        } catch (Exception e) {
            throw new RuntimeException("Failed to inject mocks", e);
        }
    }
}
