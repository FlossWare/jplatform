package org.flossware.jplatform.api;

/**
 * Exports application metrics to external monitoring systems.
 * Implementations provide integration with JMX, Prometheus, or other monitoring tools.
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * JmxExporterConfig config = JmxExporterConfig.builder()
 *     .port(9999)
 *     .domain("org.flossware.jplatform")
 *     .build();
 *
 * MetricsExporter exporter = new JmxMetricsExporter(config);
 * exporter.start();
 *
 * // Register applications
 * exporter.registerApplication("my-app", applicationContext);
 * }</pre>
 *
 * @see JmxExporterConfig
 * @see PrometheusExporterConfig
 */
public interface MetricsExporter extends AutoCloseable {

    /**
     * Starts the metrics exporter.
     *
     * @throws Exception if the exporter cannot be started
     */
    void start() throws Exception;

    /**
     * Stops the metrics exporter.
     *
     * @throws Exception if the exporter cannot be stopped
     */
    void stop() throws Exception;

    /**
     * Registers an application for metrics export.
     *
     * @param applicationId the application identifier
     * @param context the application context containing metrics
     */
    void registerApplication(String applicationId, ApplicationContext context);

    /**
     * Unregisters an application from metrics export.
     *
     * @param applicationId the application identifier
     */
    void unregisterApplication(String applicationId);

    /**
     * Checks if the exporter is currently running.
     *
     * @return true if running, false otherwise
     */
    boolean isRunning();
}
