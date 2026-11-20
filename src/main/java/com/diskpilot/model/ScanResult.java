package com.diskpilot.model;

import java.nio.file.Path;
import java.util.*;

public class ScanResult {
    private final Path rootPath;
    private long totalFiles;
    private long totalBytes;
    private long scanDurationMs;
    private boolean cancelled;

    private final Map<FileCategory, Long> categoryBytes = new EnumMap<>(FileCategory.class);
    private final Map<FileCategory, Integer> categoryCounts = new EnumMap<>(FileCategory.class);
    private final Map<String, Long> extensionBytes = new HashMap<>();
    private final List<FileRecord> largestFiles = new ArrayList<>();
    private final List<FolderRecord> topFolders = new ArrayList<>();
    private final List<FileRecord> allFiles = new ArrayList<>();
    private final List<String> errors = new ArrayList<>();

    public ScanResult(Path rootPath) {
        this.rootPath = rootPath;
        for (FileCategory cat : FileCategory.values()) {
            categoryBytes.put(cat, 0L);
            categoryCounts.put(cat, 0);
        }
    }

    public Path getRootPath() {
        return rootPath;
    }

    public long getTotalFiles() {
        return totalFiles;
    }

    public void setTotalFiles(long totalFiles) {
        this.totalFiles = totalFiles;
    }

    public long getTotalBytes() {
        return totalBytes;
    }

    public void setTotalBytes(long totalBytes) {
        this.totalBytes = totalBytes;
    }

    public long getScanDurationMs() {
        return scanDurationMs;
    }

    public void setScanDurationMs(long scanDurationMs) {
        this.scanDurationMs = scanDurationMs;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    public Map<FileCategory, Long> getCategoryBytes() {
        return Collections.unmodifiableMap(categoryBytes);
    }

    public Map<FileCategory, Integer> getCategoryCounts() {
        return Collections.unmodifiableMap(categoryCounts);
    }

    public Map<String, Long> getExtensionBytes() {
        return Collections.unmodifiableMap(extensionBytes);
    }

    public List<FileRecord> getLargestFiles() {
        return Collections.unmodifiableList(largestFiles);
    }

    public List<FolderRecord> getTopFolders() {
        return Collections.unmodifiableList(topFolders);
    }

    public List<FileRecord> getAllFiles() {
        return Collections.unmodifiableList(allFiles);
    }

    public List<String> getErrors() {
        return Collections.unmodifiableList(errors);
    }

    public void recordFile(FileRecord file) {
        if (file == null) return;
        allFiles.add(file);
        totalFiles++;
        totalBytes += file.getSize();

        FileCategory cat = file.getCategory();
        categoryBytes.put(cat, categoryBytes.getOrDefault(cat, 0L) + file.getSize());
        categoryCounts.put(cat, categoryCounts.getOrDefault(cat, 0) + 1);

        String ext = file.getExtension();
        if (!ext.isEmpty()) {
            extensionBytes.put(ext, extensionBytes.getOrDefault(ext, 0L) + file.getSize());
        }
    }

    public void finalizeAnalytics(int topFilesLimit, int topFoldersLimit, List<FolderRecord> discoveredFolders) {

        allFiles.sort(Comparator.comparingLong(FileRecord::getSize).reversed());
        largestFiles.clear();
        for (int i = 0; i < Math.min(topFilesLimit, allFiles.size()); i++) {
            largestFiles.add(allFiles.get(i));
        }

        if (discoveredFolders != null) {
            List<FolderRecord> sortedFolders = new ArrayList<>(discoveredFolders);
            sortedFolders.sort(Comparator.comparingLong(FolderRecord::getTotalSize).reversed());
            topFolders.clear();
            for (int i = 0; i < Math.min(topFoldersLimit, sortedFolders.size()); i++) {
                topFolders.add(sortedFolders.get(i));
            }
        }
    }

    public void addError(String error) {
        if (error != null) {
            errors.add(error);
        }
    }
}

