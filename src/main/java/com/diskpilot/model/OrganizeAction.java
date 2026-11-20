package com.diskpilot.model;

import java.nio.file.Path;
import java.util.Objects;

public class OrganizeAction {
    public enum Status {
        PROPOSED,
        MOVED,
        CONFLICT_RESOLVED,
        SKIPPED,
        FAILED,
        UNDONE
    }

    private final Path sourcePath;
    private Path destinationPath;
    private final FileCategory category;
    private final long fileSize;
    private Status status;
    private String message;
    private boolean selected = true;

    public OrganizeAction(Path sourcePath, Path destinationPath, FileCategory category, long fileSize) {
        this.sourcePath = Objects.requireNonNull(sourcePath, "sourcePath cannot be null");
        this.destinationPath = Objects.requireNonNull(destinationPath, "destinationPath cannot be null");
        this.category = category != null ? category : FileCategory.OTHERS;
        this.fileSize = Math.max(0, fileSize);
        this.status = Status.PROPOSED;
        this.message = "Ready to organize";
    }

    public Path getSourcePath() {
        return sourcePath;
    }

    public Path getDestinationPath() {
        return destinationPath;
    }

    public void setDestinationPath(Path destinationPath) {
        this.destinationPath = destinationPath;
    }

    public FileCategory getCategory() {
        return category;
    }

    public long getFileSize() {
        return fileSize;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    @Override
    public String toString() {
        return sourcePath.getFileName() + " -> " + destinationPath + " [" + status + "]";
    }
}

