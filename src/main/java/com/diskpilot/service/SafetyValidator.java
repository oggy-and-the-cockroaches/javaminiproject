package com.diskpilot.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public final class SafetyValidator {

    private static final Set<String> SYSTEM_FOLDER_NAMES = new HashSet<>(Arrays.asList(
            "windows",
            "system32",
            "syswow64",
            "program files",
            "program files (x86)",
            "programdata",
            "$recycle.bin",
            "system volume information",
            "recovery",
            "boot",
            "bootmgr",
            "perflogs",
            "msocache"
    ));

    private static final Set<String> PROTECTED_FILENAMES = new HashSet<>(Arrays.asList(
            "pagefile.sys",
            "hiberfil.sys",
            "swapfile.sys",
            "dumpstack.log",
            "bootmgr",
            "ntldr",
            "desktop.ini",
            "thumbs.db"
    ));

    private SafetyValidator() {}

    public static ValidationResult validatePathForOrganization(Path path) {
        if (path == null) {
            return new ValidationResult(false, "Path cannot be null.");
        }

        try {
            Path realPath = path.toAbsolutePath().normalize();

            if (realPath.getNameCount() == 0 || realPath.equals(realPath.getRoot())) {
                return new ValidationResult(false, "Cannot organize the drive root (" + realPath + "). Please select a specific folder.");
            }

            String pathStr = realPath.toString().toLowerCase();

            String winDir = System.getenv("WINDIR");
            if (winDir != null && pathStr.startsWith(winDir.toLowerCase())) {
                return new ValidationResult(false, "Access Denied: Windows Operating System directory is strictly protected (" + winDir + ").");
            }

            String progFiles = System.getenv("ProgramFiles");
            if (progFiles != null && pathStr.startsWith(progFiles.toLowerCase())) {
                return new ValidationResult(false, "Access Denied: Program Files directory is strictly protected (" + progFiles + ").");
            }

            String progFilesX86 = System.getenv("ProgramFiles(x86)");
            if (progFilesX86 != null && pathStr.startsWith(progFilesX86.toLowerCase())) {
                return new ValidationResult(false, "Access Denied: Program Files (x86) directory is strictly protected (" + progFilesX86 + ").");
            }

            String progData = System.getenv("ProgramData");
            if (progData != null && pathStr.startsWith(progData.toLowerCase())) {
                return new ValidationResult(false, "Access Denied: ProgramData directory is strictly protected (" + progData + ").");
            }

            for (Path part : realPath) {
                String segment = part.toString().toLowerCase();
                if (SYSTEM_FOLDER_NAMES.contains(segment)) {
                    return new ValidationResult(false, "Access Denied: System folder '" + part + "' cannot be modified.");
                }
            }

            if (Files.exists(realPath)) {
                if (!Files.isDirectory(realPath)) {
                    return new ValidationResult(false, "Selected path is a file, not a directory: " + realPath);
                }
                if (!Files.isReadable(realPath)) {
                    return new ValidationResult(false, "Permission Denied: Selected folder is not readable.");
                }
            }

            return new ValidationResult(true, "Path validated successfully.");
        } catch (Exception e) {
            return new ValidationResult(false, "Error validating path: " + e.getMessage());
        }
    }

    public static boolean isSafeToModify(Path filePath) {
        if (filePath == null || !Files.exists(filePath)) return false;

        Path fileName = filePath.getFileName();
        if (fileName != null) {
            String nameLower = fileName.toString().toLowerCase();
            if (PROTECTED_FILENAMES.contains(nameLower)) {
                return false;
            }
        }

        Path parent = filePath.getParent();
        if (parent != null) {
            ValidationResult parentCheck = validatePathForOrganization(parent);
            return parentCheck.isValid();
        }

        return true;
    }

    public static boolean isCircularDestination(Path sourceDir, Path destinationDir) {
        if (sourceDir == null || destinationDir == null) return false;
        try {
            Path normSource = sourceDir.toAbsolutePath().normalize();
            Path normDest = destinationDir.toAbsolutePath().normalize();
            return normDest.startsWith(normSource) && !normDest.equals(normSource);
        } catch (Exception e) {
            return false;
        }
    }

    public static class ValidationResult {
        private final boolean valid;
        private final String message;

        public ValidationResult(boolean valid, String message) {
            this.valid = valid;
            this.message = message;
        }

        public boolean isValid() {
            return valid;
        }

        public String getMessage() {
            return message;
        }
    }
}

