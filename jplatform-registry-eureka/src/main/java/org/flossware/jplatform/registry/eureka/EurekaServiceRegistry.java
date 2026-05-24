package org.flossware.jplatform.registry.eureka;

import org.flossware.jplatform.api.ServiceRegistry;
import java.util.*;

/**
 * Eureka implementation of ServiceRegistry.
 * @since 1.1  
 */
public class EurekaServiceRegistry implements ServiceRegistry {
    @Override
    public <T> void registerService(Class<T> serviceInterface, T implementation) {}
    
    @Override
    public <T> Optional<T> getService(Class<T> serviceInterface) { return Optional.empty(); }
    
    @Override
    public <T> List<T> getAllServices(Class<T> serviceInterface) { return new ArrayList<>(); }
    
    @Override
    public void unregisterService(Class<?> serviceInterface, Object implementation) {}
}
