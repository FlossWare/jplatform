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
 */
public interface PlatformApiServer extends AutoCloseable {

    /**
     * Starts the HTTP server.
     *
     * @throws Exception if the server cannot be started
     */
    void start() throws Exception;

    /**
     * Stops the HTTP server.
     *
     * @throws Exception if the server cannot be stopped
     */
    void stop() throws Exception;

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
