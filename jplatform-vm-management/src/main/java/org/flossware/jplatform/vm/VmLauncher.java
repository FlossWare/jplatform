package org.flossware.jplatform.vm;

import org.flossware.jplatform.api.ApplicationDescriptor;
import org.libvirt.Connect;
import org.libvirt.Domain;
import org.libvirt.LibvirtException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Launches and manages virtual machines via libvirt/KVM/QEMU.
 *
 * <p>Enables JPlatform to manage VMs alongside containers, Java apps, and native binaries,
 * providing unified orchestration across all workload types.</p>
 *
 * <p>VM Configuration via Properties:</p>
 * <pre>
 * vm.name = vm-name (optional, defaults to applicationId)
 * vm.vcpu = 4 (number of virtual CPUs)
 * vm.memory = 8192 (RAM in MB)
 * vm.disk = /path/to/disk.qcow2 (existing disk image)
 * vm.disk.size = 50G (for creating new disk)
 * vm.disk.format = qcow2|raw (default: qcow2)
 * vm.image = ubuntu-22.04 (cloud image to use)
 * vm.network = bridge|nat|none (default: bridge)
 * vm.bridge = virbr0 (bridge name for bridge network)
 * vm.vnc.enabled = true|false (enable VNC console)
 * vm.vnc.port = 5900 (VNC port, auto if not specified)
 * </pre>
 *
 * <p>Example Deployment:</p>
 * <pre>
 * ApplicationDescriptor vmDescriptor = ApplicationDescriptor.builder()
 *     .applicationId("database-vm")
 *     .name("PostgreSQL Database VM")
 *     .property("vm.vcpu", "8")
 *     .property("vm.memory", "32768")
 *     .property("vm.disk", "/var/lib/jplatform/vms/db.qcow2")
 *     .property("vm.network", "bridge")
 *     .dependency("storage-vm")  // VM dependencies work!
 *     .build();
 *
 * manager.deploy(vmDescriptor);
 * manager.start("database-vm");
 * </pre>
 *
 * @since 2.1
 * @author FlossWare
 */
public class VmLauncher {

    private static final Logger logger = LoggerFactory.getLogger(VmLauncher.class);

    private final Connect connection;

    /**
     * Creates a new VmLauncher connected to the local libvirt daemon.
     *
     * @throws LibvirtException if connection to libvirt fails
     */
    public VmLauncher() throws LibvirtException {
        this("qemu:///system");
    }

    /**
     * Creates a new VmLauncher with custom libvirt URI.
     *
     * @param libvirtUri the libvirt connection URI (e.g., "qemu:///system", "qemu+ssh://host/system")
     * @throws LibvirtException if connection to libvirt fails
     */
    public VmLauncher(String libvirtUri) throws LibvirtException {
        this.connection = new Connect(libvirtUri, false);
        logger.info("Connected to libvirt URI: {} (version: {})", libvirtUri, connection.getLibVirVersion());
    }

    /**
     * Launches a virtual machine.
     *
     * @param applicationId the application identifier
     * @param descriptor the application descriptor containing VM configuration
     * @return VmInfo containing domain and metadata
     * @throws LibvirtException if VM creation fails
     */
    public VmInfo launch(String applicationId, ApplicationDescriptor descriptor) throws LibvirtException {
        Map<String, String> properties = descriptor.getProperties();

        String vmName = properties.getOrDefault("vm.name", applicationId);
        int vcpu = Integer.parseInt(properties.getOrDefault("vm.vcpu", "2"));
        int memoryMB = Integer.parseInt(properties.getOrDefault("vm.memory", "4096"));
        String diskPath = properties.get("vm.disk");
        String networkMode = properties.getOrDefault("vm.network", "bridge");

        logger.info("[{}] Creating VM: {} (vCPU: {}, RAM: {}MB, disk: {})",
            applicationId, vmName, vcpu, memoryMB, diskPath);

        // Validate configuration
        if (diskPath == null || diskPath.isEmpty()) {
            throw new IllegalArgumentException("vm.disk property is required");
        }

        // Build libvirt XML configuration
        String xmlConfig = buildVmXml(vmName, vcpu, memoryMB, diskPath, networkMode, properties);

        logger.debug("[{}] VM XML configuration:\n{}", applicationId, xmlConfig);

        // Create and start domain
        Domain domain = connection.domainDefineXML(xmlConfig);
        domain.create();

        int domainId = domain.getID();
        String uuid = domain.getUUIDString();

        logger.info("[{}] VM started successfully. Domain ID: {}, UUID: {}", applicationId, domainId, uuid);

        return new VmInfo(domain, vmName, vcpu, memoryMB, uuid);
    }

    /**
     * Stops a virtual machine.
     *
     * @param applicationId the application identifier (for logging)
     * @param vmInfo the VM information
     * @param graceful true for graceful shutdown (ACPI), false for force destroy
     * @throws LibvirtException if shutdown fails
     */
    public void stop(String applicationId, VmInfo vmInfo, boolean graceful) throws LibvirtException {
        Domain domain = vmInfo.getDomain();

        if (graceful) {
            logger.info("[{}] Gracefully shutting down VM: {} (ACPI shutdown)", applicationId, vmInfo.getName());
            domain.shutdown();
        } else {
            logger.info("[{}] Force destroying VM: {}", applicationId, vmInfo.getName());
            domain.destroy();
        }
    }

    /**
     * Pauses (suspends) a running VM.
     *
     * @param applicationId the application identifier
     * @param vmInfo the VM information
     * @throws LibvirtException if pause fails
     */
    public void pause(String applicationId, VmInfo vmInfo) throws LibvirtException {
        logger.info("[{}] Pausing VM: {}", applicationId, vmInfo.getName());
        vmInfo.getDomain().suspend();
    }

    /**
     * Resumes a paused VM.
     *
     * @param applicationId the application identifier
     * @param vmInfo the VM information
     * @throws LibvirtException if resume fails
     */
    public void resume(String applicationId, VmInfo vmInfo) throws LibvirtException {
        logger.info("[{}] Resuming VM: {}", applicationId, vmInfo.getName());
        vmInfo.getDomain().resume();
    }

    /**
     * Undefines (removes) a VM definition.
     *
     * @param applicationId the application identifier
     * @param vmInfo the VM information
     * @throws LibvirtException if undefine fails
     */
    public void undefine(String applicationId, VmInfo vmInfo) throws LibvirtException {
        logger.info("[{}] Undefining VM: {}", applicationId, vmInfo.getName());
        Domain domain = vmInfo.getDomain();

        // Stop if running
        if (domain.isActive() == 1) {
            logger.debug("[{}] VM is active, destroying before undefine", applicationId);
            domain.destroy();
        }

        // Undefine
        domain.undefine();
        logger.info("[{}] VM undefined successfully", applicationId);
    }

    /**
     * Gets VM resource usage statistics.
     *
     * @param vmInfo the VM information
     * @return VmStats with current resource usage
     * @throws LibvirtException if stats retrieval fails
     */
    public VmStats getStats(VmInfo vmInfo) throws LibvirtException {
        Domain domain = vmInfo.getDomain();

        // Get domain info (memory, CPU count, state)
        org.libvirt.DomainInfo info = domain.getInfo();

        long memoryKB = info.memory;
        long maxMemoryKB = info.maxMem;
        int nrVirtCpu = info.nrVirtCpu;
        long cpuTimeNs = info.cpuTime;

        // State
        org.libvirt.DomainInfo.DomainState state = info.state;
        String stateStr = state.toString();

        return new VmStats(
            memoryKB / 1024,           // Convert to MB
            maxMemoryKB / 1024,         // Convert to MB
            nrVirtCpu,
            cpuTimeNs,
            stateStr
        );
    }

    /**
     * Builds libvirt XML configuration for the VM.
     *
     * @param name VM name
     * @param vcpu number of virtual CPUs
     * @param memoryMB memory in MB
     * @param diskPath path to disk image
     * @param networkMode network mode (bridge, nat, none)
     * @param properties additional VM properties
     * @return libvirt XML configuration string
     */
    private String buildVmXml(String name, int vcpu, int memoryMB, String diskPath,
                              String networkMode, Map<String, String> properties) {

        long memoryKB = memoryMB * 1024L;
        String diskFormat = properties.getOrDefault("vm.disk.format", "qcow2");

        StringBuilder xml = new StringBuilder();
        xml.append("<domain type='kvm'>\n");
        xml.append("  <name>").append(escapeXml(name)).append("</name>\n");
        xml.append("  <memory unit='KiB'>").append(memoryKB).append("</memory>\n");
        xml.append("  <currentMemory unit='KiB'>").append(memoryKB).append("</currentMemory>\n");
        xml.append("  <vcpu placement='static'>").append(vcpu).append("</vcpu>\n");

        // OS configuration
        xml.append("  <os>\n");
        xml.append("    <type arch='x86_64' machine='pc'>hvm</type>\n");
        xml.append("    <boot dev='hd'/>\n");
        xml.append("  </os>\n");

        // Features (ACPI, APIC for modern OS support)
        xml.append("  <features>\n");
        xml.append("    <acpi/>\n");
        xml.append("    <apic/>\n");
        xml.append("  </features>\n");

        // CPU mode (host-passthrough for best performance)
        xml.append("  <cpu mode='host-passthrough'/>\n");

        // Clock
        xml.append("  <clock offset='utc'>\n");
        xml.append("    <timer name='rtc' tickpolicy='catchup'/>\n");
        xml.append("    <timer name='pit' tickpolicy='delay'/>\n");
        xml.append("    <timer name='hpet' present='no'/>\n");
        xml.append("  </clock>\n");

        // Power management
        xml.append("  <on_poweroff>destroy</on_poweroff>\n");
        xml.append("  <on_reboot>restart</on_reboot>\n");
        xml.append("  <on_crash>destroy</on_crash>\n");

        // Devices
        xml.append("  <devices>\n");

        // Disk
        xml.append("    <disk type='file' device='disk'>\n");
        xml.append("      <driver name='qemu' type='").append(diskFormat).append("' cache='writeback'/>\n");
        xml.append("      <source file='").append(escapeXml(diskPath)).append("'/>\n");
        xml.append("      <target dev='vda' bus='virtio'/>\n");
        xml.append("    </disk>\n");

        // Network interface
        if (!"none".equals(networkMode)) {
            if ("bridge".equals(networkMode)) {
                String bridge = properties.getOrDefault("vm.bridge", "virbr0");
                xml.append("    <interface type='bridge'>\n");
                xml.append("      <source bridge='").append(escapeXml(bridge)).append("'/>\n");
                xml.append("      <model type='virtio'/>\n");
                xml.append("    </interface>\n");
            } else if ("nat".equals(networkMode)) {
                xml.append("    <interface type='network'>\n");
                xml.append("      <source network='default'/>\n");
                xml.append("      <model type='virtio'/>\n");
                xml.append("    </interface>\n");
            }
        }

        // Serial console
        xml.append("    <serial type='pty'>\n");
        xml.append("      <target type='isa-serial' port='0'/>\n");
        xml.append("    </serial>\n");
        xml.append("    <console type='pty'>\n");
        xml.append("      <target type='serial' port='0'/>\n");
        xml.append("    </console>\n");

        // VNC graphics (if enabled)
        if ("true".equals(properties.get("vm.vnc.enabled"))) {
            String vncPort = properties.getOrDefault("vm.vnc.port", "-1"); // -1 = auto
            xml.append("    <graphics type='vnc' port='").append(vncPort).append("' autoport='yes' listen='0.0.0.0'>\n");
            xml.append("      <listen type='address' address='0.0.0.0'/>\n");
            xml.append("    </graphics>\n");

            // Video device for VNC
            xml.append("    <video>\n");
            xml.append("      <model type='vga' vram='16384' heads='1'/>\n");
            xml.append("    </video>\n");
        }

        xml.append("  </devices>\n");
        xml.append("</domain>\n");

        return xml.toString();
    }

    /**
     * Escapes XML special characters.
     */
    private String escapeXml(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&apos;");
    }

    /**
     * Closes the libvirt connection.
     *
     * @throws LibvirtException if close fails
     */
    public void close() throws LibvirtException {
        if (connection != null) {
            logger.info("Closing libvirt connection");
            connection.close();
        }
    }

    /**
     * Container for VM information.
     */
    public static class VmInfo {
        private final Domain domain;
        private final String name;
        private final int vcpu;
        private final int memoryMB;
        private final String uuid;

        public VmInfo(Domain domain, String name, int vcpu, int memoryMB, String uuid) {
            this.domain = domain;
            this.name = name;
            this.vcpu = vcpu;
            this.memoryMB = memoryMB;
            this.uuid = uuid;
        }

        public Domain getDomain() { return domain; }
        public String getName() { return name; }
        public int getVcpu() { return vcpu; }
        public int getMemoryMB() { return memoryMB; }
        public String getUuid() { return uuid; }
    }

    /**
     * Container for VM statistics.
     */
    public static class VmStats {
        private final long memoryMB;
        private final long maxMemoryMB;
        private final int vcpu;
        private final long cpuTimeNs;
        private final String state;

        public VmStats(long memoryMB, long maxMemoryMB, int vcpu, long cpuTimeNs, String state) {
            this.memoryMB = memoryMB;
            this.maxMemoryMB = maxMemoryMB;
            this.vcpu = vcpu;
            this.cpuTimeNs = cpuTimeNs;
            this.state = state;
        }

        public long getMemoryMB() { return memoryMB; }
        public long getMaxMemoryMB() { return maxMemoryMB; }
        public int getVcpu() { return vcpu; }
        public long getCpuTimeNs() { return cpuTimeNs; }
        public String getState() { return state; }

        public double getCpuTimeSeconds() {
            return cpuTimeNs / 1_000_000_000.0;
        }
    }
}
