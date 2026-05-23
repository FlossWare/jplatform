package org.flossware.jplatform.api;

/**
 * Thrown when a node fails to leave a cluster gracefully.
 * This exception indicates failures during cluster shutdown,
 * member deregistration, or cleanup operations.
 *
 * @since 1.2
 */
public class ClusterLeaveException extends PlatformException {

    /**
     * Constructs a new cluster leave exception.
     *
     * @param message the detail message
     */
    public ClusterLeaveException(String message) {
        super(message);
    }

    /**
     * Constructs a new cluster leave exception with a cause.
     *
     * @param message the detail message
     * @param cause the underlying cause
     */
    public ClusterLeaveException(String message, Throwable cause) {
        super(message, cause);
    }
}
