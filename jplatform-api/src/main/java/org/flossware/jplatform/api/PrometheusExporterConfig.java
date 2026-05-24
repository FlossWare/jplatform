package org.flossware.jplatform.api;

/**
 * Configuration for Prometheus metrics exporter.
 * Specifies HTTP port and path for exposing metrics in Prometheus text format.
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * PrometheusExporterConfig config = PrometheusExporterConfig.builder()
 *     .enabled(true)
 *     .port(9090)
 *     .path("/metrics")
 *     .build();
 * }</pre>
 *
 * @see MetricsExporter
 */
public class PrometheusExporterConfig {
    private final boolean enabled;
    private final int port;
    private final String path;

    private PrometheusExporterConfig(Builder builder) {
        this.enabled = builder.enabled;
        this.port = builder.port;
        this.path = builder.path;
    }

    /**
     * Checks if Prometheus export is enabled.
     *
     * @return true if enabled
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Returns the HTTP server port.
     *
     * @return the port number
     */
    public int getPort() {
        return port;
    }

    /**
     * Returns the HTTP path for metrics endpoint.
     *
     * @return the endpoint path
     */
    public String getPath() {
        return path;
    }

    /**
     * Creates a new builder for constructing Prometheus exporter configurations.
     *
     * @return a new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for PrometheusExporterConfig.
     */
    public static class Builder {
        private boolean enabled = true;
        private int port = 9090;
        private String path = "/metrics";

        /**
         * Sets whether Prometheus export is enabled.
         *
         * @param enabled true to enable
         * @return this builder
         */
        public Builder enabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        /**
         * Sets the HTTP server port.
         *
         * @param port the port number
         * @return this builder
         * @throws IllegalArgumentException if port is not in valid range (1-65535)
         */
        public Builder port(int port) {
            if (port < 1 || port > 65535) {
                throw new IllegalArgumentException(
                    "Port must be between 1 and 65535, got: " + port);
            }
            this.port = port;
            return this;
        }

        /**
         * Sets the HTTP path for the metrics endpoint.
         *
         * @param path the endpoint path
         * @return this builder
         * @throws IllegalArgumentException if path is null, empty, or doesn't start with '/'
         */
        public Builder path(String path) {
            if (path == null || path.trim().isEmpty()) {
                throw new IllegalArgumentException(
                    "Metrics path cannot be null or empty");
            }
            if (!path.startsWith("/")) {
                throw new IllegalArgumentException(
                    "Metrics path must start with '/', got: " + path);
            }
            this.path = path;
            return this;
        }

        /**
         * Builds the PrometheusExporterConfig instance.
         *
         * @return a new PrometheusExporterConfig
         */
        public PrometheusExporterConfig build() {
            return new PrometheusExporterConfig(this);
        }
    }
}
