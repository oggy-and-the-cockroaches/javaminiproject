package com.diskpilot;

import com.diskpilot.model.*;
import com.diskpilot.service.*;
import com.diskpilot.util.FileUtils;
import com.diskpilot.util.FormatUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class DiskPilotTestSuite {

    private static int testsRun = 0;
    private static int testsPassed = 0;

    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println("    RUNNING DISKPILOT TEST & VERIFICATION SUITE   ");
        System.out.println("=================================================\n");

        testFileCategorization();
        testSafetyValidator();
        testCollisionResolution();
        testRecursiveScanning();
        testSortifyOrganizationAndUndo();
        testDuplicateDetectionSha256();
        testHistoryPersistence();

        System.out.println("\n=================================================");
        System.out.printf("  TEST RESULTS: %d / %d PASSED (%.1f%%)\n",
                testsPassed, testsRun, ((double) testsPassed / testsRun) * 100.0);
        System.out.println("=================================================");

        if (testsPassed != testsRun) {
            System.err.println("SOME TESTS FAILED!");
            System.exit(1);
        } else {
            System.out.println("ALL INTEGRATION TESTS PASSED CLEANLY!");
        }
    }

    private static void assertTrue(String testName, boolean condition, String failureMsg) {
        testsRun++;
        if (condition) {
            testsPassed++;
            System.out.println("  [PASS] " + testName);
        } else {
            System.err.println("  [FAIL] " + testName + " -> " + failureMsg);
        }
    }

    private static void testFileCategorization() {
        System.out.println("[1] Testing File Categorization Taxonomy...");
        assertTrue("Categorize PDF as DOCUMENTS", FileCategory.fromExtension("pdf") == FileCategory.DOCUMENTS, "Expected DOCUMENTS");
        assertTrue("Categorize DOCX as DOCUMENTS", FileCategory.fromExtension(".docx") == FileCategory.DOCUMENTS, "Expected DOCUMENTS");
        assertTrue("Categorize PNG as IMAGES", FileCategory.fromExtension("png") == FileCategory.IMAGES, "Expected IMAGES");
        assertTrue("Categorize MP4 as VIDEOS", FileCategory.fromExtension("mp4") == FileCategory.VIDEOS, "Expected VIDEOS");
        assertTrue("Categorize MP3 as AUDIO", FileCategory.fromExtension("mp3") == FileCategory.AUDIO, "Expected AUDIO");
        assertTrue("Categorize ZIP as ARCHIVES", FileCategory.fromExtension("zip") == FileCategory.ARCHIVES, "Expected ARCHIVES");
        assertTrue("Categorize EXE as EXECUTABLES", FileCategory.fromExtension("exe") == FileCategory.EXECUTABLES, "Expected EXECUTABLES");
        assertTrue("Categorize JAVA as CODE", FileCategory.fromExtension("java") == FileCategory.CODE, "Expected CODE");
        assertTrue("Categorize XYZ as OTHERS", FileCategory.fromExtension("xyz") == FileCategory.OTHERS, "Expected OTHERS");
    }

    private static void testSafetyValidator() {
        System.out.println("\n[2] Testing Operating System Safety Validator...");

        SafetyValidator.ValidationResult winCheck = SafetyValidator.validatePathForOrganization(Path.of("C:\\Windows"));
        assertTrue("Block C:\\Windows", !winCheck.isValid(), "C:\\Windows must be rejected");

        SafetyValidator.ValidationResult progCheck = SafetyValidator.validatePathForOrganization(Path.of("C:\\Program Files"));
        assertTrue("Block C:\\Program Files", !progCheck.isValid(), "Program Files must be rejected");

        SafetyValidator.ValidationResult sys32Check = SafetyValidator.validatePathForOrganization(Path.of("C:\\Users\\AJAY\\System32"));
        assertTrue("Block folder containing System32", !sys32Check.isValid(), "System32 segment must be rejected");

        SafetyValidator.ValidationResult rootCheck = SafetyValidator.validatePathForOrganization(Path.of("C:\\"));
        assertTrue("Block drive root C:\\", !rootCheck.isValid(), "Drive root must be rejected");

        SafetyValidator.ValidationResult safeCheck = SafetyValidator.validatePathForOrganization(Path.of("C:\\Users\\AJAY\\Desktop\\Diskpilot"));
        assertTrue("Allow safe project folder", safeCheck.isValid(), "Valid user path should pass");
    }

    private static void testCollisionResolution() {
        System.out.println("\n[3] Testing Non-destructive File Collision Resolution...");
        try {
            Path tempDir = Files.createTempDirectory("diskpilot_collision_test");
            Path original = tempDir.resolve("sample.txt");
            Files.writeString(original, "Original content");

            Path collision1 = FileUtils.resolveCollision(original);
            assertTrue("Detects collision and suggests sample (1).txt",
                    collision1.getFileName().toString().equals("sample (1).txt"),
                    "Expected 'sample (1).txt', got: " + collision1.getFileName());

            Files.writeString(collision1, "Second content");
            Path collision2 = FileUtils.resolveCollision(original);
            assertTrue("Detects second collision and suggests sample (2).txt",
                    collision2.getFileName().toString().equals("sample (2).txt"),
                    "Expected 'sample (2).txt', got: " + collision2.getFileName());

            Files.deleteIfExists(collision1);
            Files.deleteIfExists(original);
            Files.deleteIfExists(tempDir);
        } catch (IOException e) {
            assertTrue("Collision resolution test exception", false, e.getMessage());
        }
    }

    private static void testRecursiveScanning() {
        System.out.println("\n[4] Testing Java NIO Recursive File Traversal...");
        try {
            Path tempDir = Files.createTempDirectory("diskpilot_scan_test");
            Path subDir = tempDir.resolve("nested_folder");
            Files.createDirectories(subDir);

            Files.writeString(tempDir.resolve("root_doc.pdf"), "PDF data here");
            Files.writeString(subDir.resolve("nested_img.png"), "PNG data here");

            FileScanner scanner = new FileScanner();
            ScanResult result = scanner.scan(tempDir, true, null);

            assertTrue("Scan finds 2 files", result.getTotalFiles() == 2, "Found: " + result.getTotalFiles());
            assertTrue("Category counts contains DOCUMENTS", result.getCategoryCounts().get(FileCategory.DOCUMENTS) == 1, "Documents count mismatch");
            assertTrue("Category counts contains IMAGES", result.getCategoryCounts().get(FileCategory.IMAGES) == 1, "Images count mismatch");

            Files.deleteIfExists(tempDir.resolve("root_doc.pdf"));
            Files.deleteIfExists(subDir.resolve("nested_img.png"));
            Files.deleteIfExists(subDir);
            Files.deleteIfExists(tempDir);
        } catch (IOException e) {
            assertTrue("Scan test exception", false, e.getMessage());
        }
    }

    private static void testSortifyOrganizationAndUndo() {
        System.out.println("\n[5] Testing Sortify Preview, Batch Organization & Safe Undo...");
        try {
            Path testRoot = Files.createTempDirectory("diskpilot_sortify_test");

            Path docFile = testRoot.resolve("quarterly_report.pdf");
            Path imgFile = testRoot.resolve("vacation_photo.jpg");
            Path musicFile = testRoot.resolve("song_track.mp3");

            Files.writeString(docFile, "Quarterly Report Text");
            Files.writeString(imgFile, "Photo Binary Emulation");
            Files.writeString(musicFile, "Audio Stream Emulation");

            FileScanner scanner = new FileScanner();
            ScanResult initialScan = scanner.scan(testRoot, false, null);

            FileOrganizer organizer = new FileOrganizer();
            List<OrganizeAction> preview = organizer.preview(testRoot, initialScan.getAllFiles(), FileOrganizer.OrganizeMode.CATEGORY, null);

            assertTrue("Preview proposed 3 moves", preview.size() == 3, "Expected 3 proposals, got " + preview.size());

            OrganizeBatch batch = organizer.execute(testRoot, preview, FileOrganizer.OrganizeMode.CATEGORY, null);

            assertTrue("Batch succeeded with 3 moves", batch.getSuccessCount() == 3, "Expected 3 successes, got " + batch.getSuccessCount());
            assertTrue("Documents folder created", Files.exists(testRoot.resolve("Documents").resolve("quarterly_report.pdf")), "PDF missing in Documents");
            assertTrue("Images folder created", Files.exists(testRoot.resolve("Images").resolve("vacation_photo.jpg")), "JPG missing in Images");
            assertTrue("Audio folder created", Files.exists(testRoot.resolve("Audio").resolve("song_track.mp3")), "MP3 missing in Audio");
            assertTrue("Original root files cleared", !Files.exists(docFile) && !Files.exists(imgFile) && !Files.exists(musicFile), "Files still in root");

            boolean undoSuccess = organizer.undo(batch, null);
            assertTrue("Undo completed successfully", undoSuccess, "Undo reported failure");
            assertTrue("PDF restored to root", Files.exists(docFile), "PDF not restored");
            assertTrue("JPG restored to root", Files.exists(imgFile), "JPG not restored");
            assertTrue("MP3 restored to root", Files.exists(musicFile), "MP3 not restored");

            Files.deleteIfExists(docFile);
            Files.deleteIfExists(imgFile);
            Files.deleteIfExists(musicFile);
            Files.deleteIfExists(testRoot.resolve("Documents"));
            Files.deleteIfExists(testRoot.resolve("Images"));
            Files.deleteIfExists(testRoot.resolve("Audio"));
            Files.deleteIfExists(testRoot);
        } catch (IOException e) {
            assertTrue("Sortify test exception", false, e.getMessage());
        }
    }

    private static void testDuplicateDetectionSha256() {
        System.out.println("\n[6] Testing SHA-256 Multi-phase Duplicate Detection...");
        try {
            Path tempDir = Files.createTempDirectory("diskpilot_duplicate_test");

            String identicalPayload = "EXACT_IDENTICAL_BINARY_PAYLOAD_FOR_SHA256_VERIFICATION_1234567890";
            Path fileA = tempDir.resolve("original.bin");
            Path fileB = tempDir.resolve("copy_of_original.bin");
            Path fileC = tempDir.resolve("different.bin");

            Files.writeString(fileA, identicalPayload);
            Files.writeString(fileB, identicalPayload);
            Files.writeString(fileC, "SOME_DIFFERENT_PAYLOAD_THAT_WILL_NOT_MATCH_THE_HASH");

            FileScanner scanner = new FileScanner();
            ScanResult scan = scanner.scan(tempDir, false, null);

            DuplicateDetector detector = new DuplicateDetector();
            List<DuplicateGroup> duplicates = detector.findDuplicates(scan.getAllFiles(), null);

            assertTrue("Exactly 1 duplicate group discovered", duplicates.size() == 1, "Expected 1 group, got " + duplicates.size());
            if (!duplicates.isEmpty()) {
                DuplicateGroup group = duplicates.get(0);
                assertTrue("Group contains exactly 2 identical files", group.getCount() == 2, "Expected 2 files, got " + group.getCount());
                assertTrue("Wasted bytes matches file size", group.getWastedBytes() == fileA.toFile().length(), "Wasted space mismatch");
            }

            Files.deleteIfExists(fileA);
            Files.deleteIfExists(fileB);
            Files.deleteIfExists(fileC);
            Files.deleteIfExists(tempDir);
        } catch (IOException e) {
            assertTrue("Duplicate test exception", false, e.getMessage());
        }
    }

    private static void testHistoryPersistence() {
        System.out.println("\n[7] Testing Activity History & Audit Log Persistence...");
        HistoryManager manager = HistoryManager.getInstance();
        int initialCount = manager.getEntries().size();

        manager.recordScan(Path.of("C:\\TestFolder"), 100, 1024 * 1024 * 50, 450, false);
        manager.recordWarning("Security Alert Test", "Protected file skipped", "C:\\Windows\\notepad.exe");

        assertTrue("Entries incremented by 2", manager.getEntries().size() == initialCount + 2, "History entries count mismatch");
        assertTrue("Latest entry is Warning", manager.getEntries().get(0).getType() == HistoryEntry.OperationType.WARNING, "Expected Warning entry");
        assertTrue("Second latest entry is Scan", manager.getEntries().get(1).getType() == HistoryEntry.OperationType.SCAN, "Expected Scan entry");
    }
}

