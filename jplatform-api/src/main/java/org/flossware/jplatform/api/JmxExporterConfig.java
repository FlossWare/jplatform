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
         */
        public Builder port(int port) {
            this.port = port;
            return this;
        }

        /**
         * Sets the JMX domain name for MBeans.
         *
         * @param domain the domain name
         * @return this builder
         */
        public Builder domain(String domain) {
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
