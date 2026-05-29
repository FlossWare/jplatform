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

/**
 * Health check interface for applications.
 *
 * <p>Applications can implement this interface to provide custom health checks that are
 * periodically executed by the platform. If the health check fails, the platform can take action
 * based on configured policies (log, restart, etc.).
 *
 * <p>Example implementation:
 *
 * <pre>{@code
 * public class MyApp implements Application, HealthCheck {
 *     private Database db;
 *
 *     {@literal @}Override
 *     public HealthStatus checkHealth() {
 *         try {
 *             db.ping();
 *             return HealthStatus.healthy("Database connection OK");
 *         } catch (Exception e) {
 *             return HealthStatus.unhealthy("Database unreachable: " + e.getMessage());
 *         }
 *     }
 * }
 * }</pre>
 *
 * @since 2.3
 * @see HealthStatus
 */
public interface HealthCheck {

  /**
   * Performs a health check for this application.
   *
   * <p>This method should return quickly (within a few seconds) and should not block indefinitely.
   * The platform will call this method periodically based on the configured health check interval.
   *
   * <p>Return {@link HealthStatus#healthy(String)} if the application is functioning normally, or
   * {@link HealthStatus#unhealthy(String)} if there are issues.
   *
   * @return the current health status of the application
   */
  HealthStatus checkHealth();
}
