package org.flossware.jplatform.core;

import org.flossware.jplatform.api.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ApplicationManager.
 * Tests core lifecycle management: deploy, start, stop, undeploy.
 * <p>
 * Note: These tests focus on API contract and state management.
 * Full integration tests with actual application loading are in jplatform-launcher module.
 */
class ApplicationManagerTest {

    private ApplicationManager manager;

    @BeforeEach
    void setUp() {
        manager = new ApplicationManager();
    }

    @AfterEach
    void tearDown() throws Exception {
        if (manager != null) {
            manager.shutdown();
        }
    }

    @Test
    void testInitialState() {
        Map<String, ApplicationState> apps = manager.listApplications();
        assertNotNull(apps);
        assertTrue(apps.isEmpty(), "New manager should have no applications");
    }

    @Test
    void testGetApplicationContextReturnsNullForNonExistent() {
        ApplicationContext context = manager.getApplicationContext("non-existent");
        assertNull(context, "Should return null for non-existent application");
    }

    @Test
    void testUndeployNonExistentApplicationThrowsException() {
        assertThrows(IllegalStateException.class, () -> manager.undeploy("non-existent"),
                "Undeploying non-existent application should throw IllegalStateException");
    }

    @Test
    void testStartNonExistentApplicationThrowsException() {
        assertThrows(IllegalStateException.class, () -> manager.start("non-existent"),
                "Starting non-existent application should throw IllegalStateException");
    }

    @Test
    void testStopNonExistentApplicationThrowsException() {
        assertThrows(IllegalStateException.class, () -> manager.stop("non-existent"),
                "Stopping non-existent application should throw IllegalStateException");
    }

    @Test
    void testShutdownOnEmptyManager() throws Exception {
        manager.shutdown();
        assertEquals(0, manager.listApplications().size(),
                "Shutdown on empty manager should succeed");
    }

    // Note: Full deployment tests with actual class loading would require:
    // 1. A real JAR file with compiled classes
    // 2. A valid main class that implements Application interface
    // 3. Proper classpath setup
    // These are tested in integration tests in jplatform-launcher module.
}
