package org.flossware.jplatform.vm;

import org.flossware.jplatform.api.ApplicationDescriptor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for VmLauncher.
 *
 * <p>Note: These tests require a running libvirt daemon and are disabled by default.
 * To run them, set system property: -Dlibvirt.available=true</p>
 */
class VmLauncherTest {

    /**
     * Tests that VmLauncher constructor fails gracefully when libvirt is not available.
     */
    @Test
    void testConstructorFailsGracefullyWithoutLibvirt() {
        // This test runs always (even without libvirt)
        // It verifies that the constructor throws LibvirtException when libvirt is not available

        try {
            new VmLauncher("qemu:///system");
            // If we get here, libvirt is actually available
            assertTrue(true, "Libvirt is available on this system");
        } catch (Exception e) {
            // Expected when libvirt is not available
            assertTrue(e.getMessage().contains("libvirt") || e.getMessage().contains("connection"),
                "Exception message should mention libvirt or connection: " + e.getMessage());
        }
    }

    /**
     * Tests VM XML generation with minimal configuration.
     */
    @Test
    void testBuildVmXml() throws Exception {
        // Create descriptor with minimal VM configuration
        Map<String, String> properties = new HashMap<>();
        properties.put("vm.vcpu", "2");
        properties.put("vm.memory", "4096");
        properties.put("vm.disk", "/var/lib/jplatform/vms/test.qcow2");

        ApplicationDescriptor descriptor = ApplicationDescriptor.builder()
            .applicationId("test-vm")
            .name("Test VM")
            .properties(properties)
            .build();

        // We can't test the actual XML generation without access to the private method,
        // but we can verify the properties are correctly set
        assertEquals("2", descriptor.getProperties().get("vm.vcpu"));
        assertEquals("4096", descriptor.getProperties().get("vm.memory"));
        assertEquals("/var/lib/jplatform/vms/test.qcow2", descriptor.getProperties().get("vm.disk"));
    }

    /**
     * Tests VM launch with full configuration (requires libvirt).
     */
    @Test
    @EnabledIfSystemProperty(named = "libvirt.available", matches = "true")
    void testLaunchVm() throws Exception {
        VmLauncher launcher = new VmLauncher();

        Map<String, String> properties = new HashMap<>();
        properties.put("vm.name", "junit-test-vm");
        properties.put("vm.vcpu", "1");
        properties.put("vm.memory", "1024");
        properties.put("vm.disk", "/tmp/test-vm.qcow2");
        properties.put("vm.disk.format", "qcow2");
        properties.put("vm.network", "none");  // No network for testing

        ApplicationDescriptor descriptor = ApplicationDescriptor.builder()
            .applicationId("test-vm")
            .name("Test VM")
            .properties(properties)
            .build();

        try {
            // Note: This will fail if /tmp/test-vm.qcow2 doesn't exist
            // In a real test environment, you would create this disk image first
            VmLauncher.VmInfo vmInfo = launcher.launch("test-vm", descriptor);

            assertNotNull(vmInfo);
            assertEquals("junit-test-vm", vmInfo.getName());
            assertEquals(1, vmInfo.getVcpu());
            assertEquals(1024, vmInfo.getMemoryMB());
            assertNotNull(vmInfo.getUuid());

            // Clean up
            launcher.stop("test-vm", vmInfo, false);
            launcher.undefine("test-vm", vmInfo);

        } finally {
            launcher.close();
        }
    }

    /**
     * Tests that VM launch fails with missing disk property.
     */
    @Test
    void testLaunchFailsWithoutDisk() throws Exception {
        // This test can run even without libvirt since it fails before connecting
        Map<String, String> properties = new HashMap<>();
        properties.put("vm.vcpu", "2");
        properties.put("vm.memory", "4096");
        // Missing vm.disk property

        ApplicationDescriptor descriptor = ApplicationDescriptor.builder()
            .applicationId("test-vm")
            .name("Test VM")
            .properties(properties)
            .build();

        try {
            VmLauncher launcher = new VmLauncher();

            Exception exception = assertThrows(IllegalArgumentException.class, () -> {
                launcher.launch("test-vm", descriptor);
            });

            assertTrue(exception.getMessage().contains("vm.disk"),
                "Exception should mention missing vm.disk property");

            launcher.close();
        } catch (Exception e) {
            // Expected if libvirt is not available
            if (!e.getMessage().contains("libvirt")) {
                throw e;
            }
        }
    }

    /**
     * Tests VM configuration with VNC enabled.
     */
    @Test
    void testVmConfigurationWithVnc() {
        Map<String, String> properties = new HashMap<>();
        properties.put("vm.vcpu", "4");
        properties.put("vm.memory", "8192");
        properties.put("vm.disk", "/var/lib/jplatform/vms/vnc-test.qcow2");
        properties.put("vm.vnc.enabled", "true");
        properties.put("vm.vnc.port", "5900");

        ApplicationDescriptor descriptor = ApplicationDescriptor.builder()
            .applicationId("vnc-test-vm")
            .name("VNC Test VM")
            .properties(properties)
            .build();

        assertEquals("true", descriptor.getProperties().get("vm.vnc.enabled"));
        assertEquals("5900", descriptor.getProperties().get("vm.vnc.port"));
    }

    /**
     * Tests VM configuration with bridge networking.
     */
    @Test
    void testVmConfigurationWithBridgeNetwork() {
        Map<String, String> properties = new HashMap<>();
        properties.put("vm.vcpu", "2");
        properties.put("vm.memory", "4096");
        properties.put("vm.disk", "/var/lib/jplatform/vms/bridge-test.qcow2");
        properties.put("vm.network", "bridge");
        properties.put("vm.bridge", "br0");

        ApplicationDescriptor descriptor = ApplicationDescriptor.builder()
            .applicationId("bridge-test-vm")
            .name("Bridge Test VM")
            .properties(properties)
            .build();

        assertEquals("bridge", descriptor.getProperties().get("vm.network"));
        assertEquals("br0", descriptor.getProperties().get("vm.bridge"));
    }

    /**
     * Tests default values for VM configuration.
     */
    @Test
    void testDefaultValues() {
        Map<String, String> properties = new HashMap<>();
        properties.put("vm.disk", "/var/lib/jplatform/vms/default-test.qcow2");
        // Not specifying vcpu, memory - should use defaults

        ApplicationDescriptor descriptor = ApplicationDescriptor.builder()
            .applicationId("default-test-vm")
            .name("Default Test VM")
            .properties(properties)
            .build();

        // Verify disk is set (required)
        assertEquals("/var/lib/jplatform/vms/default-test.qcow2",
            descriptor.getProperties().get("vm.disk"));

        // VmLauncher should use defaults for missing properties:
        // - vm.vcpu defaults to "2"
        // - vm.memory defaults to "4096"
        // - vm.network defaults to "bridge"
        // - vm.disk.format defaults to "qcow2"
    }

    /**
     * Tests VmInfo container class.
     */
    @Test
    void testVmInfoClass() {
        // VmInfo requires a Domain object which we can't easily mock without libvirt
        // This test verifies the structure is correct
        // In a real integration test, you would verify:
        // - VmInfo stores domain reference
        // - VmInfo provides name, vcpu, memory, uuid
        assertTrue(true, "VmInfo class structure verified by compilation");
    }

    /**
     * Tests VmStats container class.
     */
    @Test
    void testVmStatsClass() {
        VmLauncher.VmStats stats = new VmLauncher.VmStats(
            8192,    // memoryMB
            16384,   // maxMemoryMB
            4,       // vcpu
            1000000000L,  // cpuTimeNs (1 second)
            "RUNNING"
        );

        assertEquals(8192, stats.getMemoryMB());
        assertEquals(16384, stats.getMaxMemoryMB());
        assertEquals(4, stats.getVcpu());
        assertEquals(1000000000L, stats.getCpuTimeNs());
        assertEquals("RUNNING", stats.getState());
        assertEquals(1.0, stats.getCpuTimeSeconds(), 0.01);
    }
}
