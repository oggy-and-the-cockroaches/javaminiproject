package com.diskpilot.service;

import com.diskpilot.model.DriveInfo;
import com.diskpilot.model.FileCategory;
import com.diskpilot.model.ScanResult;

import java.nio.file.Path;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class StorageAnalyzer {

    private final SystemDriveService driveService;
    private final FileScanner fileScanner;

    public StorageAnalyzer() {
        this.driveService = new SystemDriveService();
        this.fileScanner = new FileScanner();
    }

    public List<DriveInfo> getSystemDrives() {
        return driveService.getAvailableDrives();
    }

    public long getTotalStorage() {
        return driveService.getTotalCapacity();
    }

    public long getTotalUsedStorage() {
        return driveService.getTotalUsedSpace();
    }

    public long getTotalFreeStorage() {
        return driveService.getTotalFreeSpace();
    }

    public double getStorageUtilizationPercent() {
        long total = getTotalStorage();
        if (total <= 0) return 0.0;
        return ((double) getTotalUsedStorage() / total) * 100.0;
    }

    public ScanResult analyzeDirectory(Path targetPath, boolean recursive, FileScanner.ScanProgressListener listener) {
        return fileScanner.scan(targetPath, recursive, listener);
    }

    public void cancelScan() {
        fileScanner.cancel();
    }

    public boolean isScanCancelled() {
        return fileScanner.isCancelled();
    }
}

