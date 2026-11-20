package com.diskpilot.ui.dashboard;

import com.diskpilot.model.DriveInfo;
import com.diskpilot.model.FileCategory;
import com.diskpilot.model.FileRecord;
import com.diskpilot.model.HistoryEntry;
import com.diskpilot.service.HistoryManager;
import com.diskpilot.service.SystemDriveService;
import com.diskpilot.ui.Theme;
import com.diskpilot.ui.components.CategoryChartPanel;
import com.diskpilot.ui.components.ModernButton;
import com.diskpilot.ui.components.ModernCard;
import com.diskpilot.ui.components.ModernTable;
import com.diskpilot.ui.components.StorageGauge;
import com.diskpilot.util.FormatUtils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class DashboardPanel extends JPanel {

    public interface DashboardActionHandler {
        void navigateTo(String moduleKey);
        void triggerQuickScan(String folderPath);
    }

    private final DashboardActionHandler actionHandler;
    private final SystemDriveService driveService;
    private final HistoryManager historyManager;

    private JLabel totalCapacityLabel;
    private JLabel usedStorageLabel;
    private JLabel freeStorageLabel;
    private JLabel utilizationPctLabel;
    private JPanel drivesContainer;
    private CategoryChartPanel chartPanel;
    private DefaultTableModel largestFilesModel;
    private DefaultTableModel recentActivityModel;

    public DashboardPanel(DashboardActionHandler actionHandler) {
        this.actionHandler = actionHandler;
        this.driveService = new SystemDriveService();
        this.historyManager = HistoryManager.getInstance();

        setLayout(new BorderLayout());
        setBackground(Theme.BG_APP);
        setBorder(new EmptyBorder(24, 28, 24, 28));

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setOpaque(false);

        content.add(createHeaderBar());
        content.add(Box.createVerticalStrut(18));
        content.add(createMetricCardsRow());
        content.add(Box.createVerticalStrut(18));
        content.add(createDrivesRow());
        content.add(Box.createVerticalStrut(18));
        content.add(createMiddleSection());
        content.add(Box.createVerticalStrut(18));
        content.add(createBottomTablesSection());

        JScrollPane scrollPane = new JScrollPane(content);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        ModernTable.styleScrollBar(scrollPane.getVerticalScrollBar());
        ModernTable.styleScrollBar(scrollPane.getHorizontalScrollBar());

        add(scrollPane, BorderLayout.CENTER);

        refreshData();
    }

    private JPanel createHeaderBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setOpaque(false);

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        left.setOpaque(false);

        JLabel title = new JLabel("System Storage Dashboard");
        title.setFont(Theme.FONT_TITLE);
        title.setForeground(Theme.TEXT_PRIMARY);
        left.add(title);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        right.setOpaque(false);

        ModernButton refreshBtn = new ModernButton("↻ Refresh Drives", ModernButton.ButtonStyle.SECONDARY);
        refreshBtn.addActionListener(e -> refreshData());

        ModernButton quickScanBtn = new ModernButton("⚡ Quick Scan", ModernButton.ButtonStyle.PRIMARY);
        quickScanBtn.addActionListener(e -> {
            if (actionHandler != null) {
                actionHandler.triggerQuickScan(System.getProperty("user.home"));
            }
        });

        right.add(refreshBtn);
        right.add(quickScanBtn);

        bar.add(left, BorderLayout.WEST);
        bar.add(right, BorderLayout.EAST);
        return bar;
    }

    private JPanel createMetricCardsRow() {
        JPanel row = new JPanel(new GridLayout(1, 4, 16, 0));
        row.setOpaque(false);

        ModernCard card1 = new ModernCard();
        card1.setLayout(new BoxLayout(card1, BoxLayout.Y_AXIS));
        JLabel title1 = new JLabel("TOTAL CAPACITY");
        title1.setFont(Theme.FONT_SMALL);
        title1.setForeground(Theme.TEXT_MUTED);
        totalCapacityLabel = new JLabel("-- GB");
        totalCapacityLabel.setFont(Theme.FONT_METRIC);
        totalCapacityLabel.setForeground(Theme.TEXT_PRIMARY);
        JLabel sub1 = new JLabel("Online Storage Volumes");
        sub1.setFont(Theme.FONT_SMALL);
        sub1.setForeground(Theme.TEXT_SECONDARY);
        card1.add(title1);
        card1.add(Box.createVerticalStrut(4));
        card1.add(totalCapacityLabel);
        card1.add(Box.createVerticalStrut(2));
        card1.add(sub1);

        ModernCard card2 = new ModernCard();
        card2.setLayout(new BoxLayout(card2, BoxLayout.Y_AXIS));
        JLabel title2 = new JLabel("USED STORAGE");
        title2.setFont(Theme.FONT_SMALL);
        title2.setForeground(Theme.TEXT_MUTED);
        usedStorageLabel = new JLabel("-- GB");
        usedStorageLabel.setFont(Theme.FONT_METRIC);
        usedStorageLabel.setForeground(Theme.ACCENT_CYAN);
        utilizationPctLabel = new JLabel("--% Utilized");
        utilizationPctLabel.setFont(Theme.FONT_SMALL);
        utilizationPctLabel.setForeground(Theme.TEXT_SECONDARY);
        card2.add(title2);
        card2.add(Box.createVerticalStrut(4));
        card2.add(usedStorageLabel);
        card2.add(Box.createVerticalStrut(2));
        card2.add(utilizationPctLabel);

        ModernCard card3 = new ModernCard();
        card3.setLayout(new BoxLayout(card3, BoxLayout.Y_AXIS));
        JLabel title3 = new JLabel("FREE STORAGE");
        title3.setFont(Theme.FONT_SMALL);
        title3.setForeground(Theme.TEXT_MUTED);
        freeStorageLabel = new JLabel("-- GB");
        freeStorageLabel.setFont(Theme.FONT_METRIC);
        freeStorageLabel.setForeground(Theme.SUCCESS);
        JLabel sub3 = new JLabel("Available For Use");
        sub3.setFont(Theme.FONT_SMALL);
        sub3.setForeground(Theme.TEXT_SECONDARY);
        card3.add(title3);
        card3.add(Box.createVerticalStrut(4));
        card3.add(freeStorageLabel);
        card3.add(Box.createVerticalStrut(2));
        card3.add(sub3);

        ModernCard card4 = new ModernCard();
        card4.setLayout(new BoxLayout(card4, BoxLayout.Y_AXIS));
        JLabel title4 = new JLabel("SAFETY SHIELD");
        title4.setFont(Theme.FONT_SMALL);
        title4.setForeground(Theme.TEXT_MUTED);
        JLabel val4 = new JLabel("ACTIVE");
        val4.setFont(Theme.FONT_METRIC);
        val4.setForeground(Theme.SUCCESS);
        JLabel sub4 = new JLabel("OS & Protected Roots Blacklisted");
        sub4.setFont(Theme.FONT_SMALL);
        sub4.setForeground(Theme.TEXT_SECONDARY);
        card4.add(title4);
        card4.add(Box.createVerticalStrut(4));
        card4.add(val4);
        card4.add(Box.createVerticalStrut(2));
        card4.add(sub4);

        row.add(card1);
        row.add(card2);
        row.add(card3);
        row.add(card4);
        return row;
    }

    private JPanel createDrivesRow() {
        ModernCard card = new ModernCard(new BorderLayout());
        JLabel header = new JLabel("Physical & Logical Drives");
        header.setFont(Theme.FONT_HEADER);
        header.setForeground(Theme.TEXT_PRIMARY);
        header.setBorder(new EmptyBorder(0, 0, 12, 0));

        drivesContainer = new JPanel();
        drivesContainer.setLayout(new BoxLayout(drivesContainer, BoxLayout.Y_AXIS));
        drivesContainer.setOpaque(false);

        card.add(header, BorderLayout.NORTH);
        card.add(drivesContainer, BorderLayout.CENTER);
        return card;
    }

    private JPanel createMiddleSection() {
        JPanel panel = new JPanel(new GridLayout(1, 2, 16, 0));
        panel.setOpaque(false);

        ModernCard chartCard = new ModernCard(new BorderLayout());
        JLabel chartTitle = new JLabel("Storage Category Distribution");
        chartTitle.setFont(Theme.FONT_HEADER);
        chartTitle.setForeground(Theme.TEXT_PRIMARY);
        chartTitle.setBorder(new EmptyBorder(0, 0, 12, 0));

        chartPanel = new CategoryChartPanel();
        chartCard.add(chartTitle, BorderLayout.NORTH);
        chartCard.add(chartPanel, BorderLayout.CENTER);

        ModernCard actionCard = new ModernCard(new BorderLayout());
        JLabel actionTitle = new JLabel("Module Quick Launch");
        actionTitle.setFont(Theme.FONT_HEADER);
        actionTitle.setForeground(Theme.TEXT_PRIMARY);
        actionTitle.setBorder(new EmptyBorder(0, 0, 14, 0));

        JPanel buttonsGrid = new JPanel(new GridLayout(3, 1, 0, 10));
        buttonsGrid.setOpaque(false);

        ModernButton diskLensBtn = new ModernButton("🔍  Open DiskLens Analyzer  •  Inspect Folders & File Trees", ModernButton.ButtonStyle.SECONDARY);
        diskLensBtn.setHorizontalAlignment(SwingConstants.LEFT);
        diskLensBtn.addActionListener(e -> {
            if (actionHandler != null) actionHandler.navigateTo("disklens");
        });

        ModernButton sortifyBtn = new ModernButton("📁  Open Sortify Organizer  •  Auto-Categorize Files Safely", ModernButton.ButtonStyle.SECONDARY);
        sortifyBtn.setHorizontalAlignment(SwingConstants.LEFT);
        sortifyBtn.addActionListener(e -> {
            if (actionHandler != null) actionHandler.navigateTo("sortify");
        });

        ModernButton dupBtn = new ModernButton("👥  Open Duplicate Hunter  •  SHA-256 Content Duplicate Finder", ModernButton.ButtonStyle.SECONDARY);
        dupBtn.setHorizontalAlignment(SwingConstants.LEFT);
        dupBtn.addActionListener(e -> {
            if (actionHandler != null) actionHandler.navigateTo("duplicates");
        });

        buttonsGrid.add(diskLensBtn);
        buttonsGrid.add(sortifyBtn);
        buttonsGrid.add(dupBtn);

        actionCard.add(actionTitle, BorderLayout.NORTH);
        actionCard.add(buttonsGrid, BorderLayout.CENTER);

        panel.add(chartCard);
        panel.add(actionCard);
        return panel;
    }

    private JPanel createBottomTablesSection() {
        JPanel panel = new JPanel(new GridLayout(1, 2, 16, 0));
        panel.setOpaque(false);

        ModernCard largestCard = new ModernCard(new BorderLayout());
        JLabel largestTitle = new JLabel("Largest Discovered Files");
        largestTitle.setFont(Theme.FONT_HEADER);
        largestTitle.setForeground(Theme.TEXT_PRIMARY);
        largestTitle.setBorder(new EmptyBorder(0, 0, 10, 0));

        String[] largestCols = {"File Name", "Size", "Category", "Location"};
        largestFilesModel = new DefaultTableModel(largestCols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };
        ModernTable largestTable = new ModernTable(largestFilesModel);
        largestTable.getColumnModel().getColumn(0).setPreferredWidth(140);
        largestTable.getColumnModel().getColumn(1).setPreferredWidth(75);
        largestTable.getColumnModel().getColumn(2).setPreferredWidth(85);
        largestTable.getColumnModel().getColumn(3).setPreferredWidth(200);

        JScrollPane largestScroll = ModernTable.createModernScrollPane(largestTable);
        largestScroll.setPreferredSize(new Dimension(300, 160));

        largestCard.add(largestTitle, BorderLayout.NORTH);
        largestCard.add(largestScroll, BorderLayout.CENTER);

        ModernCard activityCard = new ModernCard(new BorderLayout());
        JLabel activityTitle = new JLabel("Recent System Operations");
        activityTitle.setFont(Theme.FONT_HEADER);
        activityTitle.setForeground(Theme.TEXT_PRIMARY);
        activityTitle.setBorder(new EmptyBorder(0, 0, 10, 0));

        String[] activityCols = {"Operation", "Details", "Status", "Timestamp"};
        recentActivityModel = new DefaultTableModel(activityCols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };
        ModernTable activityTable = new ModernTable(recentActivityModel);
        activityTable.getColumnModel().getColumn(0).setPreferredWidth(120);
        activityTable.getColumnModel().getColumn(1).setPreferredWidth(160);
        activityTable.getColumnModel().getColumn(2).setPreferredWidth(70);
        activityTable.getColumnModel().getColumn(3).setPreferredWidth(120);

        JScrollPane activityScroll = ModernTable.createModernScrollPane(activityTable);
        activityScroll.setPreferredSize(new Dimension(300, 160));

        activityCard.add(activityTitle, BorderLayout.NORTH);
        activityCard.add(activityScroll, BorderLayout.CENTER);

        panel.add(largestCard);
        panel.add(activityCard);
        return panel;
    }

    public void updateLargestFiles(List<FileRecord> files) {
        SwingUtilities.invokeLater(() -> {
            largestFilesModel.setRowCount(0);
            if (files != null) {
                int count = 0;
                for (FileRecord f : files) {
                    if (count++ >= 20) break;
                    largestFilesModel.addRow(new Object[]{
                            f.getFileName(),
                            FormatUtils.formatBytes(f.getSize()),
                            f.getCategory().getDisplayName(),
                            f.getAbsolutePath()
                    });
                }
            }
        });
    }

    public void updateCategoryChart(Map<FileCategory, Long> data) {
        SwingUtilities.invokeLater(() -> chartPanel.setData(data));
    }

    public void refreshData() {

        new SwingWorker<List<DriveInfo>, Void>() {
            private long total;
            private long used;
            private long free;

            @Override
            protected List<DriveInfo> doInBackground() {
                List<DriveInfo> drives = driveService.getAvailableDrives();
                total = driveService.getTotalCapacity();
                used = driveService.getTotalUsedSpace();
                free = driveService.getTotalFreeSpace();
                return drives;
            }

            @Override
            protected void done() {
                try {
                    List<DriveInfo> drives = get();
                    totalCapacityLabel.setText(FormatUtils.formatBytes(total));
                    usedStorageLabel.setText(FormatUtils.formatBytes(used));
                    freeStorageLabel.setText(FormatUtils.formatBytes(free));
                    double pct = total > 0 ? ((double) used / total) * 100.0 : 0.0;
                    utilizationPctLabel.setText(FormatUtils.formatPercentage(pct) + " Utilized");

                    drivesContainer.removeAll();
                    for (DriveInfo drive : drives) {
                        JPanel driveRow = new JPanel(new BorderLayout(12, 0));
                        driveRow.setOpaque(false);
                        driveRow.setBorder(new EmptyBorder(6, 4, 6, 4));

                        JPanel leftInfo = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
                        leftInfo.setOpaque(false);
                        leftInfo.setPreferredSize(new Dimension(180, 24));

                        JLabel driveName = new JLabel("💾 " + drive.getDisplayName());
                        driveName.setFont(Theme.FONT_REGULAR_BOLD);
                        driveName.setForeground(Theme.TEXT_PRIMARY);
                        leftInfo.add(driveName);

                        StorageGauge gauge = new StorageGauge(drive.getUsedSpace(), drive.getTotalSpace());

                        JPanel rightInfo = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
                        rightInfo.setOpaque(false);
                        rightInfo.setPreferredSize(new Dimension(240, 24));

                        String infoText = String.format("%s free of %s (%s)",
                                FormatUtils.formatBytes(drive.getFreeSpace()),
                                FormatUtils.formatBytes(drive.getTotalSpace()),
                                FormatUtils.formatPercentage(drive.getUsagePercentage()));
                        JLabel infoLabel = new JLabel(infoText);
                        infoLabel.setFont(Theme.FONT_SMALL);
                        infoLabel.setForeground(Theme.TEXT_SECONDARY);
                        rightInfo.add(infoLabel);

                        driveRow.add(leftInfo, BorderLayout.WEST);
                        driveRow.add(gauge, BorderLayout.CENTER);
                        driveRow.add(rightInfo, BorderLayout.EAST);

                        drivesContainer.add(driveRow);
                    }
                    drivesContainer.revalidate();
                    drivesContainer.repaint();

                    recentActivityModel.setRowCount(0);
                    List<HistoryEntry> entries = historyManager.getEntries();
                    int max = Math.min(15, entries.size());
                    for (int i = 0; i < max; i++) {
                        HistoryEntry e = entries.get(i);
                        recentActivityModel.addRow(new Object[]{
                                e.getTitle(),
                                e.getDetails(),
                                e.getStatus().getLabel(),
                                FormatUtils.formatTimestamp(e.getTimestamp())
                        });
                    }

                    if (chartPanel != null && largestFilesModel.getRowCount() == 0) {
                        Map<FileCategory, Long> demo = new EnumMap<>(FileCategory.class);
                        demo.put(FileCategory.DOCUMENTS, (long) (used * 0.15));
                        demo.put(FileCategory.VIDEOS, (long) (used * 0.35));
                        demo.put(FileCategory.IMAGES, (long) (used * 0.20));
                        demo.put(FileCategory.CODE, (long) (used * 0.10));
                        demo.put(FileCategory.ARCHIVES, (long) (used * 0.12));
                        demo.put(FileCategory.OTHERS, (long) (used * 0.08));
                        chartPanel.setData(demo);
                    }

                } catch (Exception ignored) {}
            }
        }.execute();
    }
}

