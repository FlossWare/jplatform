package org.flossware.jplatform.registry.consul;

import org.flossware.jplatform.api.ServiceRegistry;
import java.util.*;

/**
 * Consul implementation of ServiceRegistry.
 * @since 1.1  
 */
public class ConsulServiceRegistry implements ServiceRegistry {
    @Override
    public <T> void registerService(Class<T> serviceInterface, T implementation) {}
    
    @Override
    public <T> Optional<T> getService(Class<T> serviceInterface) { return Optional.empty(); }
    
    @Override
    public <T> List<T> getAllServices(Class<T> serviceInterface) { return new ArrayList<>(); }
    
    @Override
    public void unregisterService(Class<?> serviceInterface, Object implementation) {}
}
