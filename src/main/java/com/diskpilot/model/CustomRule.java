package com.diskpilot.model;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class CustomRule {
    private String name;
    private String targetFolderName;
    private Set<String> extensions = new HashSet<>();
    private String nameContains;
    private long minSizeBytes;
    private boolean enabled;

    public CustomRule(String name, String targetFolderName, String extensionsCsv, String nameContains, long minSizeBytes, boolean enabled) {
        this.name = name != null ? name : "New Rule";
        this.targetFolderName = targetFolderName != null && !targetFolderName.isBlank() ? targetFolderName.trim() : "Custom";
        this.nameContains = nameContains != null ? nameContains.trim().toLowerCase() : "";
        this.minSizeBytes = Math.max(0, minSizeBytes);
        this.enabled = enabled;
        setExtensionsFromCsv(extensionsCsv);
    }

    public void setExtensionsFromCsv(String extensionsCsv) {
        extensions.clear();
        if (extensionsCsv != null && !extensionsCsv.isBlank()) {
            for (String part : extensionsCsv.split("[,;\\s]+")) {
                String clean = part.trim().toLowerCase();
                if (clean.startsWith(".")) clean = clean.substring(1);
                if (!clean.isEmpty()) {
                    extensions.add(clean);
                }
            }
        }
    }

    public boolean matches(FileRecord record) {
        if (!enabled || record == null) return false;

        if (!extensions.isEmpty()) {
            String ext = record.getExtension();
            if (!extensions.contains(ext)) {
                return false;
            }
        }

        if (!nameContains.isEmpty()) {
            String lowerName = record.getFileName().toLowerCase();
            if (!lowerName.contains(nameContains)) {
                return false;
            }
        }

        if (minSizeBytes > 0) {
            if (record.getSize() < minSizeBytes) {
                return false;
            }
        }

        return true;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTargetFolderName() {
        return targetFolderName;
    }

    public void setTargetFolderName(String targetFolderName) {
        this.targetFolderName = targetFolderName;
    }

    public Set<String> getExtensions() {
        return extensions;
    }

    public String getExtensionsCsv() {
        return String.join(", ", extensions);
    }

    public String getNameContains() {
        return nameContains;
    }

    public void setNameContains(String nameContains) {
        this.nameContains = nameContains;
    }

    public long getMinSizeBytes() {
        return minSizeBytes;
    }

    public void setMinSizeBytes(long minSizeBytes) {
        this.minSizeBytes = minSizeBytes;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}

