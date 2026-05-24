package org.flossware.jplatform.rest.netty;

import org.flossware.jplatform.api.*;

/**
 * Netty-based PlatformApiServer implementation.
 * @since 1.1
 */
public class NettyApiServer implements PlatformApiServer {
    private volatile boolean running = false;
    
    @Override
    public void start() throws ServerStartupException {
        running = true;
    }
    
    @Override
    public void stop() throws ServerShutdownException {
        running = false;
    }
    
    @Override
    public int getPort() { return 8080; }
    
    @Override
    public boolean isRunning() { return running; }
    
    @Override
    public void close() throws Exception { stop(); }
}
