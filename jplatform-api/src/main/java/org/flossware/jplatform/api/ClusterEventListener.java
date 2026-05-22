package org.flossware.jplatform.api;

/**
 * Listener for cluster membership events.
 * Notified when nodes join, leave, or when leadership changes.
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * ClusterEventListener listener = new ClusterEventListener() {
 *     @Override
 *     public void onNodeJoined(ClusterNode node) {
 *         System.out.println("Node joined: " + node.getNodeId());
 *     }
 *
 *     @Override
 *     public void onLeaderChanged(ClusterNode newLeader) {
 *         System.out.println("New leader: " + newLeader.getNodeId());
 *     }
 * };
 * }</pre>
 *
 * @see ClusterManager
 * @see ClusterNode
 */
public interface ClusterEventListener {

    /**
     * Called when a node joins the cluster.
     *
     * @param node the node that joined
     */
    void onNodeJoined(ClusterNode node);

    /**
     * Called when a node leaves the cluster.
     *
     * @param node the node that left
     */
    void onNodeLeft(ClusterNode node);

    /**
     * Called when cluster leadership changes.
     *
     * @param newLeader the new leader node
     */
    void onLeaderChanged(ClusterNode newLeader);
}
