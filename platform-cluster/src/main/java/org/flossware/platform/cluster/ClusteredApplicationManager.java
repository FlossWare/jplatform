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

package org.flossware.platform.cluster;

import java.util.Map;

import org.flossware.platform.api.*;
import org.flossware.platform.core.ApplicationManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Cluster-aware extension of ApplicationManager. Integrates with ClusterManager and
 * ClusterStateStore to enable distributed application deployment and lifecycle management across
 * multiple nodes.
 *
 * <p>Key features:
 *
 * <ul>
 *   <li>Distributed deployment - descriptors stored in cluster state
 *   <li>Leader-based scheduling - leader assigns apps to nodes
 *   <li>Node-local execution - apps only start if assigned to this node
 *   <li>State synchronization - state changes replicated across cluster
 *   <li>Fallback to standalone - works without clustering
 * </ul>
 *
 * <p>Deployment flow in clustered mode:
 *
 * <ol>
 *   <li>Application descriptor written to cluster state store
 *   <li>Leader node assigns application to a target node via scheduler
 *   <li>Target node detects assignment and deploys locally
 *   <li>State changes are synchronized via cluster state store
 * </ol>
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * // Create cluster manager
 * HazelcastClusterManager clusterManager = new HazelcastClusterManager();
 * clusterManager.join(clusterConfig);
 *
 * // Create state store
 * HazelcastStateStore stateStore = new HazelcastStateStore(
 *     clusterManager.getHazelcastInstance());
 *
 * // Create clustered application manager
 * ClusteredApplicationManager appManager = new ClusteredApplicationManager(
 *     messageBus, serviceRegistry, clusterManager, stateStore);
 *
 * // Deploy application (will be assigned by leader)
 * appManager.deploy(descriptor);
 * }</pre>
 *
 * @see ApplicationManager
 * @see ClusterManager
 * @see ClusterStateStore
 */
public class ClusteredApplicationManager extends ApplicationManager {

  private static final Logger LOGGER = LoggerFactory.getLogger(ClusteredApplicationManager.class);

  private final ClusterManager clusterManager;
  private final ClusterStateStore stateStore;
  private final ApplicationScheduler scheduler;
  private final ClusterEventListener clusterListener;

  /**
   * Constructs a new clustered application manager.
   *
   * @param messageBus the shared message bus, or null to disable messaging
   * @param serviceRegistry the shared service registry, or null to disable
   * @param clusterManager the cluster manager for distributed coordination
   * @param stateStore the cluster state store for distributed state
   * @throws IllegalArgumentException if clusterManager and stateStore are inconsistent
   */
  public ClusteredApplicationManager(
      MessageBus messageBus,
      ServiceRegistry serviceRegistry,
      ClusterManager clusterManager,
      ClusterStateStore stateStore) {
    super(messageBus, serviceRegistry);

    // Validate consistency of cluster components
    if ((clusterManager == null) != (stateStore == null)) {
      throw new IllegalArgumentException(
          "ClusterManager and ClusterStateStore must both be null (standalone mode) "
              + "or both be non-null (clustered mode). "
              + "Provided: clusterManager="
              + (clusterManager != null ? "present" : "null")
              + ", stateStore="
              + (stateStore != null ? "present" : "null"));
    }

    this.clusterManager = clusterManager;
    this.stateStore = stateStore;

    // Create scheduler if we have a Hazelcast-based cluster
    if (clusterManager instanceof HazelcastClusterManager) {
      HazelcastClusterManager hzManager = (HazelcastClusterManager) clusterManager;
      this.scheduler =
          new ApplicationScheduler(
              hzManager.getHazelcastInstance(),
              clusterManager,
              ApplicationScheduler.SchedulingStrategy.LEAST_LOADED);
    } else {
      this.scheduler = null;
      if (clusterManager != null) {
        LOGGER.warn(
            "Application scheduling is only supported with HazelcastClusterManager. "
                + "Using {} will result in local-only deployment. "
                + "Applications will not be scheduled across the cluster.",
            clusterManager.getClass().getSimpleName());
      }
    }

    // Subscribe to cluster events (only if cluster manager is present)
    if (clusterManager != null) {
      this.clusterListener =
          new ClusterEventListener() {
            @Override
            public void onNodeJoined(ClusterNode node) {
              LOGGER.info("Node joined cluster: {}", node.getNodeId());
            }

            @Override
            public void onNodeLeft(ClusterNode node) {
              LOGGER.warn("Node left cluster: {}", node.getNodeId());
              handleNodeFailure(node);
            }

            @Override
            public void onLeaderChanged(ClusterNode newLeader) {
              LOGGER.info("New cluster leader: {}", newLeader.getNodeId());
            }
          };
      clusterManager.addListener(clusterListener);
    } else {
      this.clusterListener = null;
    }

    LOGGER.info(
        "ClusteredApplicationManager initialized in {} mode",
        clusterManager != null ? "clustered" : "standalone");
  }

  /**
   * Deploys an application to the cluster. In clustered mode, the descriptor is written to the
   * cluster state store, and the leader assigns the application to a node. In standalone mode,
   * falls back to local deployment.
   *
   * @param descriptor the application descriptor
   * @throws Exception if deployment fails
   */
  @Override
  public synchronized void deploy(ApplicationDescriptor descriptor) throws Exception {
    String appId = descriptor.getApplicationId();

    if (clusterManager != null && clusterManager.isJoined()) {
      LOGGER.info("[{}] Deploying application in cluster mode", appId);

      boolean clusterStateWritten = false;
      boolean assigned = false;
      boolean deployedLocally = false;

      try {
        // Step 1: Write descriptor to cluster state
        stateStore.putApplicationDescriptor(appId, descriptor);
        stateStore.putApplicationState(appId, ApplicationState.DEPLOYED);
        clusterStateWritten = true;

        // Step 2: If leader, assign to a node
        if (scheduler != null) {
          // Perform leadership operations with proper exception handling
          boolean wasLeader = clusterManager.isLeader();
          if (wasLeader) {
            try {
              // Verify leadership immediately before critical operation
              if (!clusterManager.isLeader()) {
                LOGGER.info("[{}] Lost leadership before assignment, skipping", appId);
              } else {
                String assignedNode = scheduler.assignApplication(appId);
                assigned = true;
                LOGGER.info("[{}] Leader assigned application to node: {}", appId, assignedNode);
              }
            } catch (IllegalStateException e) {
              // Lost leadership during assignment - expected race condition
              LOGGER.info(
                  "[{}] Lost leadership during assignment, will be handled by new leader", appId);
              assigned = false; // Ensure rollback doesn't attempt to unassign
            } catch (Exception e) {
              // Other assignment errors should trigger rollback
              LOGGER.error("[{}] Assignment failed: {}", appId, e.getMessage());
              throw e;
            }
          }

          // Step 3: Check if assigned to local node and deploy locally
          if (scheduler.isAssignedToLocalNode(appId)) {
            LOGGER.info("[{}] Application assigned to local node, deploying locally", appId);

            // Verify assignment hasn't changed before deploying (race condition check)
            if (!scheduler.isAssignedToLocalNode(appId)) {
              LOGGER.info("[{}] Assignment changed before deployment, skipping", appId);
            } else {
              super.deploy(descriptor);
              deployedLocally = true;

              // Verify assignment is still valid after deployment (race condition check)
              if (!scheduler.isAssignedToLocalNode(appId)) {
                LOGGER.warn(
                    "[{}] Assignment changed during deployment, cleanup may be needed", appId);
                // Deployment happened but assignment changed - rollback will handle cleanup
                throw new Exception("Assignment changed during deployment");
              }
            }
          } else {
            LOGGER.info(
                "[{}] Application assigned to another node, skipping local deployment", appId);
          }
        } else {
          // No scheduler available, deploy locally
          LOGGER.info("[{}] No scheduler available, deploying locally", appId);
          super.deploy(descriptor);
          deployedLocally = true;
        }

      } catch (Exception e) {
        LOGGER.error("[{}] Deployment failed, initiating rollback", appId, e);

        // Rollback in reverse order
        if (deployedLocally) {
          try {
            super.undeploy(appId);
            LOGGER.info("[{}] Rolled back local deployment", appId);
          } catch (Exception rollbackEx) {
            LOGGER.error("[{}] Failed to rollback local deployment", appId, rollbackEx);
          }
        }

        if (assigned && clusterManager.isLeader()) {
          try {
            scheduler.unassignApplication(appId);
            LOGGER.info("[{}] Rolled back application assignment", appId);
          } catch (Exception rollbackEx) {
            LOGGER.error("[{}] Failed to rollback assignment", appId, rollbackEx);
          }
        }

        if (clusterStateWritten) {
          try {
            stateStore.putApplicationState(appId, ApplicationState.FAILED);
            LOGGER.info("[{}] Updated cluster state to FAILED", appId);
          } catch (Exception rollbackEx) {
            LOGGER.error("[{}] Failed to update cluster state to FAILED", appId, rollbackEx);
          }
        }

        throw new Exception("Deployment failed and rollback completed: " + e.getMessage(), e);
      }
    } else {
      // Standalone mode
      LOGGER.info("[{}] Deploying application in standalone mode", appId);
      super.deploy(descriptor);
    }
  }

  /**
   * Starts a deployed application. In clustered mode, checks if the application is assigned to this
   * node. Only starts if assigned locally or in standalone mode.
   *
   * @param applicationId the application identifier
   * @throws Exception if starting fails
   */
  @Override
  public synchronized void start(String applicationId) throws Exception {
    if (clusterManager != null && clusterManager.isJoined() && scheduler != null) {
      // Check if assigned to this node
      if (!scheduler.isAssignedToLocalNode(applicationId)) {
        LOGGER.warn("[{}] Application not assigned to this node, cannot start", applicationId);
        throw new IllegalStateException("Application not assigned to this node: " + applicationId);
      }

      LOGGER.info("[{}] Starting application on assigned node", applicationId);
      super.start(applicationId);

      // Update cluster state
      try {
        stateStore.putApplicationState(applicationId, ApplicationState.RUNNING);
      } catch (Exception e) {
        LOGGER.error("[{}] Failed to update cluster state to RUNNING", applicationId, e);
        // Application is already started locally, but cluster state is inconsistent
        throw new Exception(
            "Application started but failed to update cluster state: " + e.getMessage(), e);
      }

    } else {
      // Standalone mode
      LOGGER.info("[{}] Starting application in standalone mode", applicationId);
      super.start(applicationId);
    }
  }

  /**
   * Stops a running application. In clustered mode, updates the cluster state.
   *
   * @param applicationId the application identifier
   * @throws Exception if stopping fails
   */
  @Override
  public synchronized void stop(String applicationId) throws Exception {
    if (clusterManager != null && clusterManager.isJoined() && scheduler != null) {
      // Check if assigned to this node
      if (!scheduler.isAssignedToLocalNode(applicationId)) {
        LOGGER.warn("[{}] Application not assigned to this node, cannot stop", applicationId);
        throw new IllegalStateException("Application not assigned to this node: " + applicationId);
      }

      LOGGER.info("[{}] Stopping application on assigned node", applicationId);
      super.stop(applicationId);

      // Update cluster state
      try {
        stateStore.putApplicationState(applicationId, ApplicationState.STOPPED);
      } catch (Exception e) {
        LOGGER.error("[{}] Failed to update cluster state to STOPPED", applicationId, e);
        // Application is already stopped locally, but cluster state is inconsistent
        throw new Exception(
            "Application stopped but failed to update cluster state: " + e.getMessage(), e);
      }

    } else {
      // Standalone mode
      LOGGER.info("[{}] Stopping application in standalone mode", applicationId);
      super.stop(applicationId);
    }
  }

  /**
   * Undeploys an application from the cluster. In clustered mode, removes from cluster state and
   * scheduler.
   *
   * @param applicationId the application identifier
   * @throws Exception if undeployment fails
   */
  @Override
  public synchronized void undeploy(String applicationId) throws Exception {
    if (clusterManager != null && clusterManager.isJoined() && scheduler != null) {
      boolean undeployedLocally = false;
      boolean unassigned = false;
      ApplicationDescriptor descriptor = null;

      try {
        // Save descriptor for potential rollback
        if (scheduler.isAssignedToLocalNode(applicationId)) {
          descriptor = stateStore.getApplicationDescriptor(applicationId);
        }

        // Step 1: Undeploy locally if assigned to this node
        if (scheduler.isAssignedToLocalNode(applicationId)) {
          LOGGER.info("[{}] Undeploying application from assigned node", applicationId);
          super.undeploy(applicationId);
          undeployedLocally = true;
        }

        // Step 2: If leader, remove from scheduler and cluster state
        boolean wasLeader = clusterManager.isLeader();
        if (wasLeader) {
          try {
            // Verify leadership immediately before critical operations
            if (!clusterManager.isLeader()) {
              LOGGER.info(
                  "[{}] Lost leadership before unassignment, skipping cluster cleanup",
                  applicationId);
            } else {
              scheduler.unassignApplication(applicationId);
              unassigned = true;

              // Verify leadership again before state update
              if (!clusterManager.isLeader()) {
                LOGGER.warn(
                    "[{}] Lost leadership after unassignment, state update may fail",
                    applicationId);
              }

              stateStore.putApplicationState(applicationId, ApplicationState.UNDEPLOYED);
              LOGGER.info("[{}] Leader removed application from cluster", applicationId);
            }
          } catch (IllegalStateException e) {
            // Lost leadership during operations - expected race condition
            LOGGER.info(
                "[{}] Lost leadership during cluster cleanup, will be handled by new leader",
                applicationId);
            // Don't attempt rollback of cluster operations if we lost leadership
            if (unassigned) {
              unassigned = false;
            }
          } catch (Exception e) {
            // Other errors should trigger rollback
            LOGGER.error("[{}] Cluster cleanup failed: {}", applicationId, e.getMessage());
            throw e;
          }
        }

      } catch (Exception e) {
        LOGGER.error("[{}] Undeploy failed, initiating rollback", applicationId, e);

        // Rollback in reverse order
        if (unassigned && clusterManager.isLeader() && descriptor != null) {
          try {
            scheduler.assignApplication(applicationId);
            LOGGER.info("[{}] Rolled back application unassignment", applicationId);
          } catch (Exception rollbackEx) {
            LOGGER.error("[{}] Failed to rollback unassignment", applicationId, rollbackEx);
          }
        }

        if (undeployedLocally && descriptor != null) {
          try {
            super.deploy(descriptor);
            LOGGER.info("[{}] Rolled back local undeploy by redeploying", applicationId);
          } catch (Exception rollbackEx) {
            LOGGER.error("[{}] Failed to rollback local undeploy", applicationId, rollbackEx);
          }
        }

        throw new Exception("Undeploy failed and rollback attempted: " + e.getMessage(), e);
      }

    } else {
      // Standalone mode
      LOGGER.info("[{}] Undeploying application in standalone mode", applicationId);
      super.undeploy(applicationId);
    }
  }

  /**
   * Lists all applications in the cluster. In clustered mode, returns applications from cluster
   * state. In standalone mode, returns local applications.
   *
   * @return a map of application IDs to their current states
   */
  @Override
  public Map<String, ApplicationState> listApplications() {
    if (clusterManager != null && clusterManager.isJoined() && stateStore != null) {
      try {
        // Try to return cluster-wide applications
        return stateStore.getAllApplicationStates();
      } catch (Exception e) {
        LOGGER.warn(
            "Failed to get cluster application states, falling back to local: {}", e.getMessage());
        // Fall through to local applications
      }
    }

    // Return local applications
    return super.listApplications();
  }

  /**
   * Returns the cluster manager.
   *
   * @return the cluster manager, or null if not clustered
   */
  public ClusterManager getClusterManager() {
    return clusterManager;
  }

  /**
   * Returns the cluster state store.
   *
   * @return the state store, or null if not clustered
   */
  public ClusterStateStore getStateStore() {
    return stateStore;
  }

  /**
   * Returns the application scheduler.
   *
   * @return the scheduler, or null if not clustered
   */
  public ApplicationScheduler getScheduler() {
    return scheduler;
  }

  /**
   * Handles node failure by reassigning applications if this is the leader.
   *
   * @param failedNode the failed cluster node
   */
  private void handleNodeFailure(ClusterNode failedNode) {
    if (failedNode == null) {
      LOGGER.error("handleNodeFailure called with null failedNode, ignoring");
      return;
    }

    if (clusterManager.isLeader() && scheduler != null) {
      String nodeId = failedNode.getNodeId();
      if (nodeId == null) {
        LOGGER.error("Failed node has null nodeId, cannot reassign applications");
        return;
      }

      LOGGER.warn("Handling failure of node: {}", nodeId);
      try {
        int reassignedCount = scheduler.reassignFromFailedNode(nodeId);
        LOGGER.info("Reassigned {} applications from failed node {}", reassignedCount, nodeId);
      } catch (Exception e) {
        LOGGER.error("Error reassigning applications from failed node {}", nodeId, e);
      }
    }
  }

  /**
   * Shuts down the clustered application manager. Removes cluster event listener before delegating
   * to parent shutdown.
   */
  @Override
  public void shutdown() {
    try {
      // Remove cluster event listener to prevent resource leak
      if (clusterManager != null && clusterListener != null) {
        clusterManager.removeListener(clusterListener);
        LOGGER.debug("Removed cluster event listener");
      }
    } catch (Exception e) {
      LOGGER.error("Error removing cluster event listener", e);
    } finally {
      // Always shutdown parent, even if listener removal failed
      super.shutdown();
    }
  }
}
