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

/**
 * Execution backend types for workload placement.
 *
 * <p>Defines the available execution environments where applications can run. The {@link
 * WorkloadPlacementScheduler} uses these backends to determine the optimal placement for each
 * workload.
 *
 * <p>Backend characteristics:
 *
 * <ul>
 *   <li>{@link #IN_JVM} - Fastest startup, lowest overhead, Java-only, shared JVM
 *   <li>{@link #CONTAINER} - Process isolation, portable, multi-language via images
 *   <li>{@link #VIRTUAL_MACHINE} - Kernel isolation, full OS, highest overhead
 *   <li>{@link #KUBERNETES} - Cluster orchestration, massive scale, external dependency
 * </ul>
 *
 * @since 2.2
 * @see WorkloadPlacementScheduler
 * @see PlacementDecision
 */
public enum ExecutionBackend {

  /**
   * In-JVM execution using isolated classloaders.
   *
   * <p><b>Characteristics:</b>
   *
   * <ul>
   *   <li>Startup: &lt;100ms
   *   <li>Overhead: ~10MB per app
   *   <li>Isolation: ClassLoader, thread pool, security policy
   *   <li>Supported: Java applications only
   *   <li>Best for: Lightweight Java apps, microservices, dev environments
   * </ul>
   *
   * <p><b>Limitations:</b>
   *
   * <ul>
   *   <li>Shared JVM heap (requires resource quotas)
   *   <li>No kernel-level isolation
   *   <li>Java-only (no native binaries or polyglot)
   * </ul>
   */
  IN_JVM("in-jvm", "Isolated ClassLoader", true, false, false),

  /**
   * Container execution using Docker, Podman, or LXC.
   *
   * <p><b>Characteristics:</b>
   *
   * <ul>
   *   <li>Startup: 1-3 seconds
   *   <li>Overhead: 100-500MB per container
   *   <li>Isolation: Process namespace, cgroups, network
   *   <li>Supported: Any containerized application
   *   <li>Best for: Heavy Java apps, polyglot services, portable deployments
   * </ul>
   *
   * <p><b>Limitations:</b>
   *
   * <ul>
   *   <li>Higher memory overhead than in-JVM
   *   <li>Requires container runtime (Docker/Podman/LXC)
   *   <li>Slower startup than in-JVM
   * </ul>
   */
  CONTAINER("container", "Docker/Podman/LXC Container", true, true, false),

  /**
   * Virtual machine execution using KVM/QEMU via libvirt.
   *
   * <p><b>Characteristics:</b>
   *
   * <ul>
   *   <li>Startup: 10-30 seconds
   *   <li>Overhead: 512MB+ per VM
   *   <li>Isolation: Full kernel isolation, separate OS
   *   <li>Supported: Any OS, native binaries, legacy apps
   *   <li>Best for: Non-Java apps, kernel-level isolation, Windows/legacy
   * </ul>
   *
   * <p><b>Limitations:</b>
   *
   * <ul>
   *   <li>Highest overhead (full OS per VM)
   *   <li>Slowest startup time
   *   <li>Requires KVM/libvirt infrastructure
   * </ul>
   */
  VIRTUAL_MACHINE("vm", "KVM/QEMU Virtual Machine", false, true, true),

  /**
   * Kubernetes cluster execution (future support).
   *
   * <p><b>Characteristics:</b>
   *
   * <ul>
   *   <li>Startup: 5-10 seconds
   *   <li>Overhead: Varies by deployment
   *   <li>Isolation: Pod/namespace isolation
   *   <li>Supported: Any containerized app, KubeVirt VMs
   *   <li>Best for: Massive scale (50+ replicas), multi-cloud, polyglot
   * </ul>
   *
   * <p><b>Limitations:</b>
   *
   * <ul>
   *   <li>Requires external Kubernetes cluster
   *   <li>Higher operational complexity
   *   <li>Network latency for cross-cluster communication
   * </ul>
   *
   * <p><b>Note:</b> Kubernetes backend is not yet implemented. Will be added in a future release.
   */
  KUBERNETES("kubernetes", "Kubernetes Cluster", true, true, false);

  private final String id;
  private final String displayName;
  private final boolean supportsJava;
  private final boolean supportsNative;
  private final boolean requiresKernelIsolation;

  /**
   * Constructs an execution backend.
   *
   * @param id unique identifier for the backend
   * @param displayName human-readable name
   * @param supportsJava whether this backend can run Java applications
   * @param supportsNative whether this backend can run native binaries
   * @param requiresKernelIsolation whether this backend provides kernel-level isolation
   */
  ExecutionBackend(
      String id,
      String displayName,
      boolean supportsJava,
      boolean supportsNative,
      boolean requiresKernelIsolation) {
    this.id = id;
    this.displayName = displayName;
    this.supportsJava = supportsJava;
    this.supportsNative = supportsNative;
    this.requiresKernelIsolation = requiresKernelIsolation;
  }

  /**
   * Returns the unique identifier for this backend.
   *
   * @return the backend ID (e.g., "in-jvm", "container", "vm")
   */
  public String getId() {
    return id;
  }

  /**
   * Returns the human-readable display name.
   *
   * @return the display name
   */
  public String getDisplayName() {
    return displayName;
  }

  /**
   * Checks if this backend supports Java applications.
   *
   * @return true if Java apps can run on this backend
   */
  public boolean supportsJava() {
    return supportsJava;
  }

  /**
   * Checks if this backend supports native binaries.
   *
   * @return true if native executables can run on this backend
   */
  public boolean supportsNative() {
    return supportsNative;
  }

  /**
   * Checks if this backend provides kernel-level isolation.
   *
   * @return true if kernel isolation is provided
   */
  public boolean requiresKernelIsolation() {
    return requiresKernelIsolation;
  }

  /**
   * Parses a backend from its string ID.
   *
   * @param id the backend ID (case-insensitive)
   * @return the matching ExecutionBackend
   * @throws IllegalArgumentException if no matching backend found
   */
  public static ExecutionBackend fromId(String id) {
    if (id == null || id.trim().isEmpty()) {
      throw new IllegalArgumentException("Backend ID cannot be null or empty");
    }

    String normalizedId = id.trim().toLowerCase();
    for (ExecutionBackend backend : values()) {
      if (backend.getId().equals(normalizedId)) {
        return backend;
      }
    }

    throw new IllegalArgumentException("Unknown execution backend: " + id);
  }

  @Override
  public String toString() {
    return displayName + " (" + id + ")";
  }
}
