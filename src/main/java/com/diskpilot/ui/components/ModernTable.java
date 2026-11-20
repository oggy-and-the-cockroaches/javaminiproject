package com.diskpilot.ui.components;

import com.diskpilot.ui.Theme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicScrollBarUI;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableModel;
import java.awt.*;

public class ModernTable extends JTable {

    public ModernTable(TableModel model) {
        super(model);
        setRowHeight(32);
        setShowGrid(false);
        setIntercellSpacing(new Dimension(0, 0));
        setOpaque(false);
        setBackground(Theme.BG_CARD);
        setForeground(Theme.TEXT_PRIMARY);
        setSelectionBackground(new Color(0x1B, 0x3A, 0x57));
        setSelectionForeground(Theme.TEXT_PRIMARY);
        setFont(Theme.FONT_REGULAR);

        JTableHeader header = getTableHeader();
        header.setOpaque(false);
        header.setBackground(Theme.BG_HEADER);
        header.setForeground(Theme.TEXT_SECONDARY);
        header.setFont(Theme.FONT_SUBHEADER);
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER_CARD));
        header.setPreferredSize(new Dimension(header.getWidth(), 36));

        DefaultTableCellRenderer headerRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
                setBackground(Theme.BG_HEADER);
                setForeground(Theme.TEXT_SECONDARY);
                setFont(Theme.FONT_SUBHEADER);
                setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
                return this;
            }
        };
        header.setDefaultRenderer(headerRenderer);

        setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
                setBorder(new EmptyBorder(0, 10, 0, 10));
                if (isSelected) {
                    setBackground(new Color(0x1E, 0x3A, 0x5C));
                    setForeground(Theme.TEXT_PRIMARY);
                } else {
                    setBackground(row % 2 == 0 ? Theme.BG_CARD : Theme.BG_CARD_ALT);
                    setForeground(Theme.TEXT_PRIMARY);
                }
                return this;
            }
        });
    }

    public static JScrollPane createModernScrollPane(JTable table) {
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.getViewport().setBackground(Theme.BG_CARD);
        scrollPane.setBorder(BorderFactory.createLineBorder(Theme.BORDER_CARD, 1));

        styleScrollBar(scrollPane.getVerticalScrollBar());
        styleScrollBar(scrollPane.getHorizontalScrollBar());

        return scrollPane;
    }

    public static void styleScrollBar(JScrollBar bar) {
        bar.setUI(new BasicScrollBarUI() {
            @Override
            protected void configureScrollBarColors() {
                this.thumbColor = new Color(0x33, 0x41, 0x55);
                this.thumbDarkShadowColor = new Color(0x1E, 0x29, 0x3B);
                this.thumbHighlightColor = new Color(0x47, 0x55, 0x69);
                this.trackColor = Theme.BG_APP;
            }

            @Override
            protected JButton createDecreaseButton(int orientation) {
                return createZeroButton();
            }

            @Override
            protected JButton createIncreaseButton(int orientation) {
                return createZeroButton();
            }

            private JButton createZeroButton() {
                JButton b = new JButton();
                b.setPreferredSize(new Dimension(0, 0));
                b.setMinimumSize(new Dimension(0, 0));
                b.setMaximumSize(new Dimension(0, 0));
                return b;
            }

            @Override
            protected void paintThumb(Graphics g, JComponent c, Rectangle thumbBounds) {
                Graphics2D g2 = (Graphics2D) g.create();
                Theme.enableAntialiasing(g2);
                g2.setColor(thumbColor);
                g2.fillRoundRect(thumbBounds.x + 2, thumbBounds.y + 2, thumbBounds.width - 4, thumbBounds.height - 4, 6, 6);
                g2.dispose();
            }
        });
    }
}

