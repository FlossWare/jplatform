package org.flossware.jplatform.api;

/**
 * HTTP API server for remote platform management.
 * Provides REST endpoints for deploying, starting, stopping applications,
 * and retrieving platform status and metrics.
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * ApiServerConfig config = ApiServerConfig.builder()
 *     .port(8080)
 *     .bindAddress("0.0.0.0")
 *     .enableAuth(true)
 *     .apiKey("secret-key")
 *     .build();
 *
 * PlatformApiServer server = new JdkHttpApiServer(config, applicationManager);
 * server.start();
 * }</pre>
 *
 * @see ApiServerConfig
 * @since 1.0
 */
public interface PlatformApiServer extends AutoCloseable {

    /**
     * Starts the HTTP server.
     *
     * @throws ServerStartupException if the server cannot be started
     * @since 1.0
     */
    void start() throws ServerStartupException;

    /**
     * Stops the HTTP server.
     *
     * @throws ServerShutdownException if the server cannot be stopped
     * @since 1.0
     */
    void stop() throws ServerShutdownException;

    /**
     * Returns the port the server is listening on.
     *
     * @return the server port
     */
    int getPort();

    /**
     * Checks if the server is currently running.
     *
     * @return true if running, false otherwise
     */
    boolean isRunning();
}
