package com.diskpilot.model;

import java.awt.Color;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public enum FileCategory {
    DOCUMENTS(
        "Documents",
        "Documents",
        new Color(0x38, 0xBD, 0xF8),
        "pdf", "doc", "docx", "txt", "rtf", "odt", "xls", "xlsx", "ppt", "pptx", "csv", "md", "epub"
    ),
    IMAGES(
        "Images",
        "Images",
        new Color(0x34, 0xD3, 0x99),
        "jpg", "jpeg", "png", "gif", "bmp", "webp", "svg", "tiff", "tif", "ico", "raw", "psd"
    ),
    VIDEOS(
        "Videos",
        "Videos",
        new Color(0xF4, 0x3F, 0x5E),
        "mp4", "mkv", "mov", "avi", "wmv", "flv", "webm", "m4v", "mpg", "mpeg", "3gp"
    ),
    AUDIO(
        "Audio",
        "Audio",
        new Color(0xA8, 0x55, 0xF7),
        "mp3", "wav", "flac", "aac", "ogg", "m4a", "wma", "opus", "aiff"
    ),
    ARCHIVES(
        "Archives",
        "Archives",
        new Color(0xFB, 0xBF, 0x24),
        "zip", "rar", "7z", "tar", "gz", "bz2", "xz", "iso", "tgz"
    ),
    EXECUTABLES(
        "Executables",
        "Executables",
        new Color(0xFB, 0x71, 0x85),
        "exe", "msi", "bat", "cmd", "ps1", "sh", "jar", "bin"
    ),
    CODE(
        "Code & Dev",
        "Development",
        new Color(0x2D, 0xD4, 0xBF),
        "java", "py", "js", "ts", "html", "css", "c", "cpp", "h", "hpp", "cs", "go", "rs", "json", "xml", "sql", "yaml", "yml", "php", "kt"
    ),
    OTHERS(
        "Others",
        "Others",
        new Color(0x94, 0xA3, 0xB8),
        new String[0]
    );

    private final String displayName;
    private final String folderName;
    private final Color color;
    private final Set<String> extensions;

    FileCategory(String displayName, String folderName, Color color, String... extensions) {
        this.displayName = displayName;
        this.folderName = folderName;
        this.color = color;
        Set<String> set = new HashSet<>();
        for (String ext : extensions) {
            set.add(ext.toLowerCase());
        }
        this.extensions = Collections.unmodifiableSet(set);
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getFolderName() {
        return folderName;
    }

    public Color getColor() {
        return color;
    }

    public Set<String> getExtensions() {
        return extensions;
    }

    public static FileCategory fromExtension(String ext) {
        if (ext == null || ext.isBlank()) {
            return OTHERS;
        }
        String clean = ext.trim().toLowerCase();
        if (clean.startsWith(".")) {
            clean = clean.substring(1);
        }
        for (FileCategory cat : values()) {
            if (cat.extensions.contains(clean)) {
                return cat;
            }
        }
        return OTHERS;
    }
}

