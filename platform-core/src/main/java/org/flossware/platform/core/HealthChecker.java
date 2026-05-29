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

package org.flossware.platform.core;

import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URI;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.flossware.platform.api.HealthCheck;
import org.flossware.platform.api.HealthStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Executes health checks for applications.
 *
 * <p>Supports two types of health checks:
 *
 * <ul>
 *   <li><b>Application-based</b>: Calls the {@link HealthCheck#checkHealth()} method if the
 *       application implements {@link HealthCheck}
 *   <li><b>HTTP-based</b>: Sends HTTP GET requests to a configured endpoint
 *   <li><b>TCP-based</b>: Attempts to connect to a configured TCP port
 * </ul>
 *
 * <p>Health checks are executed periodically on a background thread. If a check fails, the
 * configured action is taken (log, restart, etc.).
 *
 * @since 2.3
 */
public class HealthChecker {

  private static final Logger LOGGER = LoggerFactory.getLogger(HealthChecker.class);

  private final ScheduledExecutorService scheduler;
  private final HealthCheckConfig config;
  private final ApplicationContextImpl context;
  private final ApplicationManager applicationManager;
  private volatile HealthStatus lastStatus;
  private volatile int consecutiveFailures;

  /**
   * Creates a health checker.
   *
   * @param context the application context
   * @param config the health check configuration
   * @param applicationManager the application manager for lifecycle notifications
   */
  public HealthChecker(
      ApplicationContextImpl context,
      HealthCheckConfig config,
      ApplicationManager applicationManager) {
    this.context = Objects.requireNonNull(context, "context cannot be null");
    this.config = Objects.requireNonNull(config, "config cannot be null");
    this.applicationManager =
        Objects.requireNonNull(applicationManager, "applicationManager cannot be null");
    this.scheduler =
        Executors.newSingleThreadScheduledExecutor(
            r -> {
              Thread t = new Thread(r, "health-check-" + context.getApplicationId());
              t.setDaemon(true);
              return t;
            });
    this.lastStatus = HealthStatus.healthy("Not yet checked");
    this.consecutiveFailures = 0;
  }

  /**
   * Starts periodic health checking.
   *
   * <p>The first check is performed after {@code initialDelaySeconds}, then every {@code
   * intervalSeconds}.
   */
  public void start() {
    scheduler.scheduleAtFixedRate(
        this::performCheck,
        config.getInitialDelaySeconds(),
        config.getIntervalSeconds(),
        TimeUnit.SECONDS);
    LOGGER.info(
        "[{}] Health checks started: interval={}s, timeout={}s, type={}",
        context.getApplicationId(),
        config.getIntervalSeconds(),
        config.getTimeoutSeconds(),
        config.getType());
  }

  /** Stops health checking and shuts down the scheduler. */
  public void stop() {
    scheduler.shutdownNow();
    LOGGER.info("[{}] Health checks stopped", context.getApplicationId());
  }

  /** Performs a single health check. */
  private void performCheck() {
    try {
      HealthStatus status;

      switch (config.getType()) {
        case APPLICATION:
          status = checkApplication();
          break;
        case HTTP:
          status = checkHttp();
          break;
        case TCP:
          status = checkTcp();
          break;
        default:
          status = HealthStatus.unhealthy("Unknown health check type: " + config.getType());
      }

      handleCheckResult(status);
    } catch (Exception e) {
      LOGGER.error("[{}] Health check threw exception", context.getApplicationId(), e);
      handleCheckResult(HealthStatus.unhealthy("Check failed: " + e.getMessage()));
    }
  }

  private HealthStatus checkApplication() {
    Object instance = context.getApplicationInstance();
    if (instance instanceof HealthCheck) {
      return ((HealthCheck) instance).checkHealth();
    } else {
      return HealthStatus.unhealthy("Application does not implement HealthCheck interface");
    }
  }

  private HealthStatus checkHttp() {
    try {
      URI uri = URI.create(config.getHttpUrl());
      HttpURLConnection conn = (HttpURLConnection) uri.toURL().openConnection();
      conn.setRequestMethod("GET");
      conn.setConnectTimeout((int) TimeUnit.SECONDS.toMillis(config.getTimeoutSeconds()));
      conn.setReadTimeout((int) TimeUnit.SECONDS.toMillis(config.getTimeoutSeconds()));

      int responseCode = conn.getResponseCode();
      conn.disconnect();

      if (responseCode >= 200 && responseCode < 300) {
        return HealthStatus.healthy("HTTP " + responseCode);
      } else {
        return HealthStatus.unhealthy("HTTP " + responseCode);
      }
    } catch (Exception e) {
      return HealthStatus.unhealthy("HTTP check failed: " + e.getMessage());
    }
  }

  private HealthStatus checkTcp() {
    try (Socket socket = new Socket()) {
      socket.connect(
          new java.net.InetSocketAddress(config.getTcpHost(), config.getTcpPort()),
          (int) TimeUnit.SECONDS.toMillis(config.getTimeoutSeconds()));
      return HealthStatus.healthy("TCP connection successful");
    } catch (Exception e) {
      return HealthStatus.unhealthy("TCP connection failed: " + e.getMessage());
    }
  }

  private void handleCheckResult(HealthStatus status) {
    HealthStatus previousStatus = lastStatus;
    lastStatus = status;

    // Notify listeners if status changed
    if (previousStatus.isHealthy() != status.isHealthy()) {
      applicationManager.notifyListeners(
          listener -> listener.onHealthChanged(context.getApplicationId(), previousStatus, status));
    }

    if (status.isHealthy()) {
      if (consecutiveFailures > 0) {
        LOGGER.info(
            "[{}] Health check recovered after {} failures",
            context.getApplicationId(),
            consecutiveFailures);
      }
      consecutiveFailures = 0;
    } else {
      consecutiveFailures++;
      LOGGER.warn(
          "[{}] Health check failed ({}/{}): {}",
          context.getApplicationId(),
          consecutiveFailures,
          config.getFailureThreshold(),
          status.getMessage());

      if (consecutiveFailures >= config.getFailureThreshold()) {
        handleFailureThresholdExceeded();
      }
    }
  }

  private void handleFailureThresholdExceeded() {
    LOGGER.error(
        "[{}] Health check failure threshold exceeded: {} consecutive failures",
        context.getApplicationId(),
        consecutiveFailures);

    // Action could be: RESTART, SHUTDOWN, NOTIFY, etc.
    // For now, just log - actual action handling can be added later
    // based on config.getOnFailureAction()
  }

  /**
   * Returns the most recent health status.
   *
   * @return the last health status
   */
  public HealthStatus getLastStatus() {
    return lastStatus;
  }

  /**
   * Returns the number of consecutive failures.
   *
   * @return consecutive failure count
   */
  public int getConsecutiveFailures() {
    return consecutiveFailures;
  }

  /** Configuration for health checks. */
  public static class HealthCheckConfig {
    private final HealthCheckType type;
    private final long intervalSeconds;
    private final long initialDelaySeconds;
    private final long timeoutSeconds;
    private final int failureThreshold;
    private final String httpUrl;
    private final String tcpHost;
    private final int tcpPort;

    private HealthCheckConfig(Builder builder) {
      this.type = builder.type;
      this.intervalSeconds = builder.intervalSeconds;
      this.initialDelaySeconds = builder.initialDelaySeconds;
      this.timeoutSeconds = builder.timeoutSeconds;
      this.failureThreshold = builder.failureThreshold;
      this.httpUrl = builder.httpUrl;
      this.tcpHost = builder.tcpHost;
      this.tcpPort = builder.tcpPort;
    }

    public HealthCheckType getType() {
      return type;
    }

    public long getIntervalSeconds() {
      return intervalSeconds;
    }

    public long getInitialDelaySeconds() {
      return initialDelaySeconds;
    }

    public long getTimeoutSeconds() {
      return timeoutSeconds;
    }

    public int getFailureThreshold() {
      return failureThreshold;
    }

    public String getHttpUrl() {
      return httpUrl;
    }

    public String getTcpHost() {
      return tcpHost;
    }

    public int getTcpPort() {
      return tcpPort;
    }

    public static Builder builder() {
      return new Builder();
    }

    /** Builder for HealthCheckConfig. */
    public static class Builder {
      private HealthCheckType type = HealthCheckType.APPLICATION;
      private long intervalSeconds = 30;
      private long initialDelaySeconds = 10;
      private long timeoutSeconds = 5;
      private int failureThreshold = 3;
      private String httpUrl;
      private String tcpHost;
      private int tcpPort;

      public Builder type(HealthCheckType type) {
        this.type = type;
        return this;
      }

      public Builder intervalSeconds(long intervalSeconds) {
        this.intervalSeconds = intervalSeconds;
        return this;
      }

      public Builder initialDelaySeconds(long initialDelaySeconds) {
        this.initialDelaySeconds = initialDelaySeconds;
        return this;
      }

      public Builder timeoutSeconds(long timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
        return this;
      }

      public Builder failureThreshold(int failureThreshold) {
        this.failureThreshold = failureThreshold;
        return this;
      }

      public Builder httpUrl(String httpUrl) {
        this.httpUrl = httpUrl;
        return this;
      }

      public Builder tcpHost(String tcpHost) {
        this.tcpHost = tcpHost;
        return this;
      }

      public Builder tcpPort(int tcpPort) {
        this.tcpPort = tcpPort;
        return this;
      }

      public HealthCheckConfig build() {
        return new HealthCheckConfig(this);
      }
    }
  }

  /** Type of health check. */
  public enum HealthCheckType {
    /** Call the application's HealthCheck.checkHealth() method. */
    APPLICATION,
    /** Send HTTP GET request to a URL. */
    HTTP,
    /** Attempt TCP connection to a port. */
    TCP
  }
}
