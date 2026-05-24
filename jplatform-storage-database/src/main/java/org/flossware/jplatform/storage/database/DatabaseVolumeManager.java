package org.flossware.jplatform.storage.database;

import org.flossware.jplatform.api.*;
import java.nio.file.*;
import java.io.IOException;
import java.util.*;

/**
 * Database implementation of VolumeManager.
 * @since 1.1
 */
public class DatabaseVolumeManager implements VolumeManager {
    private final Map<String, VolumeMount> volumes = new HashMap<>();
    
    public DatabaseVolumeManager(String appId, List<VolumeMount> mounts) {
        mounts.forEach(v -> volumes.put(v.getName(), v));
    }
    
    @Override
    public Path getVolumePath(String volumeName) { return Paths.get("/tmp/" + volumeName); }
    
    @Override
    public List<VolumeMount> getVolumes() { return new ArrayList<>(volumes.values()); }
    
    @Override
    public long getVolumeUsageBytes(String volumeName) throws IOException { return 0; }
    
    @Override
    public boolean volumeExists(String volumeName) { return volumes.containsKey(volumeName); }
    
    @Override
    public long getVolumeSizeLimit(String volumeName) { 
        VolumeMount v = volumes.get(volumeName);
        return v != null ? v.getMaxSizeMB() * 1024 * 1024 : 0;
    }
    
    @Override
    public boolean isPersistent(String volumeName) {
        VolumeMount v = volumes.get(volumeName);
        return v != null && v.isPersistent();
    }
}
