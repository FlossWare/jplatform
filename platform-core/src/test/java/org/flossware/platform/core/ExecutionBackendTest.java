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

import org.junit.jupiter.api.Test;

/** Unit tests for ExecutionBackend enum. */
class ExecutionBackendTest {

  @Test
  void testInJvmBackendProperties() {
    ExecutionBackend backend = ExecutionBackend.IN_JVM;

    assertEquals("in-jvm", backend.getId());
    assertTrue(backend.supportsJava());
    assertFalse(backend.supportsNative());
    assertFalse(backend.requiresKernelIsolation());
    assertEquals("Isolated ClassLoader (in-jvm)", backend.toString());
  }

  @Test
  void testContainerBackendProperties() {
    ExecutionBackend backend = ExecutionBackend.CONTAINER;

    assertEquals("container", backend.getId());
    assertTrue(backend.supportsJava());
    assertTrue(backend.supportsNative());
    assertFalse(backend.requiresKernelIsolation());
  }

  @Test
  void testVirtualMachineBackendProperties() {
    ExecutionBackend backend = ExecutionBackend.VIRTUAL_MACHINE;

    assertEquals("vm", backend.getId());
    assertFalse(backend.supportsJava());
    assertTrue(backend.supportsNative());
    assertTrue(backend.requiresKernelIsolation());
  }

  @Test
  void testKubernetesBackendProperties() {
    ExecutionBackend backend = ExecutionBackend.KUBERNETES;

    assertEquals("kubernetes", backend.getId());
    assertTrue(backend.supportsJava());
    assertTrue(backend.supportsNative());
    assertFalse(backend.requiresKernelIsolation());
  }

  @Test
  void testFromIdCaseInsensitive() {
    assertEquals(ExecutionBackend.IN_JVM, ExecutionBackend.fromId("in-jvm"));
    assertEquals(ExecutionBackend.IN_JVM, ExecutionBackend.fromId("IN-JVM"));
    assertEquals(ExecutionBackend.IN_JVM, ExecutionBackend.fromId("In-Jvm"));

    assertEquals(ExecutionBackend.CONTAINER, ExecutionBackend.fromId("container"));
    assertEquals(ExecutionBackend.VIRTUAL_MACHINE, ExecutionBackend.fromId("vm"));
    assertEquals(ExecutionBackend.KUBERNETES, ExecutionBackend.fromId("kubernetes"));
  }

  @Test
  void testFromIdWithWhitespace() {
    assertEquals(ExecutionBackend.IN_JVM, ExecutionBackend.fromId("  in-jvm  "));
    assertEquals(ExecutionBackend.CONTAINER, ExecutionBackend.fromId(" container "));
  }

  @Test
  void testFromIdInvalid() {
    assertThrows(IllegalArgumentException.class, () -> ExecutionBackend.fromId("invalid"));
    assertThrows(IllegalArgumentException.class, () -> ExecutionBackend.fromId("docker"));
    assertThrows(IllegalArgumentException.class, () -> ExecutionBackend.fromId(""));
  }

  @Test
  void testFromIdNull() {
    assertThrows(IllegalArgumentException.class, () -> ExecutionBackend.fromId(null));
  }

  @Test
  void testAllBackendsHaveUniqueIds() {
    ExecutionBackend[] backends = ExecutionBackend.values();
    for (int i = 0; i < backends.length; i++) {
      for (int j = i + 1; j < backends.length; j++) {
        assertNotEquals(backends[i].getId(), backends[j].getId(), "Backend IDs must be unique");
      }
    }
  }

  @Test
  void testAllBackendsHaveDisplayNames() {
    for (ExecutionBackend backend : ExecutionBackend.values()) {
      assertNotNull(backend.getDisplayName());
      assertFalse(backend.getDisplayName().isEmpty());
    }
  }
}
