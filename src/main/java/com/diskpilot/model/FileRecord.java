package com.diskpilot.model;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Objects;

public class FileRecord implements Comparable<FileRecord> {
    private final Path path;
    private final String fileName;
    private final String extension;
    private final long size;
    private final Instant lastModified;
    private final FileCategory category;
    private String sha256Hash;

    public FileRecord(Path path, String fileName, String extension, long size, Instant lastModified, FileCategory category) {
        this.path = Objects.requireNonNull(path, "path cannot be null");
        this.fileName = fileName != null ? fileName : path.getFileName().toString();
        this.extension = extension != null ? extension.toLowerCase() : "";
        this.size = Math.max(0, size);
        this.lastModified = lastModified != null ? lastModified : Instant.now();
        this.category = category != null ? category : FileCategory.fromExtension(this.extension);
    }

    public Path getPath() {
        return path;
    }

    public String getAbsolutePath() {
        return path.toAbsolutePath().toString();
    }

    public String getFileName() {
        return fileName;
    }

    public String getExtension() {
        return extension;
    }

    public long getSize() {
        return size;
    }

    public Instant getLastModified() {
        return lastModified;
    }

    public FileCategory getCategory() {
        return category;
    }

    public String getSha256Hash() {
        return sha256Hash;
    }

    public void setSha256Hash(String sha256Hash) {
        this.sha256Hash = sha256Hash;
    }

    @Override
    public int compareTo(FileRecord other) {

        int cmp = Long.compare(other.size, this.size);
        if (cmp != 0) return cmp;
        return this.fileName.compareToIgnoreCase(other.fileName);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FileRecord that)) return false;
        return Objects.equals(path.toAbsolutePath(), that.path.toAbsolutePath());
    }

    @Override
    public int hashCode() {
        return Objects.hash(path.toAbsolutePath());
    }

    @Override
    public String toString() {
        return fileName + " (" + size + " bytes)";
    }
}

