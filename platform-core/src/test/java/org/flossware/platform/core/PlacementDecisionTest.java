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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for PlacementDecision.
 */
class PlacementDecisionTest {

    @Test
    void testConstructorWithoutApplicationId() {
        PlacementDecision decision = new PlacementDecision(
                ExecutionBackend.IN_JVM,
                "Lightweight Java app"
        );

        assertNull(decision.getApplicationId());
        assertEquals(ExecutionBackend.IN_JVM, decision.getBackend());
        assertEquals("Lightweight Java app", decision.getReason());
        assertTrue(decision.getTimestamp() > 0);
    }

    @Test
    void testConstructorWithApplicationId() {
        PlacementDecision decision = new PlacementDecision(
                "auth-service",
                ExecutionBackend.CONTAINER,
                "Heavy Java app"
        );

        assertEquals("auth-service", decision.getApplicationId());
        assertEquals(ExecutionBackend.CONTAINER, decision.getBackend());
        assertEquals("Heavy Java app", decision.getReason());
        assertTrue(decision.getTimestamp() > 0);
    }

    @Test
    void testConstructorWithExplicitTimestamp() {
        long timestamp = 1234567890L;
        PlacementDecision decision = new PlacementDecision(
                "test-app",
                ExecutionBackend.VIRTUAL_MACHINE,
                "Kernel access required",
                timestamp
        );

        assertEquals("test-app", decision.getApplicationId());
        assertEquals(ExecutionBackend.VIRTUAL_MACHINE, decision.getBackend());
        assertEquals("Kernel access required", decision.getReason());
        assertEquals(timestamp, decision.getTimestamp());
    }

    @Test
    void testConstructorNullBackend() {
        assertThrows(NullPointerException.class, () ->
                new PlacementDecision(null, "reason")
        );
    }

    @Test
    void testConstructorNullReason() {
        assertThrows(NullPointerException.class, () ->
                new PlacementDecision(ExecutionBackend.IN_JVM, null)
        );
    }

    @Test
    void testConstructorNegativeTimestamp() {
        assertThrows(IllegalArgumentException.class, () ->
                new PlacementDecision("app", ExecutionBackend.IN_JVM, "reason", -1)
        );
    }

    @Test
    void testEquality() {
        long timestamp = System.currentTimeMillis();
        PlacementDecision decision1 = new PlacementDecision(
                "app1", ExecutionBackend.IN_JVM, "reason", timestamp
        );
        PlacementDecision decision2 = new PlacementDecision(
                "app1", ExecutionBackend.IN_JVM, "reason", timestamp
        );

        assertEquals(decision1, decision2);
        assertEquals(decision1.hashCode(), decision2.hashCode());
    }

    @Test
    void testInequality() {
        long timestamp = System.currentTimeMillis();

        PlacementDecision decision1 = new PlacementDecision(
                "app1", ExecutionBackend.IN_JVM, "reason", timestamp
        );
        PlacementDecision decision2 = new PlacementDecision(
                "app2", ExecutionBackend.IN_JVM, "reason", timestamp
        );
        PlacementDecision decision3 = new PlacementDecision(
                "app1", ExecutionBackend.CONTAINER, "reason", timestamp
        );

        assertNotEquals(decision1, decision2);
        assertNotEquals(decision1, decision3);
    }

    @Test
    void testToStringWithoutApplicationId() {
        PlacementDecision decision = new PlacementDecision(
                ExecutionBackend.IN_JVM,
                "Lightweight Java app"
        );

        String str = decision.toString();
        assertTrue(str.contains("backend=in-jvm"));
        assertTrue(str.contains("reason='Lightweight Java app'"));
        assertFalse(str.contains("app="));
    }

    @Test
    void testToStringWithApplicationId() {
        PlacementDecision decision = new PlacementDecision(
                "auth-service",
                ExecutionBackend.CONTAINER,
                "Heavy Java app"
        );

        String str = decision.toString();
        assertTrue(str.contains("app=auth-service"));
        assertTrue(str.contains("backend=container"));
        assertTrue(str.contains("reason='Heavy Java app'"));
    }
}
