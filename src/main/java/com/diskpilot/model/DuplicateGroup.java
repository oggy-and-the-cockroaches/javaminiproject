package com.diskpilot.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class DuplicateGroup implements Comparable<DuplicateGroup> {
    private final String sha256;
    private final long fileSize;
    private final List<FileRecord> files = new ArrayList<>();

    public DuplicateGroup(String sha256, long fileSize) {
        this.sha256 = Objects.requireNonNull(sha256, "sha256 cannot be null");
        this.fileSize = Math.max(0, fileSize);
    }

    public String getSha256() {
        return sha256;
    }

    public long getFileSize() {
        return fileSize;
    }

    public List<FileRecord> getFiles() {
        return Collections.unmodifiableList(files);
    }

    public void addFile(FileRecord file) {
        if (file != null && !files.contains(file)) {
            files.add(file);
        }
    }

    public int getCount() {
        return files.size();
    }

    public long getWastedBytes() {
        if (files.size() <= 1) return 0L;
        return fileSize * (files.size() - 1);
    }

    @Override
    public int compareTo(DuplicateGroup other) {

        int cmp = Long.compare(other.getWastedBytes(), this.getWastedBytes());
        if (cmp != 0) return cmp;
        return Long.compare(other.fileSize, this.fileSize);
    }

    @Override
    public String toString() {
        return "SHA256: " + sha256.substring(0, Math.min(8, sha256.length())) +
               "... (" + files.size() + " files, " + getWastedBytes() + " wasted bytes)";
    }
}

