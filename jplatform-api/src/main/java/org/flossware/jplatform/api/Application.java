package org.flossware.jplatform.api;

/**
 * Platform-aware application interface.
 * Applications implementing this interface gain access to platform features like
 * thread pools, messaging, and resource monitoring.
 *
 * <p>Applications can also use a standard {@code main(String[] args)} method instead,
 * but will not have access to platform features.</p>
 *
 * @see ApplicationContext
 * @since 1.0
 */
public interface Application {
    /**
     * Called when the application is started by the platform.
     *
     * @param context the application context providing access to platform features
     * @throws ApplicationStartupException if the application fails to start
     * @since 1.0
     */
    void start(ApplicationContext context) throws ApplicationStartupException;

    /**
     * Called when the application is stopped by the platform.
     * Implementations should clean up resources and shut down gracefully.
     *
     * @throws ApplicationShutdownException if the application fails to stop cleanly
     * @since 1.0
     */
    void stop() throws ApplicationShutdownException;
}
