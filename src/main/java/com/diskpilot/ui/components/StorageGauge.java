package com.diskpilot.ui.components;

import com.diskpilot.ui.Theme;
import com.diskpilot.util.FormatUtils;

import javax.swing.*;
import java.awt.*;

public class StorageGauge extends JPanel {

    private long totalBytes;
    private long usedBytes;
    private int barHeight = 14;

    public StorageGauge() {
        this(0, 0);
    }

    public StorageGauge(long usedBytes, long totalBytes) {
        this.usedBytes = Math.max(0, usedBytes);
        this.totalBytes = Math.max(0, totalBytes);
        setOpaque(false);
        setPreferredSize(new Dimension(200, 26));
    }

    public void setStorage(long usedBytes, long totalBytes) {
        this.usedBytes = Math.max(0, usedBytes);
        this.totalBytes = Math.max(0, totalBytes);
        repaint();
    }

    public double getPercentage() {
        if (totalBytes <= 0) return 0.0;
        return ((double) usedBytes / totalBytes) * 100.0;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        Theme.enableAntialiasing(g2);

        int w = getWidth();
        int h = getHeight();
        int y = (h - barHeight) / 2;

        g2.setColor(new Color(0x13, 0x1A, 0x26));
        g2.fillRoundRect(0, y, w, barHeight, barHeight, barHeight);

        g2.setColor(Theme.BORDER_SUBTLE);
        g2.setStroke(new BasicStroke(1.0f));
        g2.drawRoundRect(0, y, w - 1, barHeight - 1, barHeight, barHeight);

        double pct = getPercentage();
        int fillWidth = (int) Math.round((w * Math.min(100.0, pct)) / 100.0);

        if (fillWidth > 0) {
            Color startColor;
            Color endColor;
            if (pct >= 85.0) {
                startColor = new Color(0xEF, 0x44, 0x44);
                endColor = new Color(0xDC, 0x26, 0x26);
            } else if (pct >= 70.0) {
                startColor = new Color(0xF5, 0x9E, 0x0B);
                endColor = new Color(0xD9, 0x77, 0x06);
            } else {
                startColor = new Color(0x00, 0xE5, 0xFF);
                endColor = new Color(0x10, 0xB9, 0x81);
            }

            GradientPaint gradient = new GradientPaint(0, y, startColor, fillWidth, y, endColor);
            g2.setPaint(gradient);
            g2.fillRoundRect(0, y, fillWidth, barHeight, barHeight, barHeight);
        }

        g2.dispose();
    }
}

