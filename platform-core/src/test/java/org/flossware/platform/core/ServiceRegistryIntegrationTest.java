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

package org.flossware.platform.core;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.net.URI;
import java.util.Optional;

import org.flossware.platform.api.ApplicationDescriptor;
import org.flossware.platform.api.ResourceConfig;
import org.flossware.platform.api.ServiceRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Integration tests for SimpleServiceRegistry with ApplicationManager. */
@Tag("integration")
class ServiceRegistryIntegrationTest {

  private ApplicationManager manager;
  private SimpleServiceRegistry serviceRegistry;
  private URI testClasspathUri;

  // Test service interface
  interface TestService {
    String getName();
  }

  static class TestServiceImpl implements TestService {
    private final String name;

    TestServiceImpl(String name) {
      this.name = name;
    }

    @Override
    public String getName() {
      return name;
    }
  }

  @BeforeEach
  void setUp() throws Exception {
    serviceRegistry = new SimpleServiceRegistry();
    manager = new ApplicationManager(null, serviceRegistry);

    // Get current classpath as a minimal classpath entry
    String classPath = System.getProperty("java.class.path");
    String[] entries = classPath.split(File.pathSeparator);
    testClasspathUri = new File(entries[0]).toURI();
  }

  @AfterEach
  void tearDown() {
    if (manager != null) {
      manager.shutdown();
    }
  }

  @Test
  void testDeployWithServiceRegistryEnabled() throws Exception {
    ApplicationDescriptor descriptor =
        ApplicationDescriptor.builder()
            .applicationId("service-app")
            .mainClass("com.example.App")
            .addClasspathEntry(testClasspathUri)
            .enableMessaging(true) // Enables service registry
            .resourceConfig(ResourceConfig.builder().build())
            .build();

    manager.deploy(descriptor);

    ApplicationContextImpl context =
        (ApplicationContextImpl) manager.getApplicationContext("service-app");
    assertNotNull(context);
    assertTrue(context.getServiceRegistry().isPresent(),
        "Service registry should be available when messaging enabled");

    ServiceRegistry appRegistry = context.getServiceRegistry().get();
    assertSame(serviceRegistry, appRegistry,
        "Application should get the shared service registry");
  }

  @Test
  void testDeployWithoutServiceRegistry() throws Exception {
    ApplicationDescriptor descriptor =
        ApplicationDescriptor.builder()
            .applicationId("no-service-app")
            .mainClass("com.example.App")
            .addClasspathEntry(testClasspathUri)
            .enableMessaging(false) // Disables service registry
            .resourceConfig(ResourceConfig.builder().build())
            .build();

    manager.deploy(descriptor);

    ApplicationContextImpl context =
        (ApplicationContextImpl) manager.getApplicationContext("no-service-app");
    assertNotNull(context);
    assertFalse(context.getServiceRegistry().isPresent(),
        "Service registry should not be available when messaging disabled");
  }

  @Test
  void testSharedServiceRegistryAcrossApplications() throws Exception {
    // Register a service in the shared registry
    TestService sharedService = new TestServiceImpl("shared");
    serviceRegistry.registerService(TestService.class, sharedService);

    // Deploy two applications with service registry enabled
    ApplicationDescriptor descriptor1 =
        ApplicationDescriptor.builder()
            .applicationId("app1")
            .mainClass("com.example.App1")
            .addClasspathEntry(testClasspathUri)
            .enableMessaging(true)
            .resourceConfig(ResourceConfig.builder().build())
            .build();

    ApplicationDescriptor descriptor2 =
        ApplicationDescriptor.builder()
            .applicationId("app2")
            .mainClass("com.example.App2")
            .addClasspathEntry(testClasspathUri)
            .enableMessaging(true)
            .resourceConfig(ResourceConfig.builder().build())
            .build();

    manager.deploy(descriptor1);
    manager.deploy(descriptor2);

    // Both applications should see the shared service
    ApplicationContextImpl context1 =
        (ApplicationContextImpl) manager.getApplicationContext("app1");
    ApplicationContextImpl context2 =
        (ApplicationContextImpl) manager.getApplicationContext("app2");

    assertTrue(context1.getServiceRegistry().isPresent());
    assertTrue(context2.getServiceRegistry().isPresent());

    Optional<TestService> service1 = context1.getServiceRegistry().get().getService(TestService.class);
    Optional<TestService> service2 = context2.getServiceRegistry().get().getService(TestService.class);

    assertTrue(service1.isPresent());
    assertTrue(service2.isPresent());
    assertEquals("shared", service1.get().getName());
    assertEquals("shared", service2.get().getName());
    assertSame(service1.get(), service2.get(), "Both applications should see the same service instance");
  }

  @Test
  void testManagerWithNullServiceRegistry() throws Exception {
    ApplicationManager managerNoRegistry = new ApplicationManager(null, null);

    ApplicationDescriptor descriptor =
        ApplicationDescriptor.builder()
            .applicationId("test-app")
            .mainClass("com.example.App")
            .addClasspathEntry(testClasspathUri)
            .enableMessaging(true)
            .resourceConfig(ResourceConfig.builder().build())
            .build();

    managerNoRegistry.deploy(descriptor);

    ApplicationContextImpl context =
        (ApplicationContextImpl) managerNoRegistry.getApplicationContext("test-app");
    assertFalse(context.getServiceRegistry().isPresent(),
        "Service registry should not be available when manager created without one");

    managerNoRegistry.shutdown();
  }

  @Test
  void testAllThreeFeaturesTogether() throws Exception {
    ApplicationDescriptor descriptor =
        ApplicationDescriptor.builder()
            .applicationId("full-featured-app")
            .mainClass("com.example.App")
            .addClasspathEntry(testClasspathUri)
            .enableMessaging(true) // ServiceRegistry
            .property("healthcheck.enabled", "true") // HealthChecker
            .property("restart.policy", "on-failure") // RestartManager
            .resourceConfig(ResourceConfig.builder().build())
            .build();

    manager.deploy(descriptor);

    ApplicationContextImpl context =
        (ApplicationContextImpl) manager.getApplicationContext("full-featured-app");
    assertNotNull(context);

    // Verify all three features are available
    assertTrue(context.getServiceRegistry().isPresent(),
        "Service registry should be available");
    assertTrue(context.getHealthChecker().isPresent(),
        "Health checker should be available");
    assertTrue(context.getRestartManager().isPresent(),
        "Restart manager should be available");
  }
}
