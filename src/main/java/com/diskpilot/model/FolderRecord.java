package com.diskpilot.model;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class FolderRecord implements Comparable<FolderRecord> {
    private final Path path;
    private final String folderName;
    private long totalSize;
    private int fileCount;
    private final List<FolderRecord> children = new ArrayList<>();

    public FolderRecord(Path path) {
        this.path = Objects.requireNonNull(path, "path cannot be null");
        Path fileName = path.getFileName();
        this.folderName = fileName != null ? fileName.toString() : path.toString();
        this.totalSize = 0;
        this.fileCount = 0;
    }

    public Path getPath() {
        return path;
    }

    public String getAbsolutePath() {
        return path.toAbsolutePath().toString();
    }

    public String getFolderName() {
        return folderName;
    }

    public long getTotalSize() {
        return totalSize;
    }

    public void addSize(long bytes) {
        this.totalSize += Math.max(0, bytes);
    }

    public int getFileCount() {
        return fileCount;
    }

    public void incrementFileCount() {
        this.fileCount++;
    }

    public void addChild(FolderRecord child) {
        if (child != null) {
            this.children.add(child);
        }
    }

    public List<FolderRecord> getChildren() {
        return Collections.unmodifiableList(children);
    }

    @Override
    public int compareTo(FolderRecord other) {
        return Long.compare(other.totalSize, this.totalSize);
    }

    @Override
    public String toString() {
        return folderName + " (" + totalSize + " bytes, " + fileCount + " files)";
    }
}

