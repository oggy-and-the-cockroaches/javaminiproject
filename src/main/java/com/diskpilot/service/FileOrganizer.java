package com.diskpilot.service;

import com.diskpilot.model.CustomRule;
import com.diskpilot.model.FileCategory;
import com.diskpilot.model.FileRecord;
import com.diskpilot.model.OrganizeAction;
import com.diskpilot.model.OrganizeBatch;
import com.diskpilot.util.FileUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class FileOrganizer {

    public enum OrganizeMode {
        CATEGORY("By Category (Documents, Images, etc.)"),
        EXTENSION("By File Extension (PDF, PNG, etc.)"),
        DATE_YEAR_MONTH("By Date (YYYY-MM)"),
        DATE_YEAR("By Year (YYYY)"),
        CUSTOM_RULES("By Custom Rules");

        private final String label;
        OrganizeMode(String label) { this.label = label; }
        public String getLabel() { return label; }
    }

    public interface MoveProgressListener {
        void onProgress(int current, int total, OrganizeAction action);
    }

    private static final DateTimeFormatter YYYY_MM = DateTimeFormatter.ofPattern("yyyy-MM").withZone(ZoneId.systemDefault());
    private static final DateTimeFormatter YYYY = DateTimeFormatter.ofPattern("yyyy").withZone(ZoneId.systemDefault());

    private final AtomicBoolean cancelRequested = new AtomicBoolean(false);

    public void cancel() {
        cancelRequested.set(true);
    }

    public boolean isCancelled() {
        return cancelRequested.get();
    }

    public List<OrganizeAction> preview(Path baseFolder, List<FileRecord> files, OrganizeMode mode, List<CustomRule> customRules) {
        List<OrganizeAction> actions = new ArrayList<>();
        if (baseFolder == null || files == null || files.isEmpty()) return actions;

        SafetyValidator.ValidationResult safety = SafetyValidator.validatePathForOrganization(baseFolder);
        if (!safety.isValid()) {
            throw new SecurityException(safety.getMessage());
        }

        for (FileRecord file : files) {
            Path src = file.getPath();

            String targetSubFolder = determineTargetSubFolder(file, mode, customRules);
            if (targetSubFolder == null || targetSubFolder.isBlank()) {
                continue;
            }

            Path destDir = baseFolder.resolve(targetSubFolder);

            if (src.getParent() != null && src.getParent().equals(destDir)) {
                continue;
            }

            Path proposedDest = destDir.resolve(file.getFileName());

            if (Files.exists(proposedDest)) {
                proposedDest = FileUtils.resolveCollision(proposedDest);
            }

            OrganizeAction action = new OrganizeAction(src, proposedDest, file.getCategory(), file.getSize());
            if (!proposedDest.getFileName().toString().equals(file.getFileName())) {
                action.setStatus(OrganizeAction.Status.CONFLICT_RESOLVED);
                action.setMessage("Name collision: will rename to " + proposedDest.getFileName());
            }

            actions.add(action);
        }

        return actions;
    }

    public OrganizeBatch execute(Path baseFolder, List<OrganizeAction> actions, OrganizeMode mode, MoveProgressListener listener) {
        cancelRequested.set(false);
        OrganizeBatch batch = new OrganizeBatch(baseFolder, mode.name());

        if (actions == null || actions.isEmpty()) {
            return batch;
        }

        SafetyValidator.ValidationResult safety = SafetyValidator.validatePathForOrganization(baseFolder);
        if (!safety.isValid()) {
            throw new SecurityException(safety.getMessage());
        }

        int total = actions.size();
        for (int i = 0; i < total; i++) {
            if (cancelRequested.get()) {
                break;
            }

            OrganizeAction action = actions.get(i);
            if (!action.isSelected()) {
                action.setStatus(OrganizeAction.Status.SKIPPED);
                action.setMessage("Skipped by user");
                batch.addAction(action);
                continue;
            }

            Path src = action.getSourcePath();
            Path dst = action.getDestinationPath();

            if (!SafetyValidator.isSafeToModify(src)) {
                action.setStatus(OrganizeAction.Status.FAILED);
                action.setMessage("Blocked: System or protected file.");
                batch.addAction(action);
                continue;
            }

            try {

                Path actualDest = FileUtils.moveFileSafely(src, dst);
                action.setDestinationPath(actualDest);
                if (!actualDest.getFileName().equals(dst.getFileName())) {
                    action.setStatus(OrganizeAction.Status.CONFLICT_RESOLVED);
                    action.setMessage("Moved as " + actualDest.getFileName() + " to prevent overwrite");
                } else {
                    action.setStatus(OrganizeAction.Status.MOVED);
                    action.setMessage("Successfully organized");
                }
            } catch (Exception e) {
                action.setStatus(OrganizeAction.Status.FAILED);
                action.setMessage("Error: " + e.getMessage());
            }

            batch.addAction(action);

            if (listener != null) {
                listener.onProgress(i + 1, total, action);
            }
        }

        return batch;
    }

    public boolean undo(OrganizeBatch batch, MoveProgressListener listener) {
        if (batch == null || batch.isReverted()) return false;
        cancelRequested.set(false);

        List<OrganizeAction> actions = batch.getActions();
        int total = actions.size();
        boolean allSuccess = true;

        for (int i = 0; i < total; i++) {
            if (cancelRequested.get()) break;

            OrganizeAction action = actions.get(i);
            if (action.getStatus() == OrganizeAction.Status.MOVED || action.getStatus() == OrganizeAction.Status.CONFLICT_RESOLVED) {
                Path movedPath = action.getDestinationPath();
                Path originalPath = action.getSourcePath();

                try {
                    if (Files.exists(movedPath)) {
                        Path revertedPath = FileUtils.moveFileSafely(movedPath, originalPath);
                        action.setStatus(OrganizeAction.Status.UNDONE);
                        action.setMessage("Restored to " + revertedPath);
                    } else {
                        action.setStatus(OrganizeAction.Status.FAILED);
                        action.setMessage("Cannot undo: file missing at " + movedPath);
                        allSuccess = false;
                    }
                } catch (Exception e) {
                    action.setStatus(OrganizeAction.Status.FAILED);
                    action.setMessage("Undo failed: " + e.getMessage());
                    allSuccess = false;
                }
            }

            if (listener != null) {
                listener.onProgress(i + 1, total, action);
            }
        }

        batch.setReverted(true);
        return allSuccess;
    }

    private String determineTargetSubFolder(FileRecord file, OrganizeMode mode, List<CustomRule> customRules) {
        switch (mode) {
            case CATEGORY -> {
                return file.getCategory().getFolderName();
            }
            case EXTENSION -> {
                String ext = file.getExtension();
                return ext.isEmpty() ? "No Extension" : ext.toUpperCase();
            }
            case DATE_YEAR_MONTH -> {
                return YYYY_MM.format(file.getLastModified());
            }
            case DATE_YEAR -> {
                return YYYY.format(file.getLastModified());
            }
            case CUSTOM_RULES -> {
                if (customRules != null) {
                    for (CustomRule rule : customRules) {
                        if (rule.isEnabled() && rule.matches(file)) {
                            return rule.getTargetFolderName();
                        }
                    }
                }
                return file.getCategory().getFolderName();
            }
            default -> {
                return file.getCategory().getFolderName();
            }
        }
    }
}

