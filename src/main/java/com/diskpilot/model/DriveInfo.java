package com.diskpilot.model;

import java.nio.file.FileStore;
import java.nio.file.Path;

public class DriveInfo {
    private final String rootPath;
    private final String displayName;
    private final String fileSystemType;
    private final long totalSpace;
    private final long freeSpace;
    private final long usedSpace;
    private final double usagePercentage;
    private final boolean isReady;

    public DriveInfo(String rootPath, String displayName, String fileSystemType, long totalSpace, long freeSpace, boolean isReady) {
        this.rootPath = rootPath;
        this.displayName = displayName;
        this.fileSystemType = fileSystemType;
        this.totalSpace = Math.max(0, totalSpace);
        this.freeSpace = Math.max(0, freeSpace);
        this.usedSpace = Math.max(0, this.totalSpace - this.freeSpace);
        this.usagePercentage = this.totalSpace > 0 ? ((double) this.usedSpace / this.totalSpace) * 100.0 : 0.0;
        this.isReady = isReady;
    }

    public String getRootPath() {
        return rootPath;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getFileSystemType() {
        return fileSystemType;
    }

    public long getTotalSpace() {
        return totalSpace;
    }

    public long getFreeSpace() {
        return freeSpace;
    }

    public long getUsedSpace() {
        return usedSpace;
    }

    public double getUsagePercentage() {
        return usagePercentage;
    }

    public boolean isReady() {
        return isReady;
    }

    @Override
    public String toString() {
        return displayName + " (" + rootPath + ") - " + String.format("%.1f%% used", usagePercentage);
    }
}

