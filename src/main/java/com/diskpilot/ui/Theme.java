package com.diskpilot.ui;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public final class Theme {

    private Theme() {}

    public static final Color BG_APP = new Color(0x0B, 0x0E, 0x14);
    public static final Color BG_SIDEBAR = new Color(0x10, 0x14, 0x1E);
    public static final Color BG_HEADER = new Color(0x10, 0x14, 0x1E);
    public static final Color BG_CARD = new Color(0x16, 0x1C, 0x28);
    public static final Color BG_CARD_ALT = new Color(0x1B, 0x23, 0x33);
    public static final Color BG_CARD_HOVER = new Color(0x21, 0x2A, 0x3D);
    public static final Color BG_INPUT = new Color(0x0F, 0x14, 0x1E);

    public static final Color BORDER_SUBTLE = new Color(0x23, 0x2C, 0x3E);
    public static final Color BORDER_CARD = new Color(0x2A, 0x35, 0x4B);
    public static final Color BORDER_FOCUS = new Color(0x00, 0xD2, 0xFF);

    public static final Color ACCENT_CYAN = new Color(0x00, 0xE5, 0xFF);
    public static final Color ACCENT_BLUE = new Color(0x38, 0xBD, 0xF8);
    public static final Color ACCENT_PURPLE = new Color(0x8B, 0x5C, 0xF6);
    public static final Color SUCCESS = new Color(0x10, 0xB9, 0x81);
    public static final Color WARNING = new Color(0xF5, 0x9E, 0x0B);
    public static final Color DANGER = new Color(0xEF, 0x44, 0x44);

    public static final Color TEXT_PRIMARY = new Color(0xF1, 0xF5, 0xF9);
    public static final Color TEXT_SECONDARY = new Color(0x94, 0xA3, 0xB8);
    public static final Color TEXT_MUTED = new Color(0x64, 0x74, 0x8B);

    public static final Font FONT_TITLE = new Font("Segoe UI", Font.BOLD, 22);
    public static final Font FONT_HEADER = new Font("Segoe UI", Font.BOLD, 16);
    public static final Font FONT_SUBHEADER = new Font("Segoe UI", Font.BOLD, 13);
    public static final Font FONT_REGULAR = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font FONT_REGULAR_BOLD = new Font("Segoe UI", Font.BOLD, 13);
    public static final Font FONT_SMALL = new Font("Segoe UI", Font.PLAIN, 11);
    public static final Font FONT_MONO = new Font("Consolas", Font.PLAIN, 12);
    public static final Font FONT_METRIC = new Font("Segoe UI", Font.BOLD, 24);

    public static void setupGlobalAntialiasing() {
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}
    }

    public static void enableAntialiasing(Graphics g) {
        if (g instanceof Graphics2D g2) {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        }
    }

    public static Border createEmptyPadding(int top, int left, int bottom, int right) {
        return new EmptyBorder(top, left, bottom, right);
    }
}

