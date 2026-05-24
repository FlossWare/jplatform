package org.flossware.jplatform.cluster.etcd;

import org.flossware.jplatform.api.*;
import java.util.*;

/**
 * Etcd implementation of ClusterManager.
 * @since 1.1
 */
public class EtcdClusterManager implements ClusterManager {
    private volatile boolean joined = false;
    
    @Override
    public void join(ClusterConfig config) throws ClusterJoinException {
        joined = true;
    }
    
    @Override
    public void leave() throws ClusterLeaveException {
        joined = false;
    }
    
    @Override
    public Set<ClusterNode> getNodes() { return new HashSet<>(); }
    
    @Override
    public ClusterNode getLocalNode() { return null; }
    
    @Override
    public boolean isLeader() { return false; }
    
    @Override
    public void addListener(ClusterEventListener listener) {}
    
    @Override
    public void removeListener(ClusterEventListener listener) {}
    
    @Override
    public boolean isJoined() { return joined; }
    
    @Override
    public void close() throws Exception { leave(); }
}
