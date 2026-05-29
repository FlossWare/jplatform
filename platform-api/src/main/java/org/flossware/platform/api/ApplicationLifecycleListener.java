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
 * Observer interface for application lifecycle events.
 *
 * <p>Listeners are notified of key events in an application's lifecycle, allowing external systems
 * to react to changes without modifying application code.
 *
 * <p><b>Thread Safety:</b> Listener methods may be called from different threads. Implementations
 * must be thread-safe.
 *
 * <p><b>Error Handling:</b> Exceptions thrown by listener methods are logged but do not affect the
 * lifecycle operation. Listeners should handle errors internally.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * public class MetricsListener implements ApplicationLifecycleListener {
 *   @Override
 *   public void onStarted(String applicationId) {
 *     metrics.increment("app.started", "app", applicationId);
 *   }
 *
 *   @Override
 *   public void onStopped(String applicationId, int exitCode) {
 *     metrics.increment("app.stopped", "app", applicationId, "exitCode", String.valueOf(exitCode));
 *   }
 *
 *   // ... other methods
 * }
 *
 * applicationManager.addLifecycleListener(new MetricsListener());
 * }</pre>
 *
 * @since 2.4
 */
public interface ApplicationLifecycleListener {

  /**
   * Called when an application is deployed to the platform.
   *
   * <p>At this point resources are allocated but the application is not yet started.
   *
   * @param applicationId the application identifier
   * @param descriptor the application descriptor containing configuration
   */
  void onDeployed(String applicationId, ApplicationDescriptor descriptor);

  /**
   * Called when an application transitions to RUNNING state.
   *
   * @param applicationId the application identifier
   */
  void onStarted(String applicationId);

  /**
   * Called when an application transitions to STOPPED or FAILED state.
   *
   * @param applicationId the application identifier
   * @param exitCode the exit code (0 for normal shutdown, non-zero for errors)
   */
  void onStopped(String applicationId, int exitCode);

  /**
   * Called when an application is automatically restarted by the RestartManager.
   *
   * @param applicationId the application identifier
   * @param attemptNumber the restart attempt number (1-based)
   */
  void onRestarted(String applicationId, int attemptNumber);

  /**
   * Called when an application's health status changes.
   *
   * <p>This is triggered by the HealthChecker when health checks detect a status change.
   *
   * @param applicationId the application identifier
   * @param oldStatus the previous health status
   * @param newStatus the new health status
   */
  void onHealthChanged(String applicationId, HealthStatus oldStatus, HealthStatus newStatus);

  /**
   * Called when an application is undeployed from the platform.
   *
   * <p>Resources are released and the application context is destroyed.
   *
   * @param applicationId the application identifier
   */
  void onUndeployed(String applicationId);
}
