package com.diskpilot.service;

import com.diskpilot.model.DriveInfo;

import java.io.File;
import java.nio.file.FileStore;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SystemDriveService {

    public List<DriveInfo> getAvailableDrives() {
        List<DriveInfo> drives = new ArrayList<>();
        File[] roots = File.listRoots();
        if (roots == null) return drives;

        for (File root : roots) {
            try {
                Path rootPath = root.toPath();
                FileStore store = Files.getFileStore(rootPath);
                long total = store.getTotalSpace();
                long free = store.getUsableSpace();
                String type = store.type();
                String name = store.name();
                if (name == null || name.isBlank()) {
                    name = root.getPath();
                } else {
                    name = name + " (" + root.getPath().replace("\\", "") + ")";
                }

                drives.add(new DriveInfo(root.getPath(), name, type, total, free, true));
            } catch (Exception e) {

                long total = root.getTotalSpace();
                long free = root.getFreeSpace();
                boolean ready = total > 0;
                drives.add(new DriveInfo(root.getPath(), "Drive (" + root.getPath() + ")", "Unknown", total, free, ready));
            }
        }
        return Collections.unmodifiableList(drives);
    }

    public long getTotalCapacity() {
        long total = 0;
        for (DriveInfo d : getAvailableDrives()) {
            if (d.isReady()) {
                total += d.getTotalSpace();
            }
        }
        return total;
    }

    public long getTotalUsedSpace() {
        long used = 0;
        for (DriveInfo d : getAvailableDrives()) {
            if (d.isReady()) {
                used += d.getUsedSpace();
            }
        }
        return used;
    }

    public long getTotalFreeSpace() {
        long free = 0;
        for (DriveInfo d : getAvailableDrives()) {
            if (d.isReady()) {
                free += d.getFreeSpace();
            }
        }
        return free;
    }
}

