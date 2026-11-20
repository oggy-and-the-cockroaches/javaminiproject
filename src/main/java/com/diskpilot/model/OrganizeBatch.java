package com.diskpilot.model;

import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class OrganizeBatch {
    private final String batchId;
    private final Instant timestamp;
    private final Path sourceDirectory;
    private final String mode;
    private final List<OrganizeAction> actions = new ArrayList<>();
    private boolean reverted;

    public OrganizeBatch(Path sourceDirectory, String mode) {
        this(UUID.randomUUID().toString(), Instant.now(), sourceDirectory, mode, false);
    }

    public OrganizeBatch(String batchId, Instant timestamp, Path sourceDirectory, String mode, boolean reverted) {
        this.batchId = batchId != null ? batchId : UUID.randomUUID().toString();
        this.timestamp = timestamp != null ? timestamp : Instant.now();
        this.sourceDirectory = sourceDirectory;
        this.mode = mode != null ? mode : "Category";
        this.reverted = reverted;
    }

    public String getBatchId() {
        return batchId;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public Path getSourceDirectory() {
        return sourceDirectory;
    }

    public String getMode() {
        return mode;
    }

    public boolean isReverted() {
        return reverted;
    }

    public void setReverted(boolean reverted) {
        this.reverted = reverted;
    }

    public void addAction(OrganizeAction action) {
        if (action != null) {
            actions.add(action);
        }
    }

    public List<OrganizeAction> getActions() {
        return Collections.unmodifiableList(actions);
    }

    public int getSuccessCount() {
        int c = 0;
        for (OrganizeAction a : actions) {
            if (a.getStatus() == OrganizeAction.Status.MOVED || a.getStatus() == OrganizeAction.Status.CONFLICT_RESOLVED) {
                c++;
            }
        }
        return c;
    }

    public int getFailedCount() {
        int c = 0;
        for (OrganizeAction a : actions) {
            if (a.getStatus() == OrganizeAction.Status.FAILED) {
                c++;
            }
        }
        return c;
    }

    public long getTotalMovedBytes() {
        long bytes = 0;
        for (OrganizeAction a : actions) {
            if (a.getStatus() == OrganizeAction.Status.MOVED || a.getStatus() == OrganizeAction.Status.CONFLICT_RESOLVED) {
                bytes += a.getFileSize();
            }
        }
        return bytes;
    }
}

