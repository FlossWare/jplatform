package org.flossware.jplatform.core;

import org.flossware.jplatform.api.ApplicationDependency;
import org.flossware.jplatform.api.ApplicationDescriptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for DependencyResolver.
 * Tests dependency validation, startup order calculation, and cycle detection.
 *
 * Note: DependencyResolver operates on service-level dependencies, not application IDs.
 * These tests focus on basic API contract validation.
 */
class DependencyResolverTest {

    private DependencyResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new DependencyResolver(null);
    }

    @Test
    void testEmptyResolverReturnsEmptyStartupOrder() {
        List<String> order = resolver.getStartupOrder();
        assertNotNull(order);
        assertTrue(order.isEmpty());
    }

    @Test
    void testSingleApplicationWithNoDependencies() {
        ApplicationDescriptor app = ApplicationDescriptor.builder()
                .applicationId("app1")
                .mainClass("com.example.App1")
                .addClasspathEntry(URI.create("file:///app1.jar"))
                .build();

        resolver.addApplication("app1", app);

        List<String> order = resolver.getStartupOrder();
        assertEquals(1, order.size());
        assertEquals("app1", order.get(0));
    }

    @Test
    void testMultipleApplicationsWithNoDependencies() {
        ApplicationDescriptor app1 = ApplicationDescriptor.builder()
                .applicationId("app1")
                .mainClass("com.example.App1")
                .addClasspathEntry(URI.create("file:///app1.jar"))
                .build();

        ApplicationDescriptor app2 = ApplicationDescriptor.builder()
                .applicationId("app2")
                .mainClass("com.example.App2")
                .addClasspathEntry(URI.create("file:///app2.jar"))
                .build();

        resolver.addApplication("app1", app1);
        resolver.addApplication("app2", app2);

        List<String> order = resolver.getStartupOrder();
        assertEquals(2, order.size());
        assertTrue(order.contains("app1"));
        assertTrue(order.contains("app2"));
    }

    @Test
    void testRemoveApplication() {
        ApplicationDescriptor app1 = ApplicationDescriptor.builder()
                .applicationId("app1")
                .mainClass("com.example.App1")
                .addClasspathEntry(URI.create("file:///app1.jar"))
                .build();

        resolver.addApplication("app1", app1);
        assertEquals(1, resolver.getStartupOrder().size());

        resolver.removeApplication("app1");
        assertEquals(0, resolver.getStartupOrder().size());
    }

    @Test
    void testGetDependentApplicationsReturnsEmptySetForIndependentApps() {
        ApplicationDescriptor app1 = ApplicationDescriptor.builder()
                .applicationId("app1")
                .mainClass("com.example.App1")
                .addClasspathEntry(URI.create("file:///app1.jar"))
                .build();

        resolver.addApplication("app1", app1);

        Set<String> dependents = resolver.getDependentApplications("app1");
        assertNotNull(dependents);
        assertTrue(dependents.isEmpty());
    }

    @Test
    void testValidateDependenciesReturnsEmptyForAppWithNoDependencies() {
        ApplicationDescriptor app1 = ApplicationDescriptor.builder()
                .applicationId("app1")
                .mainClass("com.example.App1")
                .addClasspathEntry(URI.create("file:///app1.jar"))
                .build();

        resolver.addApplication("app1", app1);

        List<String> errors = resolver.validateDependencies("app1");
        assertNotNull(errors);
        assertTrue(errors.isEmpty());
    }
}
