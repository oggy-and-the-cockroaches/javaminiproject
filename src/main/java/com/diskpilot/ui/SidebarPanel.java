package com.diskpilot.ui;

import com.diskpilot.ui.components.ModernCard;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

public class SidebarPanel extends JPanel {

    public interface NavigationListener {
        void onNavigate(String moduleKey);
    }

    public static final String DASHBOARD = "dashboard";
    public static final String SORTIFY = "sortify";
    public static final String DISKLENS = "disklens";
    public static final String DUPLICATES = "duplicates";
    public static final String HISTORY = "history";

    private final NavigationListener listener;
    private final List<NavButton> buttons = new ArrayList<>();
    private String activeModule = DASHBOARD;

    public SidebarPanel(NavigationListener listener) {
        this.listener = listener;
        setPreferredSize(new Dimension(240, 800));
        setBackground(Theme.BG_SIDEBAR);
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, Theme.BORDER_CARD));

        add(createHeaderPanel(), BorderLayout.NORTH);
        add(createNavItemsPanel(), BorderLayout.CENTER);
        add(createFooterPanel(), BorderLayout.SOUTH);
    }

    private JPanel createHeaderPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(24, 20, 20, 20));

        JLabel brandLabel = new JLabel("DISKPILOT");
        brandLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        brandLabel.setForeground(Theme.ACCENT_CYAN);

        JLabel subLabel = new JLabel("Storage Management Suite");
        subLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        subLabel.setForeground(Theme.TEXT_MUTED);

        JPanel badge = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        badge.setOpaque(false);
        badge.setBorder(new EmptyBorder(8, 0, 0, 0));

        JLabel dot = new JLabel("●");
        dot.setForeground(Theme.SUCCESS);
        dot.setFont(new Font("Segoe UI", Font.BOLD, 10));

        JLabel badgeText = new JLabel("CORE PROTECTED");
        badgeText.setFont(new Font("Consolas", Font.BOLD, 10));
        badgeText.setForeground(Theme.TEXT_SECONDARY);

        badge.add(dot);
        badge.add(badgeText);

        panel.add(brandLabel);
        panel.add(Box.createVerticalStrut(2));
        panel.add(subLabel);
        panel.add(badge);

        return panel;
    }

    private JPanel createNavItemsPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(10, 12, 10, 12));

        addNavItem(panel, DASHBOARD, "Dashboard", "📊");
        addNavItem(panel, DISKLENS, "DiskLens Analyzer", "🔍");
        addNavItem(panel, SORTIFY, "Sortify Organizer", "📁");
        addNavItem(panel, DUPLICATES, "Duplicate Hunter", "👥");
        addNavItem(panel, HISTORY, "Activity & Undo", "📜");

        panel.add(Box.createVerticalGlue());
        return panel;
    }

    private void addNavItem(JPanel container, String key, String title, String icon) {
        NavButton btn = new NavButton(key, title, icon);
        buttons.add(btn);
        container.add(btn);
        container.add(Box.createVerticalStrut(6));
    }

    private JPanel createFooterPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(16, 20, 20, 20));

        JLabel version = new JLabel("v1.0.0 • Java 17+ NIO");
        version.setFont(Theme.FONT_SMALL);
        version.setForeground(Theme.TEXT_MUTED);

        JLabel status = new JLabel("Windows 10/11 Native");
        status.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        status.setForeground(new Color(0x47, 0x55, 0x69));

        panel.add(version, BorderLayout.NORTH);
        panel.add(status, BorderLayout.SOUTH);
        return panel;
    }

    public void setActive(String key) {
        this.activeModule = key;
        for (NavButton btn : buttons) {
            btn.updateState();
        }
    }

    private class NavButton extends JPanel {
        private final String key;
        private final String title;
        private final String icon;
        private boolean hovered = false;

        public NavButton(String key, String title, String icon) {
            this.key = key;
            this.title = title;
            this.icon = icon;

            setLayout(new BorderLayout());
            setPreferredSize(new Dimension(216, 42));
            setMaximumSize(new Dimension(216, 42));
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            JLabel label = new JLabel(icon + "  " + title);
            label.setFont(Theme.FONT_SUBHEADER);
            label.setForeground(Theme.TEXT_PRIMARY);
            label.setBorder(new EmptyBorder(0, 14, 0, 10));
            add(label, BorderLayout.CENTER);

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    hovered = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hovered = false;
                    repaint();
                }

                @Override
                public void mousePressed(MouseEvent e) {
                    setActive(key);
                    if (listener != null) {
                        listener.onNavigate(key);
                    }
                }
            });
        }

        public void updateState() {
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            Theme.enableAntialiasing(g2);

            boolean isActive = key.equals(activeModule);
            int w = getWidth();
            int h = getHeight();

            if (isActive) {

                g2.setColor(new Color(0x18, 0x24, 0x38));
                g2.fillRoundRect(0, 0, w, h, 8, 8);

                g2.setColor(Theme.ACCENT_CYAN);
                g2.fillRoundRect(0, 4, 4, h - 8, 4, 4);

                g2.setColor(new Color(0x27, 0x41, 0x66));
                g2.drawRoundRect(0, 0, w - 1, h - 1, 8, 8);
            } else if (hovered) {
                g2.setColor(Theme.BG_CARD_HOVER);
                g2.fillRoundRect(0, 0, w, h, 8, 8);
            }

            g2.dispose();
            super.paintComponent(g);
        }
    }
}

