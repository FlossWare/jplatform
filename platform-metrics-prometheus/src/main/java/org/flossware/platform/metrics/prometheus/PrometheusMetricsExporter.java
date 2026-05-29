/*
 * Copyright (C) 2024-2026 FlossWare
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.flossware.platform.metrics.prometheus;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.flossware.platform.api.ApplicationContext;
import org.flossware.platform.api.MetricsExporter;
import org.flossware.platform.api.PrometheusExporterConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

/**
 * Prometheus metrics exporter implementation. Exposes application metrics via HTTP endpoint in
 * Prometheus text exposition format.
 *
 * <p>This exporter:
 *
 * <ul>
 *   <li>Starts an embedded HTTP server on the configured port
 *   <li>Serves metrics at the configured path (default: /metrics)
 *   <li>Collects metrics from all registered applications
 *   <li>Formats metrics in Prometheus text format
 *   <li>Thread-safe for concurrent registrations and scrapes
 * </ul>
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * PrometheusExporterConfig config = PrometheusExporterConfig.builder()
 *     .port(9090)
 *     .path("/metrics")
 *     .build();
 *
 * PrometheusMetricsExporter exporter = new PrometheusMetricsExporter(config);
 * exporter.start();
 *
 * // Register applications
 * exporter.registerApplication("app1", context1);
 * exporter.registerApplication("app2", context2);
 *
 * // Metrics available at http://localhost:9090/metrics
 *
 * // Cleanup
 * exporter.stop();
 * }</pre>
 *
 * @see MetricsExporter
 * @see PrometheusExporterConfig
 * @see ApplicationMetricsCollector
 */
public class PrometheusMetricsExporter implements MetricsExporter {

  private static final Logger LOGGER = LoggerFactory.getLogger(PrometheusMetricsExporter.class);

  private final PrometheusExporterConfig config;
  private final Map<String, ApplicationContext> applications;
  private HttpServer server;
  private volatile boolean running;

  /**
   * Constructs a new Prometheus metrics exporter with the specified configuration.
   *
   * @param config the exporter configuration specifying port and path
   * @throws IllegalArgumentException if config is null
   */
  public PrometheusMetricsExporter(PrometheusExporterConfig config) {
    if (config == null) {
      throw new IllegalArgumentException("PrometheusExporterConfig cannot be null");
    }
    this.config = config;
    this.applications = new ConcurrentHashMap<>();
    this.running = false;
  }

  /**
   * Starts the HTTP server to expose metrics. Creates an HTTP server on the configured port and
   * path.
   *
   * @throws IOException if the server cannot be started (e.g., port already in use)
   */
  @Override
  public void start() throws IOException {
    if (running) {
      LOGGER.warn("Prometheus exporter already running on port {}", config.getPort());
      return;
    }

    try {
      LOGGER.info(
          "Starting Prometheus metrics exporter on port {} path {}",
          config.getPort(),
          config.getPath());

      // Create HTTP server
      server = HttpServer.create(new InetSocketAddress(config.getPort()), 0);

      // Register metrics endpoint handler
      server.createContext(config.getPath(), this::handleMetrics);

      // Use default executor (creates daemon threads)
      server.setExecutor(null);

      // Start the server
      server.start();
      running = true;

      LOGGER.info("Prometheus metrics exporter started successfully");

    } catch (IOException e) {
      LOGGER.error("Failed to start Prometheus exporter: {}", e.getMessage(), e);
      throw e;
    }
  }

  /**
   * Stops the HTTP server and releases resources. Waits up to 5 seconds for ongoing requests to
   * complete.
   */
  @Override
  public void stop() {
    if (!running) {
      LOGGER.warn("Prometheus exporter is not running");
      return;
    }

    try {
      LOGGER.info("Stopping Prometheus metrics exporter");

      if (server != null) {
        // Stop with 5 second delay to allow ongoing requests to complete
        server.stop(5);
        server = null;
      }

      running = false;
      LOGGER.info("Prometheus metrics exporter stopped");

    } catch (Exception e) {
      LOGGER.error("Error stopping Prometheus exporter: {}", e.getMessage(), e);
    }
  }

  /**
   * Registers an application for metrics export. Metrics from this application will be included in
   * future scrapes.
   *
   * @param applicationId the unique application identifier
   * @param context the application context containing metrics
   */
  @Override
  public void registerApplication(String applicationId, ApplicationContext context) {
    LOGGER.info("Registering application {} for Prometheus export", applicationId);
    applications.put(applicationId, context);
  }

  /**
   * Unregisters an application from metrics export. Metrics from this application will no longer be
   * included in scrapes.
   *
   * @param applicationId the application identifier to unregister
   */
  @Override
  public void unregisterApplication(String applicationId) {
    LOGGER.info("Unregistering application {} from Prometheus export", applicationId);
    applications.remove(applicationId);
  }

  /**
   * Checks if the exporter is currently running.
   *
   * @return true if the HTTP server is running, false otherwise
   */
  @Override
  public boolean isRunning() {
    return running;
  }

  /** Closes the exporter and releases resources. Calls stop() to shut down the HTTP server. */
  @Override
  public void close() {
    stop();
  }

  /**
   * Handles HTTP requests to the metrics endpoint. Collects metrics from all registered
   * applications and returns them in Prometheus format.
   *
   * @param exchange the HTTP exchange representing the request and response
   */
  private void handleMetrics(HttpExchange exchange) {
    try {
      LOGGER.debug("Handling metrics request from {}", exchange.getRemoteAddress());

      // Collect metrics from all registered applications
      StringBuilder sb = new StringBuilder();

      for (Map.Entry<String, ApplicationContext> entry : applications.entrySet()) {
        String appId = entry.getKey();
        ApplicationContext context = entry.getValue();

        try {
          String metrics = ApplicationMetricsCollector.collectMetrics(context);
          sb.append(metrics);
        } catch (Exception e) {
          LOGGER.error(
              "Failed to collect metrics for application {}: {}", appId, e.getMessage(), e);
        }
      }

      // Send response
      byte[] response = sb.toString().getBytes("UTF-8");
      exchange.getResponseHeaders().set("Content-Type", "text/plain; version=0.0.4; charset=utf-8");
      exchange.sendResponseHeaders(200, response.length);

      try (OutputStream os = exchange.getResponseBody()) {
        os.write(response);
      }

      LOGGER.debug("Metrics request completed, sent {} bytes", response.length);

    } catch (Exception e) {
      LOGGER.error("Error handling metrics request: {}", e.getMessage(), e);
      try {
        // Send error response
        byte[] errorResponse = "Internal server error".getBytes("UTF-8");
        exchange.sendResponseHeaders(500, errorResponse.length);
        try (OutputStream os = exchange.getResponseBody()) {
          os.write(errorResponse);
        }
      } catch (IOException ioException) {
        LOGGER.error("Failed to send error response: {}", ioException.getMessage());
      }
    } finally {
      exchange.close();
    }
  }
}
