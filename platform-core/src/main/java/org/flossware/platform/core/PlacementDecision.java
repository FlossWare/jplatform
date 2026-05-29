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

import java.util.Objects;

/**
 * Represents a workload placement decision made by the scheduler.
 *
 * <p>Contains the selected execution backend and the rationale for the decision. This information
 * is used for observability, debugging, and policy validation.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * PlacementDecision decision = scheduler.scheduleWorkload(descriptor);
 * System.out.println("Backend: " + decision.getBackend());
 * System.out.println("Reason: " + decision.getReason());
 *
 * if (decision.getBackend() == ExecutionBackend.IN_JVM) {
 *     // Deploy to in-JVM classloader
 * } else if (decision.getBackend() == ExecutionBackend.CONTAINER) {
 *     // Deploy to container runtime
 * }
 * }</pre>
 *
 * @since 2.2
 * @see WorkloadPlacementScheduler
 * @see ExecutionBackend
 */
public final class PlacementDecision {

  private final ExecutionBackend backend;
  private final String reason;
  private final long timestamp;
  private final String applicationId;

  /**
   * Creates a placement decision with current timestamp.
   *
   * @param backend the selected execution backend
   * @param reason human-readable explanation for the decision
   * @throws NullPointerException if backend or reason is null
   */
  public PlacementDecision(ExecutionBackend backend, String reason) {
    this(null, backend, reason, System.currentTimeMillis());
  }

  /**
   * Creates a placement decision for a specific application.
   *
   * @param applicationId the application identifier
   * @param backend the selected execution backend
   * @param reason human-readable explanation for the decision
   * @throws NullPointerException if backend or reason is null
   */
  public PlacementDecision(String applicationId, ExecutionBackend backend, String reason) {
    this(applicationId, backend, reason, System.currentTimeMillis());
  }

  /**
   * Creates a placement decision with explicit timestamp.
   *
   * @param applicationId the application identifier (may be null)
   * @param backend the selected execution backend
   * @param reason human-readable explanation for the decision
   * @param timestamp the decision timestamp (milliseconds since epoch)
   * @throws NullPointerException if backend or reason is null
   * @throws IllegalArgumentException if timestamp is negative
   */
  public PlacementDecision(
      String applicationId, ExecutionBackend backend, String reason, long timestamp) {
    this.applicationId = applicationId;
    this.backend = Objects.requireNonNull(backend, "backend cannot be null");
    this.reason = Objects.requireNonNull(reason, "reason cannot be null");

    if (timestamp < 0) {
      throw new IllegalArgumentException("timestamp cannot be negative: " + timestamp);
    }
    this.timestamp = timestamp;
  }

  /**
   * Returns the application identifier for this decision.
   *
   * @return the application ID, or null if not specified
   */
  public String getApplicationId() {
    return applicationId;
  }

  /**
   * Returns the selected execution backend.
   *
   * @return the backend where the workload should run
   */
  public ExecutionBackend getBackend() {
    return backend;
  }

  /**
   * Returns the human-readable reason for this decision.
   *
   * <p>The reason explains why this particular backend was selected, useful for debugging,
   * auditing, and policy validation.
   *
   * <p>Example reasons:
   *
   * <ul>
   *   <li>"Lightweight Java app (512MB memory)"
   *   <li>"Heavy Java app requires container isolation"
   *   <li>"Non-Java workload requires VM"
   *   <li>"High replica count (50) requires Kubernetes"
   *   <li>"Policy override: financial apps require VM isolation"
   * </ul>
   *
   * @return the decision rationale
   */
  public String getReason() {
    return reason;
  }

  /**
   * Returns the timestamp when this decision was made.
   *
   * @return milliseconds since epoch
   */
  public long getTimestamp() {
    return timestamp;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    PlacementDecision that = (PlacementDecision) o;
    return timestamp == that.timestamp
        && backend == that.backend
        && Objects.equals(reason, that.reason)
        && Objects.equals(applicationId, that.applicationId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(backend, reason, timestamp, applicationId);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("PlacementDecision{");
    if (applicationId != null) {
      sb.append("app=").append(applicationId).append(", ");
    }
    sb.append("backend=")
        .append(backend.getId())
        .append(", reason='")
        .append(reason)
        .append('\'')
        .append(", timestamp=")
        .append(timestamp)
        .append('}');
    return sb.toString();
  }
}
