package com.diskpilot.model;

import java.time.Instant;
import java.util.UUID;

public class HistoryEntry {
    public enum OperationType {
        SCAN("Storage Scan"),
        ORGANIZE("File Organization"),
        DUPLICATE_DETECTION("Duplicate Detection"),
        UNDO("Operation Undo"),
        WARNING("Warning"),
        ERROR("Error");

        private final String label;
        OperationType(String label) { this.label = label; }
        public String getLabel() { return label; }
    }

    public enum Status {
        SUCCESS("Success"),
        WARNING("Warning"),
        FAILED("Failed"),
        UNDONE("Undone");

        private final String label;
        Status(String label) { this.label = label; }
        public String getLabel() { return label; }
    }

    private final String id;
    private final Instant timestamp;
    private final OperationType type;
    private final String title;
    private final String details;
    private Status status;
    private final String targetPath;
    private final int affectedCount;
    private final long affectedBytes;
    private final String batchId;

    public HistoryEntry(OperationType type, String title, String details, Status status,
                        String targetPath, int affectedCount, long affectedBytes, String batchId) {
        this(UUID.randomUUID().toString(), Instant.now(), type, title, details, status, targetPath, affectedCount, affectedBytes, batchId);
    }

    public HistoryEntry(String id, Instant timestamp, OperationType type, String title, String details, Status status,
                        String targetPath, int affectedCount, long affectedBytes, String batchId) {
        this.id = id != null ? id : UUID.randomUUID().toString();
        this.timestamp = timestamp != null ? timestamp : Instant.now();
        this.type = type != null ? type : OperationType.SCAN;
        this.title = title != null ? title : "";
        this.details = details != null ? details : "";
        this.status = status != null ? status : Status.SUCCESS;
        this.targetPath = targetPath != null ? targetPath : "";
        this.affectedCount = Math.max(0, affectedCount);
        this.affectedBytes = Math.max(0, affectedBytes);
        this.batchId = batchId;
    }

    public String getId() {
        return id;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public OperationType getType() {
        return type;
    }

    public String getTitle() {
        return title;
    }

    public String getDetails() {
        return details;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public String getTargetPath() {
        return targetPath;
    }

    public int getAffectedCount() {
        return affectedCount;
    }

    public long getAffectedBytes() {
        return affectedBytes;
    }

    public String getBatchId() {
        return batchId;
    }
}

