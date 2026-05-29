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
 * <p>Defines the available execution environments where applications can run.
 * The {@link WorkloadPlacementScheduler} uses these backends to determine the
 * optimal placement for each workload.</p>
 *
 * <p>Backend characteristics:</p>
 * <ul>
 *   <li>{@link #IN_JVM} - Fastest startup, lowest overhead, Java-only, shared JVM</li>
 *   <li>{@link #CONTAINER} - Process isolation, portable, multi-language via images</li>
 *   <li>{@link #VIRTUAL_MACHINE} - Kernel isolation, full OS, highest overhead</li>
 *   <li>{@link #KUBERNETES} - Cluster orchestration, massive scale, external dependency</li>
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
     * <p><b>Characteristics:</b></p>
     * <ul>
     *   <li>Startup: &lt;100ms</li>
     *   <li>Overhead: ~10MB per app</li>
     *   <li>Isolation: ClassLoader, thread pool, security policy</li>
     *   <li>Supported: Java applications only</li>
     *   <li>Best for: Lightweight Java apps, microservices, dev environments</li>
     * </ul>
     *
     * <p><b>Limitations:</b></p>
     * <ul>
     *   <li>Shared JVM heap (requires resource quotas)</li>
     *   <li>No kernel-level isolation</li>
     *   <li>Java-only (no native binaries or polyglot)</li>
     * </ul>
     */
    IN_JVM("in-jvm", "Isolated ClassLoader", true, false, false),

    /**
     * Container execution using Docker, Podman, or LXC.
     *
     * <p><b>Characteristics:</b></p>
     * <ul>
     *   <li>Startup: 1-3 seconds</li>
     *   <li>Overhead: 100-500MB per container</li>
     *   <li>Isolation: Process namespace, cgroups, network</li>
     *   <li>Supported: Any containerized application</li>
     *   <li>Best for: Heavy Java apps, polyglot services, portable deployments</li>
     * </ul>
     *
     * <p><b>Limitations:</b></p>
     * <ul>
     *   <li>Higher memory overhead than in-JVM</li>
     *   <li>Requires container runtime (Docker/Podman/LXC)</li>
     *   <li>Slower startup than in-JVM</li>
     * </ul>
     */
    CONTAINER("container", "Docker/Podman/LXC Container", true, true, false),

    /**
     * Virtual machine execution using KVM/QEMU via libvirt.
     *
     * <p><b>Characteristics:</b></p>
     * <ul>
     *   <li>Startup: 10-30 seconds</li>
     *   <li>Overhead: 512MB+ per VM</li>
     *   <li>Isolation: Full kernel isolation, separate OS</li>
     *   <li>Supported: Any OS, native binaries, legacy apps</li>
     *   <li>Best for: Non-Java apps, kernel-level isolation, Windows/legacy</li>
     * </ul>
     *
     * <p><b>Limitations:</b></p>
     * <ul>
     *   <li>Highest overhead (full OS per VM)</li>
     *   <li>Slowest startup time</li>
     *   <li>Requires KVM/libvirt infrastructure</li>
     * </ul>
     */
    VIRTUAL_MACHINE("vm", "KVM/QEMU Virtual Machine", false, true, true),

    /**
     * Kubernetes cluster execution (future support).
     *
     * <p><b>Characteristics:</b></p>
     * <ul>
     *   <li>Startup: 5-10 seconds</li>
     *   <li>Overhead: Varies by deployment</li>
     *   <li>Isolation: Pod/namespace isolation</li>
     *   <li>Supported: Any containerized app, KubeVirt VMs</li>
     *   <li>Best for: Massive scale (50+ replicas), multi-cloud, polyglot</li>
     * </ul>
     *
     * <p><b>Limitations:</b></p>
     * <ul>
     *   <li>Requires external Kubernetes cluster</li>
     *   <li>Higher operational complexity</li>
     *   <li>Network latency for cross-cluster communication</li>
     * </ul>
     *
     * <p><b>Note:</b> Kubernetes backend is not yet implemented.
     * Will be added in a future release.</p>
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
    ExecutionBackend(String id, String displayName, boolean supportsJava,
                     boolean supportsNative, boolean requiresKernelIsolation) {
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
