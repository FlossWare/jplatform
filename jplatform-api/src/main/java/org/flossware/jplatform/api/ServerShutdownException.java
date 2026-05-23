package org.flossware.jplatform.api;

/**
 * Thrown when a platform server (HTTP API, web console) fails to stop gracefully.
 * This exception indicates failures during server shutdown or resource cleanup.
 *
 * @since 1.2
 */
public class ServerShutdownException extends PlatformException {

    /**
     * Constructs a new server shutdown exception.
     *
     * @param message the detail message
     */
    public ServerShutdownException(String message) {
        super(message);
    }

    /**
     * Constructs a new server shutdown exception with a cause.
     *
     * @param message the detail message
     * @param cause the underlying cause
     */
    public ServerShutdownException(String message, Throwable cause) {
        super(message, cause);
    }
}
