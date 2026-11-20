package com.diskpilot.service;

import com.diskpilot.model.DuplicateGroup;
import com.diskpilot.model.FileRecord;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class DuplicateDetector {

    public interface DuplicateProgressListener {
        void onProgress(int processed, int total, String currentFileName);
    }

    private static final int BUFFER_SIZE = 64 * 1024;
    private static final int PARTIAL_HASH_SIZE = 4 * 1024;

    private final AtomicBoolean cancelRequested = new AtomicBoolean(false);

    public void cancel() {
        cancelRequested.set(true);
    }

    public boolean isCancelled() {
        return cancelRequested.get();
    }

    public List<DuplicateGroup> findDuplicates(List<FileRecord> files, DuplicateProgressListener listener) {
        cancelRequested.set(false);
        List<DuplicateGroup> duplicateGroups = new ArrayList<>();
        if (files == null || files.size() < 2) return duplicateGroups;

        Map<Long, List<FileRecord>> sizeMap = new HashMap<>();
        for (FileRecord file : files) {
            if (file.getSize() > 0) {
                sizeMap.computeIfAbsent(file.getSize(), k -> new ArrayList<>()).add(file);
            }
        }

        List<FileRecord> candidates = new ArrayList<>();
        for (List<FileRecord> group : sizeMap.values()) {
            if (group.size() > 1) {
                candidates.addAll(group);
            }
        }

        if (candidates.isEmpty()) {
            return duplicateGroups;
        }

        int totalCandidates = candidates.size();
        int processedCount = 0;

        Map<String, DuplicateGroup> hashMap = new HashMap<>();

        for (List<FileRecord> sizeGroup : sizeMap.values()) {
            if (sizeGroup.size() <= 1) continue;
            if (cancelRequested.get()) break;

            Map<String, List<FileRecord>> partialMap = new HashMap<>();
            for (FileRecord file : sizeGroup) {
                if (cancelRequested.get()) break;
                if (file.getSize() > 64 * 1024) {
                    String partial = computePartialHash(file.getPath());
                    if (partial != null) {
                        partialMap.computeIfAbsent(partial, k -> new ArrayList<>()).add(file);
                    } else {
                        partialMap.computeIfAbsent("full", k -> new ArrayList<>()).add(file);
                    }
                } else {
                    partialMap.computeIfAbsent("small", k -> new ArrayList<>()).add(file);
                }
            }

            for (List<FileRecord> subGroup : partialMap.values()) {
                if (subGroup.size() <= 1) {
                    processedCount += subGroup.size();
                    continue;
                }

                for (FileRecord file : subGroup) {
                    if (cancelRequested.get()) break;

                    String fullHash = file.getSha256Hash();
                    if (fullHash == null) {
                        fullHash = computeFullSha256(file.getPath());
                        file.setSha256Hash(fullHash);
                    }

                    if (fullHash != null) {
                        String groupKey = file.getSize() + "_" + fullHash;
                        DuplicateGroup dg = hashMap.computeIfAbsent(groupKey, k -> new DuplicateGroup(file.getSha256Hash(), file.getSize()));
                        dg.addFile(file);
                    }

                    processedCount++;
                    if (listener != null) {
                        listener.onProgress(processedCount, totalCandidates, file.getFileName());
                    }
                }
            }
        }

        for (DuplicateGroup dg : hashMap.values()) {
            if (dg.getCount() > 1) {
                duplicateGroups.add(dg);
            }
        }

        Collections.sort(duplicateGroups);

        return duplicateGroups;
    }

    private String computePartialHash(Path path) {
        try (InputStream in = new BufferedInputStream(new FileInputStream(path.toFile()), PARTIAL_HASH_SIZE)) {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] buf = new byte[PARTIAL_HASH_SIZE];
            int read = in.read(buf);
            if (read > 0) {
                md.update(buf, 0, read);
                return bytesToHex(md.digest());
            }
        } catch (Exception ignored) {}
        return null;
    }

    public String computeFullSha256(Path path) {
        try (InputStream in = new BufferedInputStream(new FileInputStream(path.toFile()), BUFFER_SIZE)) {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                if (cancelRequested.get()) {
                    return null;
                }
                md.update(buffer, 0, bytesRead);
            }
            return bytesToHex(md.digest());
        } catch (Exception e) {
            return null;
        }
    }

    private static String bytesToHex(byte[] hash) {
        StringBuilder hexString = new StringBuilder(2 * hash.length);
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
}

