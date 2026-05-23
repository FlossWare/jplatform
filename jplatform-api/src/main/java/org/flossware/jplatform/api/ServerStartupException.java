package org.flossware.jplatform.api;

/**
 * Thrown when a platform server (HTTP API, web console) fails to start.
 * This exception indicates failures during server initialization, port binding,
 * or other startup-related issues.
 *
 * @since 1.2
 */
public class ServerStartupException extends PlatformException {

    private final int port;

    /**
     * Constructs a new server startup exception.
     *
     * @param message the detail message
     * @param port the port the server was attempting to bind to
     */
    public ServerStartupException(String message, int port) {
        super(message);
        this.port = port;
    }

    /**
     * Constructs a new server startup exception with a cause.
     *
     * @param message the detail message
     * @param port the port the server was attempting to bind to
     * @param cause the underlying cause
     */
    public ServerStartupException(String message, int port, Throwable cause) {
        super(message, cause);
        this.port = port;
    }

    /**
     * Returns the port the server was attempting to bind to.
     *
     * @return the port number
     */
    public int getPort() {
        return port;
    }
}
