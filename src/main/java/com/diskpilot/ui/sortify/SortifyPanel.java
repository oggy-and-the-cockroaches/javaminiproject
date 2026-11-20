package com.diskpilot.ui.sortify;

import com.diskpilot.model.CustomRule;
import com.diskpilot.model.FileRecord;
import com.diskpilot.model.OrganizeAction;
import com.diskpilot.model.OrganizeBatch;
import com.diskpilot.model.ScanResult;
import com.diskpilot.service.FileOrganizer;
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
import java.util.List;

public class SortifyPanel extends JPanel {

    private final FileOrganizer fileOrganizer;
    private final FileScanner fileScanner;
    private final HistoryManager historyManager;

    private JTextField sourceFolderField;
    private JComboBox<FileOrganizer.OrganizeMode> modeCombo;
    private JCheckBox recursiveCheck;

    private ModernButton previewBtn;
    private ModernButton executeBtn;
    private ModernButton undoBtn;
    private ModernButton rulesBtn;
    private ModernButton cancelBtn;

    private JLabel summaryLabel;
    private JProgressBar progressBar;

    private DefaultTableModel previewTableModel;
    private ModernTable previewTable;

    private List<OrganizeAction> currentActions = new ArrayList<>();
    private final List<CustomRule> customRules = new ArrayList<>();
    private OrganizeBatch lastBatch;
    private Path currentBaseFolder;

    public SortifyPanel() {
        this.fileOrganizer = new FileOrganizer();
        this.fileScanner = new FileScanner();
        this.historyManager = HistoryManager.getInstance();

        setupDefaultRules();

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
        mainContent.add(createPreviewTableCard());

        add(mainContent, BorderLayout.CENTER);
    }

    private void setupDefaultRules() {
        customRules.add(new CustomRule("Python Development", "Python Projects", "py, ipynb, whl", "", 0, true));
        customRules.add(new CustomRule("Large Disc Images", "ISO Images", "iso, img, vmdk", "", 100L * 1024 * 1024, true));
        customRules.add(new CustomRule("Financial & Invoices", "Invoices & Reports", "pdf, xlsx, csv", "invoice", 0, true));
    }

    private JPanel createHeaderPanel() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setOpaque(false);

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        left.setOpaque(false);

        JLabel title = new JLabel("Sortify: Intelligent File Organizer");
        title.setFont(Theme.FONT_TITLE);
        title.setForeground(Theme.TEXT_PRIMARY);

        JLabel sub = new JLabel("  •  Automated Categorization, Rules & Safe Undo");
        sub.setFont(Theme.FONT_REGULAR);
        sub.setForeground(Theme.TEXT_MUTED);

        left.add(title);
        left.add(sub);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        right.setOpaque(false);

        undoBtn = new ModernButton("↶ Undo Last Batch", ModernButton.ButtonStyle.SECONDARY);
        undoBtn.addActionListener(e -> undoLastOperation());
        right.add(undoBtn);

        bar.add(left, BorderLayout.WEST);
        bar.add(right, BorderLayout.EAST);
        return bar;
    }

    private JPanel createControlsCard() {
        ModernCard card = new ModernCard(new BorderLayout(12, 10));

        JPanel pathRow = new JPanel(new BorderLayout(10, 0));
        pathRow.setOpaque(false);

        JLabel pathLabel = new JLabel("Target Folder to Organize:");
        pathLabel.setFont(Theme.FONT_SUBHEADER);
        pathLabel.setForeground(Theme.TEXT_PRIMARY);

        sourceFolderField = new JTextField(Paths.get(System.getProperty("user.home"), "Downloads").toString());
        sourceFolderField.setFont(Theme.FONT_REGULAR);
        sourceFolderField.setBackground(Theme.BG_INPUT);
        sourceFolderField.setForeground(Theme.TEXT_PRIMARY);
        sourceFolderField.setCaretColor(Theme.ACCENT_CYAN);
        sourceFolderField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER_CARD, 1),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)
        ));

        ModernButton browseBtn = new ModernButton("📁 Browse...", ModernButton.ButtonStyle.SECONDARY);
        browseBtn.addActionListener(e -> chooseSourceFolder());

        pathRow.add(pathLabel, BorderLayout.WEST);
        pathRow.add(sourceFolderField, BorderLayout.CENTER);
        pathRow.add(browseBtn, BorderLayout.EAST);

        JPanel optionsRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        optionsRow.setOpaque(false);

        JLabel modeLabel = new JLabel("Organization Strategy:");
        modeLabel.setFont(Theme.FONT_REGULAR);
        modeLabel.setForeground(Theme.TEXT_SECONDARY);

        modeCombo = new JComboBox<>(FileOrganizer.OrganizeMode.values());
        modeCombo.setFont(Theme.FONT_REGULAR);
        modeCombo.setBackground(Theme.BG_CARD);
        modeCombo.setForeground(Theme.TEXT_PRIMARY);
        modeCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof FileOrganizer.OrganizeMode m) {
                    setText(m.getLabel());
                }
                return this;
            }
        });

        recursiveCheck = new JCheckBox("Include Subdirectories", false);
        recursiveCheck.setFont(Theme.FONT_REGULAR);
        recursiveCheck.setForeground(Theme.TEXT_PRIMARY);
        recursiveCheck.setOpaque(false);

        previewBtn = new ModernButton("👁 Generate Preview", ModernButton.ButtonStyle.PRIMARY);
        previewBtn.addActionListener(e -> generatePreview());

        rulesBtn = new ModernButton("⚙ Custom Rules", ModernButton.ButtonStyle.SECONDARY);
        rulesBtn.addActionListener(e -> openRulesDialog());

        optionsRow.add(modeLabel);
        optionsRow.add(modeCombo);
        optionsRow.add(recursiveCheck);
        optionsRow.add(Box.createHorizontalStrut(10));
        optionsRow.add(previewBtn);
        optionsRow.add(rulesBtn);

        card.add(pathRow, BorderLayout.NORTH);
        card.add(optionsRow, BorderLayout.SOUTH);
        return card;
    }

    private JPanel createProgressCard() {
        ModernCard card = new ModernCard(new BorderLayout(0, 6));
        card.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));

        summaryLabel = new JLabel("Select a folder and click 'Generate Preview' to inspect proposed file movements before applying.");
        summaryLabel.setFont(Theme.FONT_REGULAR);
        summaryLabel.setForeground(Theme.TEXT_SECONDARY);

        progressBar = new JProgressBar();
        progressBar.setOpaque(false);
        progressBar.setForeground(Theme.ACCENT_CYAN);
        progressBar.setBackground(Theme.BG_INPUT);
        progressBar.setBorder(BorderFactory.createLineBorder(Theme.BORDER_SUBTLE, 1));
        progressBar.setPreferredSize(new Dimension(100, 10));

        card.add(summaryLabel, BorderLayout.NORTH);
        card.add(progressBar, BorderLayout.CENTER);
        return card;
    }

    private JPanel createPreviewTableCard() {
        ModernCard card = new ModernCard(new BorderLayout(0, 12));

        JPanel toolbar = new JPanel(new BorderLayout());
        toolbar.setOpaque(false);

        JLabel previewTitle = new JLabel("Proposed File Relocations (Review Before Execution)");
        previewTitle.setFont(Theme.FONT_HEADER);
        previewTitle.setForeground(Theme.TEXT_PRIMARY);

        JPanel actionButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actionButtons.setOpaque(false);

        ModernButton selectAllBtn = new ModernButton("Select All", ModernButton.ButtonStyle.SECONDARY);
        selectAllBtn.addActionListener(e -> setAllSelected(true));

        ModernButton deselectAllBtn = new ModernButton("Deselect All", ModernButton.ButtonStyle.SECONDARY);
        deselectAllBtn.addActionListener(e -> setAllSelected(false));

        executeBtn = new ModernButton("✔ Confirm & Execute Organization", ModernButton.ButtonStyle.SUCCESS);
        executeBtn.setEnabled(false);
        executeBtn.addActionListener(e -> executeConfirmedMoves());

        cancelBtn = new ModernButton("✖ Cancel", ModernButton.ButtonStyle.DANGER);
        cancelBtn.setEnabled(false);
        cancelBtn.addActionListener(e -> fileOrganizer.cancel());

        actionButtons.add(selectAllBtn);
        actionButtons.add(deselectAllBtn);
        actionButtons.add(executeBtn);
        actionButtons.add(cancelBtn);

        toolbar.add(previewTitle, BorderLayout.WEST);
        toolbar.add(actionButtons, BorderLayout.EAST);

        String[] cols = {"Select", "Source File", "Category", "Size", "Destination Folder", "Collision Status"};
        previewTableModel = new DefaultTableModel(cols, 0) {
            @Override
            public Class<?> getColumnClass(int col) {
                return col == 0 ? Boolean.class : String.class;
            }

            @Override
            public boolean isCellEditable(int row, int col) {
                return col == 0;
            }
        };

        previewTable = new ModernTable(previewTableModel);
        previewTable.getColumnModel().getColumn(0).setPreferredWidth(50);
        previewTable.getColumnModel().getColumn(1).setPreferredWidth(160);
        previewTable.getColumnModel().getColumn(2).setPreferredWidth(90);
        previewTable.getColumnModel().getColumn(3).setPreferredWidth(80);
        previewTable.getColumnModel().getColumn(4).setPreferredWidth(200);
        previewTable.getColumnModel().getColumn(5).setPreferredWidth(160);

        previewTableModel.addTableModelListener(e -> {
            if (e.getColumn() == 0 && e.getFirstRow() >= 0 && e.getFirstRow() < currentActions.size()) {
                Boolean checked = (Boolean) previewTableModel.getValueAt(e.getFirstRow(), 0);
                currentActions.get(e.getFirstRow()).setSelected(checked != null && checked);
                updateSummaryText();
            }
        });

        JScrollPane tableScroll = ModernTable.createModernScrollPane(previewTable);
        tableScroll.setPreferredSize(new Dimension(800, 340));

        card.add(toolbar, BorderLayout.NORTH);
        card.add(tableScroll, BorderLayout.CENTER);
        return card;
    }

    private void chooseSourceFolder() {
        JFileChooser chooser = new JFileChooser(sourceFolderField.getText().trim());
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setDialogTitle("Select Folder to Organize");
        int res = chooser.showOpenDialog(this);
        if (res == JFileChooser.APPROVE_OPTION) {
            File selected = chooser.getSelectedFile();
            if (selected != null) {
                sourceFolderField.setText(selected.getAbsolutePath());
            }
        }
    }

    private void generatePreview() {
        String pathStr = sourceFolderField.getText().trim();
        if (pathStr.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter or select a folder to organize.", "Invalid Folder", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Path target = Paths.get(pathStr);
        if (!Files.exists(target) || !Files.isDirectory(target)) {
            JOptionPane.showMessageDialog(this, "The specified directory does not exist: " + pathStr, "Directory Missing", JOptionPane.ERROR_MESSAGE);
            return;
        }

        SafetyValidator.ValidationResult safety = SafetyValidator.validatePathForOrganization(target);
        if (!safety.isValid()) {
            JOptionPane.showMessageDialog(this, safety.getMessage(), "Safety Lock Enforced", JOptionPane.ERROR_MESSAGE);
            historyManager.recordError("Safety Lock Enforced", safety.getMessage(), target.toString());
            return;
        }

        currentBaseFolder = target;
        FileOrganizer.OrganizeMode mode = (FileOrganizer.OrganizeMode) modeCombo.getSelectedItem();
        boolean recursive = recursiveCheck.isSelected();

        previewBtn.setEnabled(false);
        progressBar.setIndeterminate(true);
        summaryLabel.setText("Scanning candidate files in " + target.getFileName() + "...");

        new SwingWorker<List<OrganizeAction>, Void>() {
            @Override
            protected List<OrganizeAction> doInBackground() {
                ScanResult scan = fileScanner.scan(target, recursive, null);
                return fileOrganizer.preview(target, scan.getAllFiles(), mode, customRules);
            }

            @Override
            protected void done() {
                previewBtn.setEnabled(true);
                progressBar.setIndeterminate(false);

                try {
                    currentActions = get();
                    previewTableModel.setRowCount(0);

                    for (OrganizeAction a : currentActions) {
                        String destFolder = a.getDestinationPath().getParent() != null ?
                                a.getDestinationPath().getParent().getFileName().toString() : "";

                        previewTableModel.addRow(new Object[]{
                                a.isSelected(),
                                a.getSourcePath().getFileName().toString(),
                                a.getCategory().getDisplayName(),
                                FormatUtils.formatBytes(a.getFileSize()),
                                destFolder + " / " + a.getDestinationPath().getFileName(),
                                a.getMessage()
                        });
                    }

                    executeBtn.setEnabled(!currentActions.isEmpty());
                    updateSummaryText();

                } catch (Exception e) {
                    summaryLabel.setText("Error generating preview: " + e.getMessage());
                }
            }
        }.execute();
    }

    private void updateSummaryText() {
        int selected = 0;
        long bytes = 0;
        for (OrganizeAction a : currentActions) {
            if (a.isSelected()) {
                selected++;
                bytes += a.getFileSize();
            }
        }
        summaryLabel.setText(String.format("Preview Ready: %d of %d files selected for organization (%s total). No files have been moved yet.",
                selected, currentActions.size(), FormatUtils.formatBytes(bytes)));
    }

    private void setAllSelected(boolean selected) {
        for (int i = 0; i < previewTableModel.getRowCount(); i++) {
            previewTableModel.setValueAt(selected, i, 0);
        }
        for (OrganizeAction a : currentActions) {
            a.setSelected(selected);
        }
        updateSummaryText();
    }

    private void executeConfirmedMoves() {
        int selectedCount = 0;
        long totalBytes = 0;
        for (OrganizeAction a : currentActions) {
            if (a.isSelected()) {
                selectedCount++;
                totalBytes += a.getFileSize();
            }
        }

        if (selectedCount == 0) {
            JOptionPane.showMessageDialog(this, "No files are selected for organization.", "Nothing to Move", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        String confirmMsg = String.format("Are you sure you want to organize %d files (%s)?\n\n" +
                "Target folder: %s\n" +
                "Strategy: %s\n" +
                "Safety: Existing files will NEVER be overwritten (name collisions resolve safely).\n" +
                "An undo record will be saved to allow instant reversal.",
                selectedCount, FormatUtils.formatBytes(totalBytes),
                currentBaseFolder.toString(),
                modeCombo.getSelectedItem());

        int choice = JOptionPane.showConfirmDialog(this, confirmMsg, "Confirm File Organization", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (choice != JOptionPane.YES_OPTION) {
            return;
        }

        executeBtn.setEnabled(false);
        previewBtn.setEnabled(false);
        cancelBtn.setEnabled(true);
        progressBar.setIndeterminate(false);
        progressBar.setValue(0);
        progressBar.setMaximum(selectedCount);

        FileOrganizer.OrganizeMode mode = (FileOrganizer.OrganizeMode) modeCombo.getSelectedItem();

        new SwingWorker<OrganizeBatch, Integer>() {
            @Override
            protected OrganizeBatch doInBackground() {
                return fileOrganizer.execute(currentBaseFolder, currentActions, mode, (current, total, action) -> {
                    publish(current);
                });
            }

            @Override
            protected void process(List<Integer> chunks) {
                if (!chunks.isEmpty()) {
                    int val = chunks.get(chunks.size() - 1);
                    progressBar.setValue(val);
                    summaryLabel.setText(String.format("Organizing files: %d / %d completed...", val, currentActions.size()));
                }
            }

            @Override
            protected void done() {
                executeBtn.setEnabled(true);
                previewBtn.setEnabled(true);
                cancelBtn.setEnabled(false);

                try {
                    lastBatch = get();
                    historyManager.recordOrganize(lastBatch);

                    int success = lastBatch.getSuccessCount();
                    int failed = lastBatch.getFailedCount();

                    summaryLabel.setText(String.format("Organization complete! %d files moved successfully (%s). %d failed.",
                            success, FormatUtils.formatBytes(lastBatch.getTotalMovedBytes()), failed));

                    JOptionPane.showMessageDialog(SortifyPanel.this,
                            String.format("Sortify complete!\n\nSuccessfully organized: %d files\nTotal space relocated: %s\nErrors/Skipped: %d\n\nYou can revert this operation at any time using the Undo button.",
                                    success, FormatUtils.formatBytes(lastBatch.getTotalMovedBytes()), failed),
                            "Organization Completed", JOptionPane.INFORMATION_MESSAGE);

                    previewTableModel.setRowCount(0);
                    for (OrganizeAction a : lastBatch.getActions()) {
                        previewTableModel.addRow(new Object[]{
                                false,
                                a.getSourcePath().getFileName().toString(),
                                a.getCategory().getDisplayName(),
                                FormatUtils.formatBytes(a.getFileSize()),
                                a.getDestinationPath().toString(),
                                a.getMessage()
                        });
                    }

                } catch (Exception e) {
                    summaryLabel.setText("Execution failed: " + e.getMessage());
                }
            }
        }.execute();
    }

    private void undoLastOperation() {
        if (lastBatch == null) {

            List<OrganizeBatch> batches = historyManager.getBatches();
            if (!batches.isEmpty()) {
                lastBatch = batches.get(batches.size() - 1);
            }
        }

        if (lastBatch == null || lastBatch.isReverted()) {
            JOptionPane.showMessageDialog(this, "No active organization batch available to undo.", "Nothing to Undo", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        int choice = JOptionPane.showConfirmDialog(this,
                "Do you want to revert the last organization operation?\nAll moved files will be returned to their original folders.",
                "Confirm Undo Operation", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (choice != JOptionPane.YES_OPTION) return;

        progressBar.setIndeterminate(true);
        summaryLabel.setText("Reverting file organization...");

        new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() {
                return fileOrganizer.undo(lastBatch, null);
            }

            @Override
            protected void done() {
                progressBar.setIndeterminate(false);
                try {
                    boolean success = get();
                    historyManager.recordUndo(lastBatch, success);
                    summaryLabel.setText(success ? "Undo complete: all files restored to original locations." : "Undo completed with warnings.");

                    JOptionPane.showMessageDialog(SortifyPanel.this,
                            success ? "All files have been safely returned to their original paths." : "Undo completed, but some files could not be relocated.",
                            "Undo Completed", JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception e) {
                    summaryLabel.setText("Undo error: " + e.getMessage());
                }
            }
        }.execute();
    }

    private void openRulesDialog() {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Custom Organization Rules", true);
        dialog.setSize(520, 380);
        dialog.setLocationRelativeTo(this);
        dialog.getContentPane().setBackground(Theme.BG_APP);

        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(Theme.BG_APP);
        panel.setBorder(new EmptyBorder(16, 16, 16, 16));

        String[] cols = {"Active", "Rule Name", "Target Subfolder", "Extensions"};
        DefaultTableModel ruleModel = new DefaultTableModel(cols, 0) {
            @Override
            public Class<?> getColumnClass(int col) { return col == 0 ? Boolean.class : String.class; }
            @Override
            public boolean isCellEditable(int row, int col) { return col == 0; }
        };

        for (CustomRule r : customRules) {
            ruleModel.addRow(new Object[]{r.isEnabled(), r.getName(), r.getTargetFolderName(), r.getExtensionsCsv()});
        }

        ModernTable ruleTable = new ModernTable(ruleModel);
        ruleModel.addTableModelListener(e -> {
            if (e.getColumn() == 0 && e.getFirstRow() >= 0 && e.getFirstRow() < customRules.size()) {
                Boolean checked = (Boolean) ruleModel.getValueAt(e.getFirstRow(), 0);
                customRules.get(e.getFirstRow()).setEnabled(checked != null && checked);
            }
        });

        JScrollPane scroll = ModernTable.createModernScrollPane(ruleTable);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        bottom.setOpaque(false);

        ModernButton addRuleBtn = new ModernButton("+ Add Rule", ModernButton.ButtonStyle.SECONDARY);
        addRuleBtn.addActionListener(e -> {
            String name = JOptionPane.showInputDialog(dialog, "Enter rule name (e.g. Photoshop Files):");
            if (name == null || name.isBlank()) return;
            String exts = JOptionPane.showInputDialog(dialog, "Enter file extensions (comma separated, e.g. psd, psb):");
            String target = JOptionPane.showInputDialog(dialog, "Enter target folder name (e.g. Design Assets):");
            if (target == null || target.isBlank()) target = name;

            CustomRule newRule = new CustomRule(name, target, exts, "", 0, true);
            customRules.add(newRule);
            ruleModel.addRow(new Object[]{true, newRule.getName(), newRule.getTargetFolderName(), newRule.getExtensionsCsv()});
        });

        ModernButton closeBtn = new ModernButton("Close", ModernButton.ButtonStyle.PRIMARY);
        closeBtn.addActionListener(e -> dialog.dispose());

        bottom.add(addRuleBtn);
        bottom.add(closeBtn);

        panel.add(scroll, BorderLayout.CENTER);
        panel.add(bottom, BorderLayout.SOUTH);

        dialog.setContentPane(panel);
        dialog.setVisible(true);
    }
}

