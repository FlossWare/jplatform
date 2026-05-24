package org.flossware.jplatform.api;

/**
 * Configuration for JMX metrics exporter.
 * Specifies RMI port and MBean domain for exposing application metrics via JMX.
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * JmxExporterConfig config = JmxExporterConfig.builder()
 *     .enabled(true)
 *     .port(9999)
 *     .domain("org.flossware.jplatform")
 *     .build();
 * }</pre>
 *
 * @see MetricsExporter
 */
public class JmxExporterConfig {
    private final boolean enabled;
    private final int port;
    private final String domain;

    private JmxExporterConfig(Builder builder) {
        this.enabled = builder.enabled;
        this.port = builder.port;
        this.domain = builder.domain;
    }

    /**
     * Checks if JMX export is enabled.
     *
     * @return true if enabled
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Returns the RMI registry port.
     *
     * @return the port number
     */
    public int getPort() {
        return port;
    }

    /**
     * Returns the JMX domain name for MBeans.
     *
     * @return the domain name
     */
    public String getDomain() {
        return domain;
    }

    /**
     * Creates a new builder for constructing JMX exporter configurations.
     *
     * @return a new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for JmxExporterConfig.
     */
    public static class Builder {
        private boolean enabled = true;
        private int port = 9999;
        private String domain = "org.flossware.jplatform";

        /**
         * Sets whether JMX export is enabled.
         *
         * @param enabled true to enable
         * @return this builder
         */
        public Builder enabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        /**
         * Sets the RMI registry port.
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
         * Sets the JMX domain name for MBeans.
         *
         * @param domain the domain name
         * @return this builder
         * @throws IllegalArgumentException if domain is null or empty
         */
        public Builder domain(String domain) {
            if (domain == null || domain.trim().isEmpty()) {
                throw new IllegalArgumentException(
                    "JMX domain cannot be null or empty");
            }
            this.domain = domain;
            return this;
        }

        /**
         * Builds the JmxExporterConfig instance.
         *
         * @return a new JmxExporterConfig
         */
        public JmxExporterConfig build() {
            return new JmxExporterConfig(this);
        }
    }
}
