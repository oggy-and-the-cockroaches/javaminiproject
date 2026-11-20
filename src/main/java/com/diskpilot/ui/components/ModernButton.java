package com.diskpilot.ui.components;

import com.diskpilot.ui.Theme;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class ModernButton extends JButton {

    public enum ButtonStyle {
        PRIMARY,
        SECONDARY,
        SUCCESS,
        DANGER,
        ACCENT
    }

    private final ButtonStyle style;
    private boolean isHovered = false;
    private boolean isPressed = false;
    private int cornerRadius = 10;

    public ModernButton(String text) {
        this(text, ButtonStyle.PRIMARY);
    }

    public ModernButton(String text, ButtonStyle style) {
        super(text);
        this.style = style;
        setContentAreaFilled(false);
        setFocusPainted(false);
        setBorderPainted(false);
        setOpaque(false);
        setFont(Theme.FONT_SUBHEADER);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setBorder(BorderFactory.createEmptyBorder(10, 18, 10, 18));

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                isHovered = true;
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                isHovered = false;
                repaint();
            }

            @Override
            public void mousePressed(MouseEvent e) {
                isPressed = true;
                repaint();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                isPressed = false;
                repaint();
            }
        });
    }

    public void setCornerRadius(int radius) {
        this.cornerRadius = radius;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        Theme.enableAntialiasing(g2);

        int w = getWidth();
        int h = getHeight();

        Color baseBg;
        Color hoverBg;
        Color fg;
        Color border;

        switch (style) {
            case PRIMARY -> {
                baseBg = Theme.ACCENT_CYAN;
                hoverBg = new Color(0x38, 0xBD, 0xF8);
                fg = new Color(0x0A, 0x0E, 0x17);
                border = Theme.ACCENT_CYAN;
            }
            case SECONDARY -> {
                baseBg = isHovered ? Theme.BG_CARD_HOVER : Theme.BG_CARD;
                hoverBg = Theme.BG_CARD_HOVER;
                fg = Theme.TEXT_PRIMARY;
                border = isHovered ? Theme.ACCENT_CYAN : Theme.BORDER_CARD;
            }
            case SUCCESS -> {
                baseBg = Theme.SUCCESS;
                hoverBg = new Color(0x05, 0x96, 0x69);
                fg = Color.WHITE;
                border = Theme.SUCCESS;
            }
            case DANGER -> {
                baseBg = Theme.DANGER;
                hoverBg = new Color(0xDC, 0x26, 0x26);
                fg = Color.WHITE;
                border = Theme.DANGER;
            }
            case ACCENT -> {
                baseBg = Theme.ACCENT_PURPLE;
                hoverBg = new Color(0x7C, 0x3A, 0xED);
                fg = Color.WHITE;
                border = Theme.ACCENT_PURPLE;
            }
            default -> {
                baseBg = Theme.BG_CARD;
                hoverBg = Theme.BG_CARD_HOVER;
                fg = Theme.TEXT_PRIMARY;
                border = Theme.BORDER_CARD;
            }
        }

        if (!isEnabled()) {
            baseBg = new Color(0x1B, 0x22, 0x2E);
            fg = Theme.TEXT_MUTED;
            border = Theme.BORDER_SUBTLE;
        } else if (isPressed) {
            baseBg = baseBg.darker();
        } else if (isHovered && style != ButtonStyle.SECONDARY) {
            baseBg = hoverBg;
        }

        g2.setColor(baseBg);
        g2.fillRoundRect(0, 0, w - 1, h - 1, cornerRadius, cornerRadius);

        g2.setColor(border);
        g2.setStroke(new BasicStroke(1.2f));
        g2.drawRoundRect(0, 0, w - 1, h - 1, cornerRadius, cornerRadius);

        setForeground(fg);

        g2.dispose();
        super.paintComponent(g);
    }
}

