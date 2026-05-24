package org.flossware.jplatform.cluster.etcd;

import org.flossware.jplatform.api.*;
import java.util.*;

/**
 * Etcd implementation of ClusterStateStore.
 * @since 1.1
 */
public class EtcdStateStore implements ClusterStateStore {
    @Override
    public void putApplicationState(String id, ApplicationState state) {}
    
    @Override
    public ApplicationState getApplicationState(String id) { return null; }
    
    @Override
    public Map<String, ApplicationState> getAllApplicationStates() { return new HashMap<>(); }
    
    @Override
    public void putApplicationDescriptor(String id, ApplicationDescriptor desc) {}
    
    @Override
    public ApplicationDescriptor getApplicationDescriptor(String id) { return null; }
    
    @Override
    public Map<String, ApplicationDescriptor> getAllApplicationDescriptors() { return new HashMap<>(); }
    
    @Override
    public void subscribe(String key, StateChangeListener listener) {}
    
    @Override
    public void unsubscribe(String key, StateChangeListener listener) {}
}
