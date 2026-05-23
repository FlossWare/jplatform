package org.flossware.jplatform.cluster;

import org.flossware.jplatform.api.Application;
import org.flossware.jplatform.api.ApplicationContext;
import org.flossware.jplatform.api.ApplicationShutdownException;
import org.flossware.jplatform.api.ApplicationStartupException;

/**
 * Simple test application for unit tests.
 * Implements both Application interface and provides a main() method
 * for testing purposes.
 */
public class TestApp implements Application {

    @Override
    public void start(ApplicationContext context) throws ApplicationStartupException {
        // Do nothing - just for testing
    }

    @Override
    public void stop() throws ApplicationShutdownException {
        // Do nothing - just for testing
    }

    /**
     * Main method for testing non-Application deployments.
     */
    public static void main(String[] args) {
        // Do nothing - just for testing
    }
}
