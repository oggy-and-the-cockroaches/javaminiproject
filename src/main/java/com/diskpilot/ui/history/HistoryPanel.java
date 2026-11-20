package com.diskpilot.ui.history;

import com.diskpilot.model.HistoryEntry;
import com.diskpilot.model.OrganizeBatch;
import com.diskpilot.service.FileOrganizer;
import com.diskpilot.service.HistoryManager;
import com.diskpilot.ui.Theme;
import com.diskpilot.ui.components.ModernButton;
import com.diskpilot.ui.components.ModernCard;
import com.diskpilot.ui.components.ModernTable;
import com.diskpilot.util.FormatUtils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class HistoryPanel extends JPanel {

    private final HistoryManager historyManager;
    private final FileOrganizer fileOrganizer;

    private JComboBox<String> typeFilterCombo;
    private JTextField searchField;
    private DefaultTableModel historyTableModel;
    private ModernTable historyTable;
    private ModernButton undoBtn;
    private JLabel summaryLabel;

    private List<HistoryEntry> currentFilteredEntries = new ArrayList<>();

    public HistoryPanel() {
        this.historyManager = HistoryManager.getInstance();
        this.fileOrganizer = new FileOrganizer();

        setLayout(new BorderLayout());
        setBackground(Theme.BG_APP);
        setBorder(new EmptyBorder(20, 24, 20, 24));

        JPanel mainContent = new JPanel();
        mainContent.setLayout(new BoxLayout(mainContent, BoxLayout.Y_AXIS));
        mainContent.setOpaque(false);

        mainContent.add(createHeaderPanel());
        mainContent.add(Box.createVerticalStrut(14));
        mainContent.add(createFilterToolbar());
        mainContent.add(Box.createVerticalStrut(14));
        mainContent.add(createTableCard());

        add(mainContent, BorderLayout.CENTER);

        refreshHistory();
    }

    private JPanel createHeaderPanel() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setOpaque(false);

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        left.setOpaque(false);

        JLabel title = new JLabel("Activity History & Audit Trail");
        title.setFont(Theme.FONT_TITLE);
        title.setForeground(Theme.TEXT_PRIMARY);

        JLabel sub = new JLabel("  •  Operation Logs, Session Records & Rollback Center");
        sub.setFont(Theme.FONT_REGULAR);
        sub.setForeground(Theme.TEXT_MUTED);

        left.add(title);
        left.add(sub);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        right.setOpaque(false);

        ModernButton clearBtn = new ModernButton("🗑 Clear Log", ModernButton.ButtonStyle.SECONDARY);
        clearBtn.addActionListener(e -> clearHistoryLog());

        ModernButton exportBtn = new ModernButton("📄 Export JSON", ModernButton.ButtonStyle.SECONDARY);
        exportBtn.addActionListener(e -> exportLog());

        right.add(clearBtn);
        right.add(exportBtn);

        bar.add(left, BorderLayout.WEST);
        bar.add(right, BorderLayout.EAST);
        return bar;
    }

    private JPanel createFilterToolbar() {
        ModernCard card = new ModernCard(new BorderLayout(10, 0));
        card.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        left.setOpaque(false);

        JLabel filterLabel = new JLabel("Filter Operation:");
        filterLabel.setFont(Theme.FONT_REGULAR);
        filterLabel.setForeground(Theme.TEXT_SECONDARY);

        typeFilterCombo = new JComboBox<>(new String[]{
                "All Operations",
                "Storage Scans",
                "File Organizations",
                "Duplicate Sessions",
                "Undo Operations",
                "Warnings & Errors"
        });
        typeFilterCombo.setFont(Theme.FONT_REGULAR);
        typeFilterCombo.setBackground(Theme.BG_CARD);
        typeFilterCombo.setForeground(Theme.TEXT_PRIMARY);
        typeFilterCombo.addActionListener(e -> applyFilter());

        JLabel searchLabel = new JLabel("Search:");
        searchLabel.setFont(Theme.FONT_REGULAR);
        searchLabel.setForeground(Theme.TEXT_SECONDARY);

        searchField = new JTextField(14);
        searchField.setFont(Theme.FONT_REGULAR);
        searchField.setBackground(Theme.BG_INPUT);
        searchField.setForeground(Theme.TEXT_PRIMARY);
        searchField.setCaretColor(Theme.ACCENT_CYAN);
        searchField.addActionListener(e -> applyFilter());

        ModernButton filterBtn = new ModernButton("Apply", ModernButton.ButtonStyle.SECONDARY);
        filterBtn.addActionListener(e -> applyFilter());

        left.add(filterLabel);
        left.add(typeFilterCombo);
        left.add(searchLabel);
        left.add(searchField);
        left.add(filterBtn);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        right.setOpaque(false);

        undoBtn = new ModernButton("↶ Undo Selected Operation", ModernButton.ButtonStyle.PRIMARY);
        undoBtn.setEnabled(false);
        undoBtn.addActionListener(e -> undoSelectedOperation());
        right.add(undoBtn);

        card.add(left, BorderLayout.WEST);
        card.add(right, BorderLayout.EAST);
        return card;
    }

    private JPanel createTableCard() {
        ModernCard card = new ModernCard(new BorderLayout(0, 10));

        summaryLabel = new JLabel("Total Recorded Events: 0");
        summaryLabel.setFont(Theme.FONT_SUBHEADER);
        summaryLabel.setForeground(Theme.TEXT_SECONDARY);

        String[] cols = {"Timestamp", "Type", "Title / Summary", "Status", "Affected Files", "Storage Impact", "Path"};
        historyTableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };

        historyTable = new ModernTable(historyTableModel);
        historyTable.getColumnModel().getColumn(0).setPreferredWidth(130);
        historyTable.getColumnModel().getColumn(1).setPreferredWidth(100);
        historyTable.getColumnModel().getColumn(2).setPreferredWidth(220);
        historyTable.getColumnModel().getColumn(3).setPreferredWidth(80);
        historyTable.getColumnModel().getColumn(4).setPreferredWidth(85);
        historyTable.getColumnModel().getColumn(5).setPreferredWidth(95);
        historyTable.getColumnModel().getColumn(6).setPreferredWidth(200);

        historyTable.getSelectionModel().addListSelectionListener(e -> {
            int selectedRow = historyTable.getSelectedRow();
            if (selectedRow >= 0 && selectedRow < currentFilteredEntries.size()) {
                HistoryEntry entry = currentFilteredEntries.get(selectedRow);
                boolean canUndo = entry.getType() == HistoryEntry.OperationType.ORGANIZE
                        && entry.getBatchId() != null
                        && entry.getStatus() != HistoryEntry.Status.UNDONE;
                undoBtn.setEnabled(canUndo);
            } else {
                undoBtn.setEnabled(false);
            }
        });

        JScrollPane scroll = ModernTable.createModernScrollPane(historyTable);
        scroll.setPreferredSize(new Dimension(800, 420));

        card.add(summaryLabel, BorderLayout.NORTH);
        card.add(scroll, BorderLayout.CENTER);
        return card;
    }

    public void refreshHistory() {
        applyFilter();
    }

    private void applyFilter() {
        String filterChoice = (String) typeFilterCombo.getSelectedItem();
        String search = searchField.getText().trim().toLowerCase();

        List<HistoryEntry> all = historyManager.getEntries();
        currentFilteredEntries.clear();
        historyTableModel.setRowCount(0);

        for (HistoryEntry e : all) {
            if (filterChoice != null) {
                if (filterChoice.equals("Storage Scans") && e.getType() != HistoryEntry.OperationType.SCAN) continue;
                if (filterChoice.equals("File Organizations") && e.getType() != HistoryEntry.OperationType.ORGANIZE) continue;
                if (filterChoice.equals("Duplicate Sessions") && e.getType() != HistoryEntry.OperationType.DUPLICATE_DETECTION) continue;
                if (filterChoice.equals("Undo Operations") && e.getType() != HistoryEntry.OperationType.UNDO) continue;
                if (filterChoice.equals("Warnings & Errors") &&
                        e.getType() != HistoryEntry.OperationType.WARNING && e.getType() != HistoryEntry.OperationType.ERROR) continue;
            }

            if (!search.isEmpty()) {
                String title = e.getTitle().toLowerCase();
                String details = e.getDetails().toLowerCase();
                String path = e.getTargetPath().toLowerCase();
                if (!title.contains(search) && !details.contains(search) && !path.contains(search)) {
                    continue;
                }
            }

            currentFilteredEntries.add(e);
            historyTableModel.addRow(new Object[]{
                    FormatUtils.formatTimestamp(e.getTimestamp()),
                    e.getType().getLabel(),
                    e.getTitle() + " - " + e.getDetails(),
                    e.getStatus().getLabel(),
                    e.getAffectedCount() > 0 ? FormatUtils.formatNumber(e.getAffectedCount()) : "-",
                    e.getAffectedBytes() > 0 ? FormatUtils.formatBytes(e.getAffectedBytes()) : "-",
                    e.getTargetPath()
            });
        }

        summaryLabel.setText("Filtered Events: " + currentFilteredEntries.size() + " of " + all.size() + " total logged operations");
    }

    private void undoSelectedOperation() {
        int row = historyTable.getSelectedRow();
        if (row < 0 || row >= currentFilteredEntries.size()) return;

        HistoryEntry entry = currentFilteredEntries.get(row);
        if (entry.getBatchId() == null) {
            JOptionPane.showMessageDialog(this, "This operation does not contain a reversible file batch manifest.", "Undo Not Supported", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        OrganizeBatch batch = historyManager.getBatchById(entry.getBatchId());
        if (batch == null || batch.isReverted()) {
            JOptionPane.showMessageDialog(this, "Batch is either already reverted or no longer available in cache.", "Cannot Undo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int choice = JOptionPane.showConfirmDialog(this,
                "Do you want to revert this organization batch (" + batch.getBatchId() + ")?\nAll moved files will be returned to their previous locations.",
                "Confirm Batch Undo", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (choice != JOptionPane.YES_OPTION) return;

        new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() {
                return fileOrganizer.undo(batch, null);
            }

            @Override
            protected void done() {
                try {
                    boolean success = get();
                    entry.setStatus(HistoryEntry.Status.UNDONE);
                    historyManager.recordUndo(batch, success);
                    refreshHistory();

                    JOptionPane.showMessageDialog(HistoryPanel.this,
                            success ? "Batch organization successfully reverted!" : "Batch organization partially reverted.",
                            "Undo Completed", JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(HistoryPanel.this, "Undo failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void clearHistoryLog() {
        int choice = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to clear the entire operation history log?",
                "Clear History", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (choice == JOptionPane.YES_OPTION) {
            historyManager.clearHistory();
            refreshHistory();
        }
    }

    private void exportLog() {
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File("diskpilot_audit_log.json"));
        chooser.setDialogTitle("Export Audit Log");
        int res = chooser.showSaveDialog(this);
        if (res == JFileChooser.APPROVE_OPTION) {
            File target = chooser.getSelectedFile();
            try {
                String json = com.diskpilot.util.JsonUtils.historyToJson(historyManager.getEntries());
                Files.writeString(target.toPath(), json);
                JOptionPane.showMessageDialog(this, "History log successfully exported to:\n" + target.getAbsolutePath(), "Export Successful", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Failed to export history: " + e.getMessage(), "Export Failed", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}

