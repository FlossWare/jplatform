package org.flossware.jplatform.security;

import org.flossware.jplatform.api.SecurityPolicy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.FilePermission;
import java.net.SocketPermission;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.Permission;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for SecurityEnforcer.
 */
class SecurityEnforcerTest {

    private SecurityEnforcer enforcer;
    private URLClassLoader testClassLoader;
    private MockSecurityPolicy mockPolicy;

    @BeforeEach
    void setUp() throws Exception {
        enforcer = SecurityEnforcer.getInstance();
        enforcer.clearPolicies(); // Clear any previous policies
        enforcer.setEnabled(true); // Enable enforcement for testing

        // Create test classloader
        testClassLoader = new URLClassLoader(
                new URL[]{},
                SecurityEnforcerTest.class.getClassLoader()
        );

        // Create mock policy
        mockPolicy = new MockSecurityPolicy();
    }

    @AfterEach
    void tearDown() throws Exception {
        enforcer.clearPolicies();
        enforcer.setEnabled(false); // Disable to not affect other tests
        if (testClassLoader != null) {
            testClassLoader.close();
        }
    }

    @Test
    void testGetInstance() {
        assertNotNull(SecurityEnforcer.getInstance());
        assertSame(SecurityEnforcer.getInstance(), SecurityEnforcer.getInstance(),
                "getInstance should return same instance");
    }

    @Test
    void testEnableDisable() {
        enforcer.setEnabled(false);
        assertFalse(enforcer.isEnabled());

        enforcer.setEnabled(true);
        assertTrue(enforcer.isEnabled());
    }

    @Test
    void testRegisterPolicy() {
        enforcer.registerPolicy(testClassLoader, mockPolicy);
        assertEquals(1, enforcer.getPolicyCount());

        SecurityPolicy retrieved = enforcer.getPolicy(testClassLoader);
        assertSame(mockPolicy, retrieved);
    }

    @Test
    void testUnregisterPolicy() {
        enforcer.registerPolicy(testClassLoader, mockPolicy);
        assertEquals(1, enforcer.getPolicyCount());

        enforcer.unregisterPolicy(testClassLoader);
        assertEquals(0, enforcer.getPolicyCount());

        assertNull(enforcer.getPolicy(testClassLoader));
    }

    @Test
    void testCheckFileAccessWhenDisabled() {
        enforcer.setEnabled(false);

        // Should not throw when disabled
        assertDoesNotThrow(() ->
                enforcer.checkFileAccess("/tmp/test.txt", "read")
        );
    }

    @Test
    void testCheckFileAccessWhenEnabled() {
        enforcer.setEnabled(true);

        // Without registered policy, should allow (platform code)
        assertDoesNotThrow(() ->
                enforcer.checkFileAccess("/tmp/test.txt", "read")
        );
    }

    @Test
    void testCheckSocketAccessWhenDisabled() {
        enforcer.setEnabled(false);

        assertDoesNotThrow(() ->
                enforcer.checkSocketAccess("example.com", 80, "connect")
        );
    }

    @Test
    void testCheckSocketAccessWhenEnabled() {
        enforcer.setEnabled(true);

        // Without registered policy, should allow (platform code)
        assertDoesNotThrow(() ->
                enforcer.checkSocketAccess("example.com", 80, "connect")
        );
    }

    @Test
    void testCheckReflectionAccessWhenDisabled() {
        enforcer.setEnabled(false);

        assertDoesNotThrow(() ->
                enforcer.checkReflectionAccess()
        );
    }

    @Test
    void testCheckReflectionAccessWhenEnabled() {
        enforcer.setEnabled(true);

        // Without registered policy, should allow (platform code)
        assertDoesNotThrow(() ->
                enforcer.checkReflectionAccess()
        );
    }

    @Test
    void testCheckNativeAccessWhenDisabled() {
        enforcer.setEnabled(false);

        assertDoesNotThrow(() ->
                enforcer.checkNativeAccess("mylib")
        );
    }

    @Test
    void testCheckNativeAccessWhenEnabled() {
        enforcer.setEnabled(true);

        // Without registered policy, should allow (platform code)
        assertDoesNotThrow(() ->
                enforcer.checkNativeAccess("mylib")
        );
    }

    @Test
    void testEnforcePermissionWithDenyingPolicy() {
        // Create a denying policy
        mockPolicy.setShouldDeny(true);
        enforcer.registerPolicy(testClassLoader, mockPolicy);
        enforcer.setEnabled(true);

        // This should NOT throw because the caller (test class) is not loaded
        // by testClassLoader - it's loaded by the test framework's classloader
        assertDoesNotThrow(() ->
                enforcer.enforcePermission(new FilePermission("/tmp/test.txt", "read"))
        );
    }

    @Test
    void testClearPolicies() {
        enforcer.registerPolicy(testClassLoader, mockPolicy);
        assertEquals(1, enforcer.getPolicyCount());

        enforcer.clearPolicies();
        assertEquals(0, enforcer.getPolicyCount());
    }

    @Test
    void testMultiplePolicies() throws Exception {
        URLClassLoader classLoader2 = new URLClassLoader(
                new URL[]{},
                SecurityEnforcerTest.class.getClassLoader()
        );

        try {
            MockSecurityPolicy policy2 = new MockSecurityPolicy();

            enforcer.registerPolicy(testClassLoader, mockPolicy);
            enforcer.registerPolicy(classLoader2, policy2);

            assertEquals(2, enforcer.getPolicyCount());

            assertSame(mockPolicy, enforcer.getPolicy(testClassLoader));
            assertSame(policy2, enforcer.getPolicy(classLoader2));
        } finally {
            classLoader2.close();
        }
    }

    @Test
    void testGetPolicyWithNullClassLoader() {
        assertNull(enforcer.getPolicy(null));
    }

    /**
     * Mock SecurityPolicy for testing.
     */
    private static class MockSecurityPolicy implements SecurityPolicy {
        private boolean shouldDeny = false;
        private final Set<Permission> granted = new HashSet<>();

        public void setShouldDeny(boolean shouldDeny) {
            this.shouldDeny = shouldDeny;
        }

        public void addGranted(Permission permission) {
            granted.add(permission);
        }

        @Override
        public boolean checkPermission(Permission permission) {
            if (shouldDeny) {
                return false;
            }
            return granted.isEmpty() || granted.stream()
                    .anyMatch(p -> p.implies(permission));
        }

        @Override
        public void enforce(Permission permission) throws SecurityException {
            if (!checkPermission(permission)) {
                throw new SecurityException("Permission denied: " + permission);
            }
        }

        @Override
        public Set<Permission> getGrantedPermissions() {
            return Collections.unmodifiableSet(granted);
        }
    }
}
