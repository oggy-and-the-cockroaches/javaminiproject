package com.diskpilot.util;

import java.text.DecimalFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public final class FormatUtils {
    private static final String[] UNITS = {"B", "KB", "MB", "GB", "TB", "PB"};
    private static final DecimalFormat SIZE_FORMAT = new DecimalFormat("#,##0.#");
    private static final DecimalFormat PERCENT_FORMAT = new DecimalFormat("0.0");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    private FormatUtils() {}

    public static String formatBytes(long bytes) {
        if (bytes <= 0) return "0 B";
        int unitIndex = 0;
        double value = bytes;
        while (value >= 1024.0 && unitIndex < UNITS.length - 1) {
            value /= 1024.0;
            unitIndex++;
        }
        return SIZE_FORMAT.format(value) + " " + UNITS[unitIndex];
    }

    public static String formatNumber(long number) {
        return String.format("%,d", number);
    }

    public static String formatPercentage(double percent) {
        return PERCENT_FORMAT.format(percent) + "%";
    }

    public static String formatTimestamp(Instant instant) {
        if (instant == null) return "N/A";
        return DATE_FORMATTER.format(instant);
    }

    public static String formatDuration(long ms) {
        if (ms < 1000) {
            return ms + " ms";
        }
        double sec = ms / 1000.0;
        return String.format("%.2f s", sec);
    }
}

