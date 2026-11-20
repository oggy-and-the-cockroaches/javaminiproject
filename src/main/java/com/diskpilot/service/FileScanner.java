package com.diskpilot.service;

import com.diskpilot.model.FileCategory;
import com.diskpilot.model.FileRecord;
import com.diskpilot.model.FolderRecord;
import com.diskpilot.model.ScanResult;
import com.diskpilot.util.FileUtils;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class FileScanner {

    public interface ScanProgressListener {
        void onProgress(long filesScanned, long bytesScanned, Path currentPath);
    }

    private final AtomicBoolean cancelRequested = new AtomicBoolean(false);

    public void cancel() {
        cancelRequested.set(true);
    }

    public boolean isCancelled() {
        return cancelRequested.get();
    }

    public ScanResult scan(Path targetPath, boolean recursive, ScanProgressListener listener) {
        cancelRequested.set(false);
        ScanResult result = new ScanResult(targetPath);
        long startTime = System.currentTimeMillis();

        if (targetPath == null || !Files.exists(targetPath)) {
            result.addError("Target path does not exist.");
            return result;
        }

        Map<Path, FolderRecord> folderMap = new HashMap<>();
        FolderRecord rootFolder = new FolderRecord(targetPath);
        folderMap.put(targetPath, rootFolder);

        int maxDepth = recursive ? Integer.MAX_VALUE : 1;

        try {
            Files.walkFileTree(targetPath, EnumSet.of(FileVisitOption.FOLLOW_LINKS), maxDepth, new SimpleFileVisitor<Path>() {
                private long filesCount = 0;
                private long bytesCount = 0;
                private long lastNotify = 0;

                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    if (cancelRequested.get()) {
                        result.setCancelled(true);
                        return FileVisitResult.TERMINATE;
                    }

                    if (!dir.equals(targetPath)) {
                        FolderRecord fRecord = new FolderRecord(dir);
                        folderMap.put(dir, fRecord);
                        Path parent = dir.getParent();
                        if (parent != null && folderMap.containsKey(parent)) {
                            folderMap.get(parent).addChild(fRecord);
                        }
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (cancelRequested.get()) {
                        result.setCancelled(true);
                        return FileVisitResult.TERMINATE;
                    }

                    if (attrs.isRegularFile()) {
                        long size = attrs.size();
                        String name = file.getFileName().toString();
                        String ext = FileUtils.getExtension(name);
                        Instant modified = attrs.lastModifiedTime().toInstant();
                        FileCategory cat = FileCategory.fromExtension(ext);

                        FileRecord record = new FileRecord(file, name, ext, size, modified, cat);
                        result.recordFile(record);

                        filesCount++;
                        bytesCount += size;

                        Path parent = file.getParent();
                        while (parent != null && folderMap.containsKey(parent)) {
                            FolderRecord folder = folderMap.get(parent);
                            folder.addSize(size);
                            folder.incrementFileCount();
                            if (parent.equals(targetPath)) break;
                            parent = parent.getParent();
                        }

                        long now = System.currentTimeMillis();
                        if (listener != null && (now - lastNotify > 50 || filesCount % 200 == 0)) {
                            lastNotify = now;
                            listener.onProgress(filesCount, bytesCount, file);
                        }
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) {

                    if (exc != null) {
                        result.addError("Skipped inaccessible file/dir: " + file.getFileName() + " (" + exc.getClass().getSimpleName() + ")");
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (Exception e) {
            result.addError("Scan interrupted: " + e.getMessage());
        }

        long duration = System.currentTimeMillis() - startTime;
        result.setScanDurationMs(duration);
        result.finalizeAnalytics(100, 50, new ArrayList<>(folderMap.values()));

        if (listener != null) {
            listener.onProgress(result.getTotalFiles(), result.getTotalBytes(), targetPath);
        }

        return result;
    }
}

