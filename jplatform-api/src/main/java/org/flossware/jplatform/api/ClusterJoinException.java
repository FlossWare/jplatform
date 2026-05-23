package org.flossware.jplatform.api;

/**
 * Thrown when a node fails to join a cluster.
 * This exception indicates failures during cluster initialization,
 * seed node connection, or network configuration issues.
 *
 * @since 1.2
 */
public class ClusterJoinException extends PlatformException {

    private final String clusterName;

    /**
     * Constructs a new cluster join exception.
     *
     * @param clusterName the name of the cluster
     * @param message the detail message
     */
    public ClusterJoinException(String clusterName, String message) {
        super(message);
        this.clusterName = clusterName;
    }

    /**
     * Constructs a new cluster join exception with a cause.
     *
     * @param clusterName the name of the cluster
     * @param message the detail message
     * @param cause the underlying cause
     */
    public ClusterJoinException(String clusterName, String message, Throwable cause) {
        super(message, cause);
        this.clusterName = clusterName;
    }

    /**
     * Returns the name of the cluster.
     *
     * @return the cluster name
     */
    public String getClusterName() {
        return clusterName;
    }
}
