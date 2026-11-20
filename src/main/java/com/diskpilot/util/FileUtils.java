package com.diskpilot.util;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.Objects;

public final class FileUtils {

    private FileUtils() {}

    public static String getExtension(Path path) {
        if (path == null) return "";
        Path fileName = path.getFileName();
        if (fileName == null) return "";
        return getExtension(fileName.toString());
    }

    public static String getExtension(String fileName) {
        if (fileName == null) return "";
        int dotIdx = fileName.lastIndexOf('.');
        if (dotIdx > 0 && dotIdx < fileName.length() - 1) {
            return fileName.substring(dotIdx + 1).toLowerCase();
        }
        return "";
    }

    public static String getBaseName(String fileName) {
        if (fileName == null) return "";
        int dotIdx = fileName.lastIndexOf('.');
        if (dotIdx > 0) {
            return fileName.substring(0, dotIdx);
        }
        return fileName;
    }

    public static Path resolveCollision(Path destination) {
        if (!Files.exists(destination)) {
            return destination;
        }

        Path parent = destination.getParent();
        String fullName = destination.getFileName().toString();
        String baseName = getBaseName(fullName);
        String ext = getExtension(fullName);
        String extSuffix = ext.isEmpty() ? "" : "." + ext;

        int counter = 1;
        Path candidate;
        do {
            String newName = String.format("%s (%d)%s", baseName, counter++, extSuffix);
            candidate = parent != null ? parent.resolve(newName) : Paths.get(newName);
        } while (Files.exists(candidate));

        return candidate;
    }

    public static boolean openInFileExplorer(Path path) {
        if (path == null) return false;
        try {
            File file = path.toFile();
            if (!file.exists()) {
                Path parent = path.getParent();
                if (parent != null && Files.exists(parent)) {
                    file = parent.toFile();
                } else {
                    return false;
                }
            }

            String os = System.getProperty("os.name", "").toLowerCase();
            if (os.contains("win")) {
                new ProcessBuilder("explorer.exe", "/select,\"" + file.getAbsolutePath() + "\"").start();
                return true;
            } else if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                Desktop.getDesktop().open(file.isDirectory() ? file : file.getParentFile());
                return true;
            }
        } catch (Exception e) {

            try {
                if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                    Desktop.getDesktop().open(path.toFile());
                    return true;
                }
            } catch (Exception ignored) {}
        }
        return false;
    }

    public static Path moveFileSafely(Path source, Path destination) throws IOException {
        Objects.requireNonNull(source, "source cannot be null");
        Objects.requireNonNull(destination, "destination cannot be null");

        if (!Files.exists(source)) {
            throw new NoSuchFileException("Source file does not exist: " + source);
        }

        Path parent = destination.getParent();
        if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent);
        }

        Path finalDest = resolveCollision(destination);

        try {
            return Files.move(source, finalDest, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {

            return Files.move(source, finalDest);
        }
    }
}

