package org.flossware.jplatform.api;

/**
 * Represents a node in the platform cluster.
 * Contains information about node identity, location, and health status.
 *
 * @see ClusterManager
 */
public class ClusterNode {
    private final String nodeId;
    private final String address;
    private final int port;
    private final NodeState state;
    private final long lastHeartbeat;

    /**
     * Constructs a new cluster node.
     *
     * @param nodeId the unique node identifier
     * @param address the node network address
     * @param port the node port
     * @param state the node state
     * @param lastHeartbeat the last heartbeat timestamp
     */
    public ClusterNode(String nodeId, String address, int port, NodeState state, long lastHeartbeat) {
        this.nodeId = nodeId;
        this.address = address;
        this.port = port;
        this.state = state;
        this.lastHeartbeat = lastHeartbeat;
    }

    /**
     * Returns the unique node identifier.
     *
     * @return the node ID
     */
    public String getNodeId() {
        return nodeId;
    }

    /**
     * Returns the node network address.
     *
     * @return the address
     */
    public String getAddress() {
        return address;
    }

    /**
     * Returns the node port.
     *
     * @return the port
     */
    public int getPort() {
        return port;
    }

    /**
     * Returns the current node state.
     *
     * @return the node state
     */
    public NodeState getState() {
        return state;
    }

    /**
     * Returns the last heartbeat timestamp.
     *
     * @return the timestamp in milliseconds since epoch
     */
    public long getLastHeartbeat() {
        return lastHeartbeat;
    }

    @Override
    public String toString() {
        return String.format("ClusterNode{nodeId='%s', address='%s', port=%d, state=%s}",
                nodeId, address, port, state);
    }

    /**
     * Node states in the cluster lifecycle.
     */
    public enum NodeState {
        /** Node is joining the cluster */
        JOINING,
        /** Node is active and healthy */
        ACTIVE,
        /** Node is suspected to be unhealthy */
        SUSPECT,
        /** Node is leaving the cluster */
        LEAVING,
        /** Node is dead or unreachable */
        DEAD
    }
}
