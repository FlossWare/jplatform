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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.flossware.platform.api.ApplicationDescriptor;
import org.flossware.platform.api.ApplicationLifecycleListener;
import org.flossware.platform.api.HealthStatus;
import org.flossware.platform.api.ResourceConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Integration tests for ApplicationLifecycleListener. */
@Tag("integration")
class ApplicationLifecycleListenerTest {

  private ApplicationManager manager;
  private URI testClasspathUri;

  @BeforeEach
  void setUp() throws Exception {
    manager = new ApplicationManager();

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
  void testAddLifecycleListener() {
    TestListener listener = new TestListener();
    manager.addLifecycleListener("test", listener);

    assertEquals(1, manager.getLifecycleListenerCount());
  }

  @Test
  void testRemoveLifecycleListener() {
    TestListener listener = new TestListener();
    manager.addLifecycleListener("test", listener);

    assertTrue(manager.removeLifecycleListener("test"));
    assertEquals(0, manager.getLifecycleListenerCount());
  }

  @Test
  void testRemoveNonExistentListener() {
    assertFalse(manager.removeLifecycleListener("nonexistent"));
  }

  @Test
  void testAddDuplicateListenerThrows() {
    TestListener listener1 = new TestListener();
    TestListener listener2 = new TestListener();

    manager.addLifecycleListener("test", listener1);

    assertThrows(
        IllegalArgumentException.class,
        () -> {
          manager.addLifecycleListener("test", listener2);
        });
  }

  @Test
  void testOnDeployedNotification() throws Exception {
    CountDownLatch latch = new CountDownLatch(1);
    TestListener listener =
        new TestListener() {
          @Override
          public void onDeployed(String applicationId, ApplicationDescriptor descriptor) {
            super.onDeployed(applicationId, descriptor);
            latch.countDown();
          }
        };

    manager.addLifecycleListener("test", listener);

    ApplicationDescriptor descriptor =
        ApplicationDescriptor.builder()
            .applicationId("deploy-test-app")
            .mainClass("com.example.App")
            .addClasspathEntry(testClasspathUri)
            .resourceConfig(ResourceConfig.builder().build())
            .build();

    manager.deploy(descriptor);

    assertTrue(latch.await(2, TimeUnit.SECONDS), "onDeployed should be called");
    assertEquals(1, listener.deployedCount.get());
  }

  @Test
  void testOnUndeployedNotification() throws Exception {
    CountDownLatch latch = new CountDownLatch(1);
    TestListener listener =
        new TestListener() {
          @Override
          public void onUndeployed(String applicationId) {
            super.onUndeployed(applicationId);
            latch.countDown();
          }
        };

    manager.addLifecycleListener("test", listener);

    ApplicationDescriptor descriptor =
        ApplicationDescriptor.builder()
            .applicationId("undeploy-test-app")
            .mainClass("com.example.App")
            .addClasspathEntry(testClasspathUri)
            .resourceConfig(ResourceConfig.builder().build())
            .build();

    manager.deploy(descriptor);
    manager.undeploy("undeploy-test-app");

    assertTrue(latch.await(2, TimeUnit.SECONDS), "onUndeployed should be called");
    assertEquals(1, listener.undeployedCount.get());
  }

  @Test
  void testMultipleListeners() throws Exception {
    TestListener listener1 = new TestListener();
    TestListener listener2 = new TestListener();

    manager.addLifecycleListener("listener1", listener1);
    manager.addLifecycleListener("listener2", listener2);

    ApplicationDescriptor descriptor =
        ApplicationDescriptor.builder()
            .applicationId("multi-listener-app")
            .mainClass("com.example.App")
            .addClasspathEntry(testClasspathUri)
            .resourceConfig(ResourceConfig.builder().build())
            .build();

    manager.deploy(descriptor);

    // Both listeners should be notified
    Thread.sleep(100); // Give async calls time to complete
    assertEquals(1, listener1.deployedCount.get());
    assertEquals(1, listener2.deployedCount.get());
  }

  @Test
  void testListenerExceptionDoesNotBreakLifecycle() throws Exception {
    TestListener goodListener = new TestListener();
    ApplicationLifecycleListener badListener =
        new ApplicationLifecycleListener() {
          @Override
          public void onDeployed(String applicationId, ApplicationDescriptor descriptor) {
            throw new RuntimeException("Listener exception");
          }

          @Override
          public void onStarted(String applicationId) {}

          @Override
          public void onStopped(String applicationId, int exitCode) {}

          @Override
          public void onRestarted(String applicationId, int attemptNumber) {}

          @Override
          public void onHealthChanged(
              String applicationId, HealthStatus oldStatus, HealthStatus newStatus) {}

          @Override
          public void onUndeployed(String applicationId) {}
        };

    manager.addLifecycleListener("bad", badListener);
    manager.addLifecycleListener("good", goodListener);

    ApplicationDescriptor descriptor =
        ApplicationDescriptor.builder()
            .applicationId("exception-test-app")
            .mainClass("com.example.App")
            .addClasspathEntry(testClasspathUri)
            .resourceConfig(ResourceConfig.builder().build())
            .build();

    // Should not throw - exception should be caught and logged
    manager.deploy(descriptor);

    Thread.sleep(100);
    // Good listener should still be called
    assertEquals(1, goodListener.deployedCount.get());
  }

  /** Test implementation of ApplicationLifecycleListener. */
  static class TestListener implements ApplicationLifecycleListener {
    final AtomicInteger deployedCount = new AtomicInteger(0);
    final AtomicInteger startedCount = new AtomicInteger(0);
    final AtomicInteger stoppedCount = new AtomicInteger(0);
    final AtomicInteger restartedCount = new AtomicInteger(0);
    final AtomicInteger healthChangedCount = new AtomicInteger(0);
    final AtomicInteger undeployedCount = new AtomicInteger(0);

    @Override
    public void onDeployed(String applicationId, ApplicationDescriptor descriptor) {
      deployedCount.incrementAndGet();
    }

    @Override
    public void onStarted(String applicationId) {
      startedCount.incrementAndGet();
    }

    @Override
    public void onStopped(String applicationId, int exitCode) {
      stoppedCount.incrementAndGet();
    }

    @Override
    public void onRestarted(String applicationId, int attemptNumber) {
      restartedCount.incrementAndGet();
    }

    @Override
    public void onHealthChanged(
        String applicationId, HealthStatus oldStatus, HealthStatus newStatus) {
      healthChangedCount.incrementAndGet();
    }

    @Override
    public void onUndeployed(String applicationId) {
      undeployedCount.incrementAndGet();
    }
  }
}
