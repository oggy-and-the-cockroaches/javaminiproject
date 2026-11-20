package com.diskpilot.ui.components;

import com.diskpilot.model.FileCategory;
import com.diskpilot.ui.Theme;
import com.diskpilot.util.FormatUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.util.EnumMap;
import java.util.Map;

public class CategoryChartPanel extends JPanel {

    private final Map<FileCategory, Long> categoryData = new EnumMap<>(FileCategory.class);
    private long totalBytes = 0;

    public CategoryChartPanel() {
        setOpaque(false);
        setPreferredSize(new Dimension(360, 240));
        setLayout(new BorderLayout());
    }

    public void setData(Map<FileCategory, Long> data) {
        categoryData.clear();
        totalBytes = 0;
        if (data != null) {
            for (Map.Entry<FileCategory, Long> entry : data.entrySet()) {
                if (entry.getValue() > 0) {
                    categoryData.put(entry.getKey(), entry.getValue());
                    totalBytes += entry.getValue();
                }
            }
        }
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        Theme.enableAntialiasing(g2);

        int width = getWidth();
        int height = getHeight();

        if (totalBytes <= 0 || categoryData.isEmpty()) {
            g2.setColor(Theme.TEXT_MUTED);
            g2.setFont(Theme.FONT_REGULAR);
            String msg = "No storage scan data available";
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(msg, (width - fm.stringWidth(msg)) / 2, height / 2);
            g2.dispose();
            return;
        }

        int chartDiameter = Math.min(width / 2 - 20, height - 30);
        chartDiameter = Math.max(120, Math.min(180, chartDiameter));
        int chartX = 20;
        int chartY = (height - chartDiameter) / 2;

        double currentAngle = 90.0;
        for (Map.Entry<FileCategory, Long> entry : categoryData.entrySet()) {
            FileCategory cat = entry.getKey();
            long bytes = entry.getValue();
            double sliceAngle = (double) bytes / totalBytes * 360.0;

            g2.setColor(cat.getColor());
            g2.fill(new Arc2D.Double(chartX, chartY, chartDiameter, chartDiameter, currentAngle, sliceAngle, Arc2D.PIE));
            currentAngle += sliceAngle;
        }

        int innerDiameter = (int) (chartDiameter * 0.62);
        int innerX = chartX + (chartDiameter - innerDiameter) / 2;
        int innerY = chartY + (chartDiameter - innerDiameter) / 2;

        g2.setColor(Theme.BG_CARD);
        g2.fill(new Ellipse2D.Double(innerX, innerY, innerDiameter, innerDiameter));

        g2.setColor(Theme.TEXT_PRIMARY);
        g2.setFont(Theme.FONT_SUBHEADER);
        String centerTotal = FormatUtils.formatBytes(totalBytes);
        FontMetrics fm1 = g2.getFontMetrics();
        g2.drawString(centerTotal, innerX + (innerDiameter - fm1.stringWidth(centerTotal)) / 2, innerY + innerDiameter / 2);

        g2.setColor(Theme.TEXT_MUTED);
        g2.setFont(Theme.FONT_SMALL);
        String label = "Total Size";
        FontMetrics fm2 = g2.getFontMetrics();
        g2.drawString(label, innerX + (innerDiameter - fm2.stringWidth(label)) / 2, innerY + innerDiameter / 2 + 16);

        int legendX = chartX + chartDiameter + 30;
        int legendY = 24;
        int itemHeight = 22;

        g2.setFont(Theme.FONT_REGULAR);
        FontMetrics fmLegend = g2.getFontMetrics();

        for (Map.Entry<FileCategory, Long> entry : categoryData.entrySet()) {
            if (legendY + itemHeight > height) break;

            FileCategory cat = entry.getKey();
            long bytes = entry.getValue();
            double pct = ((double) bytes / totalBytes) * 100.0;

            g2.setColor(cat.getColor());
            g2.fillRoundRect(legendX, legendY + 3, 10, 10, 4, 4);

            g2.setColor(Theme.TEXT_PRIMARY);
            g2.drawString(cat.getDisplayName(), legendX + 18, legendY + 12);

            g2.setColor(Theme.TEXT_SECONDARY);
            String stat = String.format("%s (%.1f%%)", FormatUtils.formatBytes(bytes), pct);
            g2.drawString(stat, legendX + 115, legendY + 12);

            legendY += itemHeight;
        }

        g2.dispose();
    }
}

