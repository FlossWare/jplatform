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

package org.flossware.platform.api;

import java.time.Instant;
import java.util.Objects;

/**
 * Represents the health status of an application.
 *
 * <p>Health status consists of a boolean healthy/unhealthy indicator, an optional message, and the
 * timestamp when the check was performed.
 *
 * @since 2.3
 * @see HealthCheck
 */
public final class HealthStatus {

  private final boolean healthy;
  private final String message;
  private final Instant timestamp;

  private HealthStatus(boolean healthy, String message) {
    this.healthy = healthy;
    this.message = message;
    this.timestamp = Instant.now();
  }

  /**
   * Creates a healthy status with the given message.
   *
   * @param message description of the healthy state
   * @return a healthy status
   */
  public static HealthStatus healthy(String message) {
    return new HealthStatus(true, message);
  }

  /**
   * Creates an unhealthy status with the given message.
   *
   * @param message description of what is unhealthy
   * @return an unhealthy status
   */
  public static HealthStatus unhealthy(String message) {
    return new HealthStatus(false, message);
  }

  /**
   * Returns whether the application is healthy.
   *
   * @return true if healthy, false otherwise
   */
  public boolean isHealthy() {
    return healthy;
  }

  /**
   * Returns the health status message.
   *
   * @return the message, may be null
   */
  public String getMessage() {
    return message;
  }

  /**
   * Returns when this health check was performed.
   *
   * @return the timestamp
   */
  public Instant getTimestamp() {
    return timestamp;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    HealthStatus that = (HealthStatus) o;
    return healthy == that.healthy && Objects.equals(message, that.message);
  }

  @Override
  public int hashCode() {
    return Objects.hash(healthy, message);
  }

  @Override
  public String toString() {
    return "HealthStatus{"
        + "healthy="
        + healthy
        + ", message='"
        + message
        + '\''
        + ", timestamp="
        + timestamp
        + '}';
  }
}
