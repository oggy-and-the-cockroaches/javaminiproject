package com.diskpilot.service;

import com.diskpilot.model.HistoryEntry;
import com.diskpilot.model.OrganizeBatch;
import com.diskpilot.util.JsonUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class HistoryManager {

    private static HistoryManager instance;

    private final Path storageDir;
    private final Path historyFile;
    private final Path batchesFile;

    private final List<HistoryEntry> entries = new CopyOnWriteArrayList<>();
    private final List<OrganizeBatch> batches = new CopyOnWriteArrayList<>();

    public static synchronized HistoryManager getInstance() {
        if (instance == null) {
            instance = new HistoryManager();
        }
        return instance;
    }

    public HistoryManager() {
        String appData = System.getenv("APPDATA");
        if (appData != null && !appData.isBlank()) {
            storageDir = Paths.get(appData, "DiskPilot");
        } else {
            storageDir = Paths.get(System.getProperty("user.home"), ".diskpilot");
        }

        historyFile = storageDir.resolve("history.json");
        batchesFile = storageDir.resolve("batches.json");

        loadFromDisk();
    }

    public synchronized void recordScan(Path folder, long filesCount, long totalBytes, long durationMs, boolean cancelled) {
        String title = cancelled ? "Storage Scan Cancelled" : "Storage Scan Completed";
        String details = String.format("Scanned %d files (%s) in %s", filesCount, com.diskpilot.util.FormatUtils.formatBytes(totalBytes), com.diskpilot.util.FormatUtils.formatDuration(durationMs));
        HistoryEntry.Status status = cancelled ? HistoryEntry.Status.WARNING : HistoryEntry.Status.SUCCESS;
        addEntry(new HistoryEntry(HistoryEntry.OperationType.SCAN, title, details, status, folder.toString(), (int) Math.min(filesCount, Integer.MAX_VALUE), totalBytes, null));
    }

    public synchronized void recordOrganize(OrganizeBatch batch) {
        if (batch == null) return;
        batches.add(batch);
        int success = batch.getSuccessCount();
        int failed = batch.getFailedCount();
        long bytes = batch.getTotalMovedBytes();

        String title = "File Organization (" + batch.getMode() + ")";
        String details = String.format("Organized %d files (%s). %d skipped/failed.", success, com.diskpilot.util.FormatUtils.formatBytes(bytes), failed);
        HistoryEntry.Status status = failed == 0 ? HistoryEntry.Status.SUCCESS : HistoryEntry.Status.WARNING;

        String pathStr = batch.getSourceDirectory() != null ? batch.getSourceDirectory().toString() : "";
        addEntry(new HistoryEntry(HistoryEntry.OperationType.ORGANIZE, title, details, status, pathStr, success, bytes, batch.getBatchId()));
        saveToDisk();
    }

    public synchronized void recordDuplicateScan(Path folder, int duplicateGroupsCount, int duplicateFilesCount, long wastedBytes) {
        String title = "Duplicate Analysis Completed";
        String details = String.format("Found %d duplicate groups (%d files). Reclaimable storage: %s",
                duplicateGroupsCount, duplicateFilesCount, com.diskpilot.util.FormatUtils.formatBytes(wastedBytes));
        HistoryEntry.Status status = duplicateGroupsCount > 0 ? HistoryEntry.Status.WARNING : HistoryEntry.Status.SUCCESS;
        addEntry(new HistoryEntry(HistoryEntry.OperationType.DUPLICATE_DETECTION, title, details, status, folder.toString(), duplicateFilesCount, wastedBytes, null));
    }

    public synchronized void recordUndo(OrganizeBatch batch, boolean success) {
        if (batch == null) return;
        String title = "Undo File Organization";
        String details = success ? "Successfully reverted batch " + batch.getBatchId() : "Partially reverted batch " + batch.getBatchId();
        HistoryEntry.Status status = success ? HistoryEntry.Status.UNDONE : HistoryEntry.Status.WARNING;
        String pathStr = batch.getSourceDirectory() != null ? batch.getSourceDirectory().toString() : "";
        addEntry(new HistoryEntry(HistoryEntry.OperationType.UNDO, title, details, status, pathStr, batch.getSuccessCount(), batch.getTotalMovedBytes(), batch.getBatchId()));
        saveToDisk();
    }

    public synchronized void recordError(String title, String details, String targetPath) {
        addEntry(new HistoryEntry(HistoryEntry.OperationType.ERROR, title, details, HistoryEntry.Status.FAILED, targetPath, 0, 0L, null));
    }

    public synchronized void recordWarning(String title, String details, String targetPath) {
        addEntry(new HistoryEntry(HistoryEntry.OperationType.WARNING, title, details, HistoryEntry.Status.WARNING, targetPath, 0, 0L, null));
    }

    public synchronized void addEntry(HistoryEntry entry) {
        if (entry != null) {
            entries.add(0, entry);
            saveToDisk();
        }
    }

    public List<HistoryEntry> getEntries() {
        return Collections.unmodifiableList(entries);
    }

    public List<OrganizeBatch> getBatches() {
        return Collections.unmodifiableList(batches);
    }

    public OrganizeBatch getBatchById(String batchId) {
        if (batchId == null) return null;
        for (OrganizeBatch b : batches) {
            if (batchId.equals(b.getBatchId())) {
                return b;
            }
        }
        return null;
    }

    public synchronized void clearHistory() {
        entries.clear();
        batches.clear();
        saveToDisk();
    }

    public synchronized void saveToDisk() {
        try {
            if (!Files.exists(storageDir)) {
                Files.createDirectories(storageDir);
            }
            Files.writeString(historyFile, JsonUtils.historyToJson(new ArrayList<>(entries)));
            Files.writeString(batchesFile, JsonUtils.batchesToJson(new ArrayList<>(batches)));
        } catch (IOException ignored) {}
    }

    private synchronized void loadFromDisk() {
        try {
            if (Files.exists(historyFile)) {
                String json = Files.readString(historyFile);
                entries.clear();
                entries.addAll(JsonUtils.parseHistoryJson(json));
            }
            if (Files.exists(batchesFile)) {
                String json = Files.readString(batchesFile);
                batches.clear();
                batches.addAll(JsonUtils.parseBatchesJson(json));
            }
        } catch (Exception ignored) {}
    }
}

