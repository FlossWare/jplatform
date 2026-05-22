package org.flossware.jplatform.cluster;

import org.flossware.jplatform.api.Application;
import org.flossware.jplatform.api.ApplicationContext;

/**
 * Simple test application for unit tests.
 * Implements both Application interface and provides a main() method
 * for testing purposes.
 */
public class TestApp implements Application {

    @Override
    public void start(ApplicationContext context) throws Exception {
        // Do nothing - just for testing
    }

    @Override
    public void stop() throws Exception {
        // Do nothing - just for testing
    }

    /**
     * Main method for testing non-Application deployments.
     */
    public static void main(String[] args) {
        // Do nothing - just for testing
    }
}
