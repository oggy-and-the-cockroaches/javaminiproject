package com.diskpilot.ui.disklens;

import com.diskpilot.model.FileCategory;
import com.diskpilot.model.FileRecord;
import com.diskpilot.model.FolderRecord;
import com.diskpilot.model.ScanResult;
import com.diskpilot.service.FileScanner;
import com.diskpilot.service.HistoryManager;
import com.diskpilot.ui.Theme;
import com.diskpilot.ui.components.CategoryChartPanel;
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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class DiskLensPanel extends JPanel {

    public interface ScanCompletionListener {
        void onScanCompleted(ScanResult result);
    }

    private final ScanCompletionListener completionListener;
    private final FileScanner fileScanner;
    private final HistoryManager historyManager;

    private JTextField folderPathField;
    private JCheckBox recursiveCheck;
    private ModernButton scanBtn;
    private ModernButton cancelBtn;
    private JProgressBar progressBar;
    private JLabel statusLabel;

    private JComboBox<String> sizeFilterCombo;
    private JTextField searchFilterField;

    private DefaultTableModel largestFilesModel;
    private ModernTable largestFilesTable;
    private DefaultTableModel largestFoldersModel;
    private ModernTable largestFoldersTable;
    private CategoryChartPanel categoryChart;

    private SwingWorker<ScanResult, String> currentWorker;
    private ScanResult lastResult;
    private List<FileRecord> currentFilteredFiles = new ArrayList<>();

    public DiskLensPanel(ScanCompletionListener completionListener) {
        this.completionListener = completionListener;
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
        mainContent.add(createMainAnalyticsSection());

        JScrollPane scrollPane = new JScrollPane(mainContent);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        ModernTable.styleScrollBar(scrollPane.getVerticalScrollBar());
        ModernTable.styleScrollBar(scrollPane.getHorizontalScrollBar());

        add(scrollPane, BorderLayout.CENTER);
    }

    private JPanel createHeaderPanel() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setOpaque(false);

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        left.setOpaque(false);

        JLabel title = new JLabel("DiskLens: Storage Analyzer");
        title.setFont(Theme.FONT_TITLE);
        title.setForeground(Theme.TEXT_PRIMARY);

        JLabel sub = new JLabel("  •  Recursive Space Breakdown & Largest Files Ranking");
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

        JLabel label = new JLabel("Target Directory / Drive:");
        label.setFont(Theme.FONT_SUBHEADER);
        label.setForeground(Theme.TEXT_PRIMARY);

        folderPathField = new JTextField(System.getProperty("user.home"));
        folderPathField.setFont(Theme.FONT_REGULAR);
        folderPathField.setBackground(Theme.BG_INPUT);
        folderPathField.setForeground(Theme.TEXT_PRIMARY);
        folderPathField.setCaretColor(Theme.ACCENT_CYAN);
        folderPathField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER_CARD, 1),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)
        ));

        ModernButton browseBtn = new ModernButton("📁 Browse...", ModernButton.ButtonStyle.SECONDARY);
        browseBtn.addActionListener(e -> chooseDirectory());

        pathRow.add(label, BorderLayout.WEST);
        pathRow.add(folderPathField, BorderLayout.CENTER);
        pathRow.add(browseBtn, BorderLayout.EAST);

        JPanel optionsRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        optionsRow.setOpaque(false);

        recursiveCheck = new JCheckBox("Recursive Subdirectory Scan", true);
        recursiveCheck.setFont(Theme.FONT_REGULAR);
        recursiveCheck.setForeground(Theme.TEXT_PRIMARY);
        recursiveCheck.setOpaque(false);

        scanBtn = new ModernButton("⚡ Start Deep Scan", ModernButton.ButtonStyle.PRIMARY);
        scanBtn.addActionListener(e -> startScan());

        cancelBtn = new ModernButton("✖ Cancel Scan", ModernButton.ButtonStyle.DANGER);
        cancelBtn.setEnabled(false);
        cancelBtn.addActionListener(e -> cancelScan());

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

        statusLabel = new JLabel("Status: Idle. Select a folder or drive to begin storage analysis.");
        statusLabel.setFont(Theme.FONT_REGULAR);
        statusLabel.setForeground(Theme.TEXT_SECONDARY);

        progressBar = new JProgressBar();
        progressBar.setOpaque(false);
        progressBar.setForeground(Theme.ACCENT_CYAN);
        progressBar.setBackground(Theme.BG_INPUT);
        progressBar.setBorder(BorderFactory.createLineBorder(Theme.BORDER_SUBTLE, 1));
        progressBar.setPreferredSize(new Dimension(100, 12));

        card.add(statusLabel, BorderLayout.NORTH);
        card.add(progressBar, BorderLayout.CENTER);
        return card;
    }

    private JPanel createMainAnalyticsSection() {
        JPanel section = new JPanel();
        section.setLayout(new BoxLayout(section, BoxLayout.Y_AXIS));
        section.setOpaque(false);

        JPanel topRow = new JPanel(new GridLayout(1, 2, 14, 0));
        topRow.setOpaque(false);

        ModernCard chartCard = new ModernCard(new BorderLayout());
        JLabel chartTitle = new JLabel("Category Distribution");
        chartTitle.setFont(Theme.FONT_HEADER);
        chartTitle.setForeground(Theme.TEXT_PRIMARY);
        chartTitle.setBorder(new EmptyBorder(0, 0, 10, 0));
        categoryChart = new CategoryChartPanel();
        chartCard.add(chartTitle, BorderLayout.NORTH);
        chartCard.add(categoryChart, BorderLayout.CENTER);

        ModernCard folderCard = new ModernCard(new BorderLayout());
        JLabel folderTitle = new JLabel("Largest Directories Discovered");
        folderTitle.setFont(Theme.FONT_HEADER);
        folderTitle.setForeground(Theme.TEXT_PRIMARY);
        folderTitle.setBorder(new EmptyBorder(0, 0, 10, 0));

        String[] folderCols = {"Folder Name", "Total Size", "Files", "Full Path"};
        largestFoldersModel = new DefaultTableModel(folderCols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };
        largestFoldersTable = new ModernTable(largestFoldersModel);
        largestFoldersTable.getColumnModel().getColumn(0).setPreferredWidth(130);
        largestFoldersTable.getColumnModel().getColumn(1).setPreferredWidth(80);
        largestFoldersTable.getColumnModel().getColumn(2).setPreferredWidth(60);
        largestFoldersTable.getColumnModel().getColumn(3).setPreferredWidth(180);

        JScrollPane folderScroll = ModernTable.createModernScrollPane(largestFoldersTable);
        folderScroll.setPreferredSize(new Dimension(300, 200));

        folderCard.add(folderTitle, BorderLayout.NORTH);
        folderCard.add(folderScroll, BorderLayout.CENTER);

        topRow.add(chartCard);
        topRow.add(folderCard);

        ModernCard filesCard = new ModernCard(new BorderLayout(0, 10));

        JPanel filterBar = new JPanel(new BorderLayout(10, 0));
        filterBar.setOpaque(false);

        JLabel filesTitle = new JLabel("Top Largest Files");
        filesTitle.setFont(Theme.FONT_HEADER);
        filesTitle.setForeground(Theme.TEXT_PRIMARY);

        JPanel filtersRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        filtersRight.setOpaque(false);

        JLabel sizeFilterLabel = new JLabel("Min Size:");
        sizeFilterLabel.setFont(Theme.FONT_REGULAR);
        sizeFilterLabel.setForeground(Theme.TEXT_SECONDARY);

        sizeFilterCombo = new JComboBox<>(new String[]{"All Sizes", "> 10 MB", "> 50 MB", "> 100 MB", "> 500 MB", "> 1 GB"});
        sizeFilterCombo.setFont(Theme.FONT_REGULAR);
        sizeFilterCombo.setBackground(Theme.BG_CARD);
        sizeFilterCombo.setForeground(Theme.TEXT_PRIMARY);
        sizeFilterCombo.addActionListener(e -> applyFilter());

        JLabel searchLabel = new JLabel("Search:");
        searchLabel.setFont(Theme.FONT_REGULAR);
        searchLabel.setForeground(Theme.TEXT_SECONDARY);

        searchFilterField = new JTextField(12);
        searchFilterField.setFont(Theme.FONT_REGULAR);
        searchFilterField.setBackground(Theme.BG_INPUT);
        searchFilterField.setForeground(Theme.TEXT_PRIMARY);
        searchFilterField.setCaretColor(Theme.ACCENT_CYAN);
        searchFilterField.addActionListener(e -> applyFilter());

        ModernButton applyFilterBtn = new ModernButton("Filter", ModernButton.ButtonStyle.SECONDARY);
        applyFilterBtn.addActionListener(e -> applyFilter());

        ModernButton openExplorerBtn = new ModernButton("📂 Open in Explorer", ModernButton.ButtonStyle.SECONDARY);
        openExplorerBtn.addActionListener(e -> openSelectedInExplorer());

        filtersRight.add(sizeFilterLabel);
        filtersRight.add(sizeFilterCombo);
        filtersRight.add(searchLabel);
        filtersRight.add(searchFilterField);
        filtersRight.add(applyFilterBtn);
        filtersRight.add(openExplorerBtn);

        filterBar.add(filesTitle, BorderLayout.WEST);
        filterBar.add(filtersRight, BorderLayout.EAST);

        String[] fileCols = {"File Name", "Size", "Category", "Last Modified", "Path"};
        largestFilesModel = new DefaultTableModel(fileCols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };
        largestFilesTable = new ModernTable(largestFilesModel);
        largestFilesTable.getColumnModel().getColumn(0).setPreferredWidth(160);
        largestFilesTable.getColumnModel().getColumn(1).setPreferredWidth(85);
        largestFilesTable.getColumnModel().getColumn(2).setPreferredWidth(90);
        largestFilesTable.getColumnModel().getColumn(3).setPreferredWidth(130);
        largestFilesTable.getColumnModel().getColumn(4).setPreferredWidth(260);

        JScrollPane filesScroll = ModernTable.createModernScrollPane(largestFilesTable);
        filesScroll.setPreferredSize(new Dimension(600, 260));

        filesCard.add(filterBar, BorderLayout.NORTH);
        filesCard.add(filesScroll, BorderLayout.CENTER);

        section.add(topRow);
        section.add(Box.createVerticalStrut(14));
        section.add(filesCard);

        return section;
    }

    private void chooseDirectory() {
        JFileChooser chooser = new JFileChooser(folderPathField.getText().trim());
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setDialogTitle("Select Folder to Analyze");
        int res = chooser.showOpenDialog(this);
        if (res == JFileChooser.APPROVE_OPTION) {
            File selected = chooser.getSelectedFile();
            if (selected != null) {
                folderPathField.setText(selected.getAbsolutePath());
            }
        }
    }

    public void startScanPath(String path) {
        if (path != null && !path.isBlank()) {
            folderPathField.setText(path);
            startScan();
        }
    }

    private void startScan() {
        String pathStr = folderPathField.getText().trim();
        if (pathStr.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter or select a valid target folder path.", "Invalid Path", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Path target = Paths.get(pathStr);
        if (!java.nio.file.Files.exists(target)) {
            JOptionPane.showMessageDialog(this, "The specified directory does not exist: " + pathStr, "Folder Not Found", JOptionPane.ERROR_MESSAGE);
            return;
        }

        boolean recursive = recursiveCheck.isSelected();

        scanBtn.setEnabled(false);
        cancelBtn.setEnabled(true);
        progressBar.setIndeterminate(true);
        statusLabel.setText("Scanning directory: " + target + "...");

        currentWorker = new SwingWorker<ScanResult, String>() {
            @Override
            protected ScanResult doInBackground() {
                return fileScanner.scan(target, recursive, (filesScanned, bytesScanned, currentPath) -> {
                    publish(String.format("Scanning: %s files (%s) - %s",
                            FormatUtils.formatNumber(filesScanned),
                            FormatUtils.formatBytes(bytesScanned),
                            currentPath.getFileName() != null ? currentPath.getFileName().toString() : currentPath.toString()));
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
                progressBar.setIndeterminate(false);
                scanBtn.setEnabled(true);
                cancelBtn.setEnabled(false);

                try {
                    lastResult = get();
                    boolean cancelled = lastResult.isCancelled();

                    statusLabel.setText(String.format("Scan %s: %s files (%s) scanned in %s",
                            cancelled ? "cancelled" : "completed",
                            FormatUtils.formatNumber(lastResult.getTotalFiles()),
                            FormatUtils.formatBytes(lastResult.getTotalBytes()),
                            FormatUtils.formatDuration(lastResult.getScanDurationMs())));

                    historyManager.recordScan(target, lastResult.getTotalFiles(), lastResult.getTotalBytes(), lastResult.getScanDurationMs(), cancelled);

                    categoryChart.setData(lastResult.getCategoryBytes());
                    populateFoldersTable(lastResult.getTopFolders());

                    currentFilteredFiles = new ArrayList<>(lastResult.getAllFiles());
                    applyFilter();

                    if (completionListener != null) {
                        completionListener.onScanCompleted(lastResult);
                    }

                } catch (Exception e) {
                    statusLabel.setText("Error during scan: " + e.getMessage());
                }
            }
        };

        currentWorker.execute();
    }

    private void cancelScan() {
        if (fileScanner != null) {
            fileScanner.cancel();
        }
        if (currentWorker != null && !currentWorker.isDone()) {
            currentWorker.cancel(true);
        }
        statusLabel.setText("Cancelling scan operation...");
        cancelBtn.setEnabled(false);
    }

    private void populateFoldersTable(List<FolderRecord> folders) {
        largestFoldersModel.setRowCount(0);
        if (folders != null) {
            for (FolderRecord f : folders) {
                largestFoldersModel.addRow(new Object[]{
                        f.getFolderName(),
                        FormatUtils.formatBytes(f.getTotalSize()),
                        FormatUtils.formatNumber(f.getFileCount()),
                        f.getAbsolutePath()
                });
            }
        }
    }

    private void applyFilter() {
        if (lastResult == null) return;

        String sizeChoice = (String) sizeFilterCombo.getSelectedItem();
        long minBytes = 0;
        if (sizeChoice != null) {
            if (sizeChoice.contains("10 MB")) minBytes = 10L * 1024 * 1024;
            else if (sizeChoice.contains("50 MB")) minBytes = 50L * 1024 * 1024;
            else if (sizeChoice.contains("100 MB")) minBytes = 100L * 1024 * 1024;
            else if (sizeChoice.contains("500 MB")) minBytes = 500L * 1024 * 1024;
            else if (sizeChoice.contains("1 GB")) minBytes = 1024L * 1024 * 1024;
        }

        String search = searchFilterField.getText().trim().toLowerCase();

        largestFilesModel.setRowCount(0);
        currentFilteredFiles.clear();

        for (FileRecord f : lastResult.getAllFiles()) {
            if (f.getSize() < minBytes) continue;
            if (!search.isEmpty()) {
                String name = f.getFileName().toLowerCase();
                String ext = f.getExtension();
                if (!name.contains(search) && !ext.contains(search)) {
                    continue;
                }
            }

            currentFilteredFiles.add(f);
            if (largestFilesModel.getRowCount() < 100) {
                largestFilesModel.addRow(new Object[]{
                        f.getFileName(),
                        FormatUtils.formatBytes(f.getSize()),
                        f.getCategory().getDisplayName(),
                        FormatUtils.formatTimestamp(f.getLastModified()),
                        f.getAbsolutePath()
                });
            }
        }
    }

    private void openSelectedInExplorer() {
        int row = largestFilesTable.getSelectedRow();
        if (row >= 0 && row < currentFilteredFiles.size()) {
            FileRecord selected = currentFilteredFiles.get(row);
            FileUtils.openInFileExplorer(selected.getPath());
        } else {
            JOptionPane.showMessageDialog(this, "Please select a file from the table to reveal in Explorer.", "No File Selected", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    public ScanResult getLastResult() {
        return lastResult;
    }
}

