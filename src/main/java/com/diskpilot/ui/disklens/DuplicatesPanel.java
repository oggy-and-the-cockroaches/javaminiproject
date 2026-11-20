package com.diskpilot.ui.disklens;

import com.diskpilot.model.DuplicateGroup;
import com.diskpilot.model.FileRecord;
import com.diskpilot.model.ScanResult;
import com.diskpilot.service.DuplicateDetector;
import com.diskpilot.service.FileScanner;
import com.diskpilot.service.HistoryManager;
import com.diskpilot.service.SafetyValidator;
import com.diskpilot.ui.Theme;
import com.diskpilot.ui.components.ModernButton;
import com.diskpilot.ui.components.ModernCard;
import com.diskpilot.ui.components.ModernTable;
import com.diskpilot.util.FileUtils;
import com.diskpilot.util.FormatUtils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class DuplicatesPanel extends JPanel {

    private final DuplicateDetector duplicateDetector;
    private final FileScanner fileScanner;
    private final HistoryManager historyManager;

    private JTextField scanFolderField;
    private JCheckBox recursiveCheck;
    private ModernButton scanBtn;
    private ModernButton cancelBtn;
    private JProgressBar progressBar;
    private JLabel statusLabel;

    private DefaultTableModel dupTableModel;
    private ModernTable dupTable;

    private JLabel summaryLabel;
    private ModernButton deleteSelectedBtn;

    private List<DuplicateGroup> duplicateGroups = new ArrayList<>();
    private final List<DuplicateRowItem> tableItems = new ArrayList<>();
    private SwingWorker<List<DuplicateGroup>, String> currentWorker;
    private Path currentTarget;

    public static class DuplicateRowItem {
        public boolean selected = false;
        public final int groupId;
        public final FileRecord file;
        public final String sha256;
        public final boolean isOriginal;

        public DuplicateRowItem(int groupId, FileRecord file, String sha256, boolean isOriginal) {
            this.groupId = groupId;
            this.file = file;
            this.sha256 = sha256;
            this.isOriginal = isOriginal;
        }
    }

    public DuplicatesPanel() {
        this.duplicateDetector = new DuplicateDetector();
        this.fileScanner = new FileScanner();
        this.historyManager = HistoryManager.getInstance();

        setLayout(new BorderLayout());
        setBackground(Theme.BG_APP);
        setBorder(new EmptyBorder(20, 24, 20, 24));

        JPanel mainContent = new JPanel();
        mainContent.setLayout(new BoxLayout(mainContent, BoxLayout.Y_AXIS));
        mainContent.setOpaque(false);

        mainContent.add(createHeaderPanel());
        mainContent.add(Box.createVerticalStrut(14));
        mainContent.add(createControlsCard());
        mainContent.add(Box.createVerticalStrut(14));
        mainContent.add(createProgressCard());
        mainContent.add(Box.createVerticalStrut(14));
        mainContent.add(createTableCard());

        add(mainContent, BorderLayout.CENTER);
    }

    private JPanel createHeaderPanel() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setOpaque(false);

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        left.setOpaque(false);

        JLabel title = new JLabel("Duplicate Hunter: SHA-256 Content Verifier");
        title.setFont(Theme.FONT_TITLE);
        title.setForeground(Theme.TEXT_PRIMARY);

        JLabel sub = new JLabel("  •  Exact Binary Hash Matching & Storage Reclamation");
        sub.setFont(Theme.FONT_REGULAR);
        sub.setForeground(Theme.TEXT_MUTED);

        left.add(title);
        left.add(sub);
        bar.add(left, BorderLayout.WEST);
        return bar;
    }

    private JPanel createControlsCard() {
        ModernCard card = new ModernCard(new BorderLayout(12, 10));

        JPanel pathRow = new JPanel(new BorderLayout(10, 0));
        pathRow.setOpaque(false);

        JLabel label = new JLabel("Search Folder for Duplicates:");
        label.setFont(Theme.FONT_SUBHEADER);
        label.setForeground(Theme.TEXT_PRIMARY);

        scanFolderField = new JTextField(System.getProperty("user.home"));
        scanFolderField.setFont(Theme.FONT_REGULAR);
        scanFolderField.setBackground(Theme.BG_INPUT);
        scanFolderField.setForeground(Theme.TEXT_PRIMARY);
        scanFolderField.setCaretColor(Theme.ACCENT_CYAN);
        scanFolderField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER_CARD, 1),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)
        ));

        ModernButton browseBtn = new ModernButton("📁 Browse...", ModernButton.ButtonStyle.SECONDARY);
        browseBtn.addActionListener(e -> chooseDirectory());

        pathRow.add(label, BorderLayout.WEST);
        pathRow.add(scanFolderField, BorderLayout.CENTER);
        pathRow.add(browseBtn, BorderLayout.EAST);

        JPanel optionsRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        optionsRow.setOpaque(false);

        recursiveCheck = new JCheckBox("Recursive Scan", true);
        recursiveCheck.setFont(Theme.FONT_REGULAR);
        recursiveCheck.setForeground(Theme.TEXT_PRIMARY);
        recursiveCheck.setOpaque(false);

        scanBtn = new ModernButton("👥 Find Duplicate Files", ModernButton.ButtonStyle.PRIMARY);
        scanBtn.addActionListener(e -> startDuplicateSearch());

        cancelBtn = new ModernButton("✖ Cancel Search", ModernButton.ButtonStyle.DANGER);
        cancelBtn.setEnabled(false);
        cancelBtn.addActionListener(e -> cancelSearch());

        optionsRow.add(recursiveCheck);
        optionsRow.add(Box.createHorizontalStrut(10));
        optionsRow.add(scanBtn);
        optionsRow.add(cancelBtn);

        card.add(pathRow, BorderLayout.NORTH);
        card.add(optionsRow, BorderLayout.SOUTH);
        return card;
    }

    private JPanel createProgressCard() {
        ModernCard card = new ModernCard(new BorderLayout(0, 6));
        card.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));

        statusLabel = new JLabel("Status: Ready to detect duplicate content via multi-phase SHA-256 analysis.");
        statusLabel.setFont(Theme.FONT_REGULAR);
        statusLabel.setForeground(Theme.TEXT_SECONDARY);

        progressBar = new JProgressBar();
        progressBar.setOpaque(false);
        progressBar.setForeground(Theme.ACCENT_CYAN);
        progressBar.setBackground(Theme.BG_INPUT);
        progressBar.setBorder(BorderFactory.createLineBorder(Theme.BORDER_SUBTLE, 1));
        progressBar.setPreferredSize(new Dimension(100, 10));

        card.add(statusLabel, BorderLayout.NORTH);
        card.add(progressBar, BorderLayout.CENTER);
        return card;
    }

    private JPanel createTableCard() {
        ModernCard card = new ModernCard(new BorderLayout(0, 12));

        JPanel toolbar = new JPanel(new BorderLayout());
        toolbar.setOpaque(false);

        summaryLabel = new JLabel("Discovered Duplicate Groups: 0 | Reclaimable Space: 0 B");
        summaryLabel.setFont(Theme.FONT_HEADER);
        summaryLabel.setForeground(Theme.TEXT_PRIMARY);

        JPanel actionButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actionButtons.setOpaque(false);

        ModernButton keepNewestBtn = new ModernButton("Keep Newest", ModernButton.ButtonStyle.SECONDARY);
        keepNewestBtn.addActionListener(e -> smartSelect(true));

        ModernButton keepOldestBtn = new ModernButton("Keep Oldest", ModernButton.ButtonStyle.SECONDARY);
        keepOldestBtn.addActionListener(e -> smartSelect(false));

        ModernButton clearBtn = new ModernButton("Clear Selection", ModernButton.ButtonStyle.SECONDARY);
        clearBtn.addActionListener(e -> clearSelection());

        ModernButton openExplorerBtn = new ModernButton("📂 Open in Explorer", ModernButton.ButtonStyle.SECONDARY);
        openExplorerBtn.addActionListener(e -> openSelectedInExplorer());

        deleteSelectedBtn = new ModernButton("🗑 Delete Selected Duplicates", ModernButton.ButtonStyle.DANGER);
        deleteSelectedBtn.setEnabled(false);
        deleteSelectedBtn.addActionListener(e -> deleteSelectedDuplicates());

        actionButtons.add(keepNewestBtn);
        actionButtons.add(keepOldestBtn);
        actionButtons.add(clearBtn);
        actionButtons.add(openExplorerBtn);
        actionButtons.add(deleteSelectedBtn);

        toolbar.add(summaryLabel, BorderLayout.WEST);
        toolbar.add(actionButtons, BorderLayout.EAST);

        String[] cols = {"Select", "Group", "File Name", "Size", "SHA-256 (Prefix)", "Last Modified", "Full File Path"};
        dupTableModel = new DefaultTableModel(cols, 0) {
            @Override
            public Class<?> getColumnClass(int col) {
                return col == 0 ? Boolean.class : String.class;
            }

            @Override
            public boolean isCellEditable(int row, int col) {
                return col == 0;
            }
        };

        dupTable = new ModernTable(dupTableModel);
        dupTable.getColumnModel().getColumn(0).setPreferredWidth(50);
        dupTable.getColumnModel().getColumn(1).setPreferredWidth(60);
        dupTable.getColumnModel().getColumn(2).setPreferredWidth(160);
        dupTable.getColumnModel().getColumn(3).setPreferredWidth(85);
        dupTable.getColumnModel().getColumn(4).setPreferredWidth(110);
        dupTable.getColumnModel().getColumn(5).setPreferredWidth(130);
        dupTable.getColumnModel().getColumn(6).setPreferredWidth(260);

        dupTableModel.addTableModelListener(e -> {
            if (e.getColumn() == 0 && e.getFirstRow() >= 0 && e.getFirstRow() < tableItems.size()) {
                Boolean checked = (Boolean) dupTableModel.getValueAt(e.getFirstRow(), 0);
                tableItems.get(e.getFirstRow()).selected = (checked != null && checked);
                updateSelectionState();
            }
        });

        JScrollPane scroll = ModernTable.createModernScrollPane(dupTable);
        scroll.setPreferredSize(new Dimension(800, 360));

        card.add(toolbar, BorderLayout.NORTH);
        card.add(scroll, BorderLayout.CENTER);
        return card;
    }

    private void chooseDirectory() {
        JFileChooser chooser = new JFileChooser(scanFolderField.getText().trim());
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setDialogTitle("Select Folder to Scan for Duplicates");
        int res = chooser.showOpenDialog(this);
        if (res == JFileChooser.APPROVE_OPTION) {
            File selected = chooser.getSelectedFile();
            if (selected != null) {
                scanFolderField.setText(selected.getAbsolutePath());
            }
        }
    }

    public void startScanForFiles(List<FileRecord> files) {
        if (files == null || files.isEmpty()) return;
        runDuplicateDetection(files, Paths.get(System.getProperty("user.home")));
    }

    private void startDuplicateSearch() {
        String pathStr = scanFolderField.getText().trim();
        if (pathStr.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please select a target folder to scan.", "Invalid Path", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Path target = Paths.get(pathStr);
        if (!Files.exists(target) || !Files.isDirectory(target)) {
            JOptionPane.showMessageDialog(this, "Folder does not exist: " + pathStr, "Directory Missing", JOptionPane.ERROR_MESSAGE);
            return;
        }

        currentTarget = target;
        boolean recursive = recursiveCheck.isSelected();

        scanBtn.setEnabled(false);
        cancelBtn.setEnabled(true);
        progressBar.setIndeterminate(true);
        statusLabel.setText("Scanning file metadata in " + target.getFileName() + "...");

        currentWorker = new SwingWorker<List<DuplicateGroup>, String>() {
            @Override
            protected List<DuplicateGroup> doInBackground() {
                publish("Indexing files and sizes...");
                ScanResult scan = fileScanner.scan(target, recursive, (filesScanned, bytesScanned, currentPath) -> {
                    publish("Scanned " + filesScanned + " files. Preparing candidate size groups...");
                });

                if (fileScanner.isCancelled()) {
                    return new ArrayList<>();
                }

                publish("Running multi-phase SHA-256 analysis...");
                return duplicateDetector.findDuplicates(scan.getAllFiles(), (processed, total, currentFileName) -> {
                    publish(String.format("Hashing content: %d / %d candidates - %s", processed, total, currentFileName));
                });
            }

            @Override
            protected void process(List<String> chunks) {
                if (!chunks.isEmpty()) {
                    statusLabel.setText(chunks.get(chunks.size() - 1));
                }
            }

            @Override
            protected void done() {
                scanBtn.setEnabled(true);
                cancelBtn.setEnabled(false);
                progressBar.setIndeterminate(false);

                try {
                    duplicateGroups = get();
                    populateDuplicateTable();
                } catch (Exception e) {
                    statusLabel.setText("Duplicate search error: " + e.getMessage());
                }
            }
        };

        currentWorker.execute();
    }

    private void runDuplicateDetection(List<FileRecord> files, Path target) {
        currentTarget = target;
        scanBtn.setEnabled(false);
        cancelBtn.setEnabled(true);
        progressBar.setIndeterminate(true);

        currentWorker = new SwingWorker<List<DuplicateGroup>, String>() {
            @Override
            protected List<DuplicateGroup> doInBackground() {
                publish("Running SHA-256 duplicate verification on " + files.size() + " files...");
                return duplicateDetector.findDuplicates(files, (processed, total, currentFileName) -> {
                    publish(String.format("Computing SHA-256: %d / %d - %s", processed, total, currentFileName));
                });
            }

            @Override
            protected void process(List<String> chunks) {
                if (!chunks.isEmpty()) statusLabel.setText(chunks.get(chunks.size() - 1));
            }

            @Override
            protected void done() {
                scanBtn.setEnabled(true);
                cancelBtn.setEnabled(false);
                progressBar.setIndeterminate(false);

                try {
                    duplicateGroups = get();
                    populateDuplicateTable();
                } catch (Exception e) {
                    statusLabel.setText("Duplicate search error: " + e.getMessage());
                }
            }
        };
        currentWorker.execute();
    }

    private void cancelSearch() {
        if (fileScanner != null) fileScanner.cancel();
        if (duplicateDetector != null) duplicateDetector.cancel();
        if (currentWorker != null && !currentWorker.isDone()) currentWorker.cancel(true);
        statusLabel.setText("Search cancelled by user.");
        cancelBtn.setEnabled(false);
    }

    private void populateDuplicateTable() {
        dupTableModel.setRowCount(0);
        tableItems.clear();

        long totalWasted = 0;
        int totalDuplicateFiles = 0;

        int groupNum = 1;
        for (DuplicateGroup dg : duplicateGroups) {
            totalWasted += dg.getWastedBytes();
            totalDuplicateFiles += dg.getCount();

            List<FileRecord> files = dg.getFiles();
            String hashPrefix = dg.getSha256().substring(0, Math.min(10, dg.getSha256().length())) + "...";

            for (int i = 0; i < files.size(); i++) {
                FileRecord f = files.get(i);
                boolean isOriginal = (i == 0);
                DuplicateRowItem item = new DuplicateRowItem(groupNum, f, dg.getSha256(), isOriginal);
                tableItems.add(item);

                dupTableModel.addRow(new Object[]{
                        item.selected,
                        "#" + groupNum,
                        f.getFileName(),
                        FormatUtils.formatBytes(f.getSize()),
                        hashPrefix,
                        FormatUtils.formatTimestamp(f.getLastModified()),
                        f.getAbsolutePath()
                });
            }
            groupNum++;
        }

        summaryLabel.setText(String.format("Duplicate Groups: %d (%d files)  |  Reclaimable Storage: %s",
                duplicateGroups.size(), totalDuplicateFiles, FormatUtils.formatBytes(totalWasted)));
        statusLabel.setText(String.format("Detection completed. %d duplicate groups found. Use smart selectors to pick redundant copies.", duplicateGroups.size()));

        if (currentTarget != null) {
            historyManager.recordDuplicateScan(currentTarget, duplicateGroups.size(), totalDuplicateFiles, totalWasted);
        }

        updateSelectionState();
    }

    private void smartSelect(boolean keepNewest) {

        tableItems.clear();
        dupTableModel.setRowCount(0);

        int groupNum = 1;
        for (DuplicateGroup dg : duplicateGroups) {
            List<FileRecord> files = new ArrayList<>(dg.getFiles());
            if (keepNewest) {

                files.sort(Comparator.comparing(FileRecord::getLastModified).reversed());
            } else {

                files.sort(Comparator.comparing(FileRecord::getLastModified));
            }

            String hashPrefix = dg.getSha256().substring(0, Math.min(10, dg.getSha256().length())) + "...";

            for (int i = 0; i < files.size(); i++) {
                FileRecord f = files.get(i);
                boolean isKept = (i == 0);
                boolean markForDelete = !isKept;

                DuplicateRowItem item = new DuplicateRowItem(groupNum, f, dg.getSha256(), isKept);
                item.selected = markForDelete;
                tableItems.add(item);

                dupTableModel.addRow(new Object[]{
                        item.selected,
                        "#" + groupNum,
                        f.getFileName() + (isKept ? " [PRESERVE]" : " [COPY]"),
                        FormatUtils.formatBytes(f.getSize()),
                        hashPrefix,
                        FormatUtils.formatTimestamp(f.getLastModified()),
                        f.getAbsolutePath()
                });
            }
            groupNum++;
        }

        updateSelectionState();
    }

    private void clearSelection() {
        for (int i = 0; i < dupTableModel.getRowCount(); i++) {
            dupTableModel.setValueAt(false, i, 0);
        }
        for (DuplicateRowItem item : tableItems) {
            item.selected = false;
        }
        updateSelectionState();
    }

    private void updateSelectionState() {
        int count = 0;
        long bytes = 0;
        for (DuplicateRowItem item : tableItems) {
            if (item.selected) {
                count++;
                bytes += item.file.getSize();
            }
        }
        deleteSelectedBtn.setEnabled(count > 0);
        deleteSelectedBtn.setText(count > 0 ?
                String.format("🗑 Delete %d Selected (%s)", count, FormatUtils.formatBytes(bytes)) :
                "🗑 Delete Selected Duplicates");
    }

    private void openSelectedInExplorer() {
        int row = dupTable.getSelectedRow();
        if (row >= 0 && row < tableItems.size()) {
            FileUtils.openInFileExplorer(tableItems.get(row).file.getPath());
        } else {
            JOptionPane.showMessageDialog(this, "Select a file from the table to view in Explorer.", "No Selection", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void deleteSelectedDuplicates() {
        List<DuplicateRowItem> toDelete = new ArrayList<>();
        long totalBytes = 0;

        for (DuplicateRowItem item : tableItems) {
            if (item.selected) {
                toDelete.add(item);
                totalBytes += item.file.getSize();
            }
        }

        if (toDelete.isEmpty()) return;

        String warnMsg = String.format("SAFETY CONFIRMATION:\n\n" +
                "You are about to permanently remove %d duplicate files.\n" +
                "Total storage reclaimed: %s\n\n" +
                "Note: Only the marked redundant copies will be deleted. Preserved originals will remain untouched.\n\n" +
                "Are you sure you wish to proceed?",
                toDelete.size(), FormatUtils.formatBytes(totalBytes));

        int choice = JOptionPane.showConfirmDialog(this, warnMsg, "Confirm Duplicate Deletion", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (choice != JOptionPane.YES_OPTION) {
            return;
        }

        int deletedCount = 0;
        int failedCount = 0;

        for (DuplicateRowItem item : toDelete) {
            Path path = item.file.getPath();

            if (!SafetyValidator.isSafeToModify(path)) {
                failedCount++;
                continue;
            }

            try {
                if (Files.exists(path)) {
                    Files.delete(path);
                    deletedCount++;
                }
            } catch (Exception e) {
                failedCount++;
            }
        }

        JOptionPane.showMessageDialog(this,
                String.format("Duplicate cleanup completed!\n\nDeleted: %d files\nFailed/Protected: %d\nReclaimed: %s",
                        deletedCount, failedCount, FormatUtils.formatBytes(totalBytes)),
                "Cleanup Finished", JOptionPane.INFORMATION_MESSAGE);

        startDuplicateSearch();
    }
}

