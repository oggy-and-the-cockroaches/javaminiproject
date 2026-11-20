package com.diskpilot.util;

import com.diskpilot.model.HistoryEntry;
import com.diskpilot.model.OrganizeAction;
import com.diskpilot.model.OrganizeBatch;
import com.diskpilot.model.CustomRule;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public final class JsonUtils {

    private JsonUtils() {}

    public static String escapeJson(String str) {
        if (str == null) return "";
        StringBuilder sb = new StringBuilder();
        for (char c : str.toCharArray()) {
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\b' -> sb.append("\\b");
                case '\f' -> sb.append("\\f");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 32) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        return sb.toString();
    }

    public static String historyToJson(List<HistoryEntry> entries) {
        StringBuilder sb = new StringBuilder();
        sb.append("[\n");
        for (int i = 0; i < entries.size(); i++) {
            HistoryEntry e = entries.get(i);
            sb.append("  {\n");
            sb.append("    \"id\": \"").append(escapeJson(e.getId())).append("\",\n");
            sb.append("    \"timestamp\": \"").append(escapeJson(e.getTimestamp().toString())).append("\",\n");
            sb.append("    \"type\": \"").append(escapeJson(e.getType().name())).append("\",\n");
            sb.append("    \"title\": \"").append(escapeJson(e.getTitle())).append("\",\n");
            sb.append("    \"details\": \"").append(escapeJson(e.getDetails())).append("\",\n");
            sb.append("    \"status\": \"").append(escapeJson(e.getStatus().name())).append("\",\n");
            sb.append("    \"targetPath\": \"").append(escapeJson(e.getTargetPath())).append("\",\n");
            sb.append("    \"affectedCount\": ").append(e.getAffectedCount()).append(",\n");
            sb.append("    \"affectedBytes\": ").append(e.getAffectedBytes()).append(",\n");
            sb.append("    \"batchId\": ").append(e.getBatchId() != null ? "\"" + escapeJson(e.getBatchId()) + "\"" : "null").append("\n");
            sb.append("  }");
            if (i < entries.size() - 1) sb.append(",");
            sb.append("\n");
        }
        sb.append("]");
        return sb.toString();
    }

    public static List<HistoryEntry> parseHistoryJson(String json) {
        List<HistoryEntry> list = new ArrayList<>();
        if (json == null || json.isBlank()) return list;

        int idx = 0;
        while ((idx = json.indexOf('{', idx)) != -1) {
            int endObj = findClosingBrace(json, idx);
            if (endObj == -1) break;
            String obj = json.substring(idx, endObj + 1);
            try {
                String id = extractString(obj, "id");
                String tsStr = extractString(obj, "timestamp");
                Instant timestamp = tsStr != null ? Instant.parse(tsStr) : Instant.now();
                String typeStr = extractString(obj, "type");
                HistoryEntry.OperationType type = HistoryEntry.OperationType.valueOf(typeStr);
                String title = extractString(obj, "title");
                String details = extractString(obj, "details");
                String statusStr = extractString(obj, "status");
                HistoryEntry.Status status = HistoryEntry.Status.valueOf(statusStr);
                String targetPath = extractString(obj, "targetPath");
                int count = extractInt(obj, "affectedCount", 0);
                long bytes = extractLong(obj, "affectedBytes", 0L);
                String batchId = extractString(obj, "batchId");

                list.add(new HistoryEntry(id, timestamp, type, title, details, status, targetPath, count, bytes, batchId));
            } catch (Exception ignored) {}
            idx = endObj + 1;
        }
        return list;
    }

    public static String batchesToJson(List<OrganizeBatch> batches) {
        StringBuilder sb = new StringBuilder();
        sb.append("[\n");
        for (int i = 0; i < batches.size(); i++) {
            OrganizeBatch b = batches.get(i);
            sb.append("  {\n");
            sb.append("    \"batchId\": \"").append(escapeJson(b.getBatchId())).append("\",\n");
            sb.append("    \"timestamp\": \"").append(escapeJson(b.getTimestamp().toString())).append("\",\n");
            sb.append("    \"sourceDirectory\": \"").append(escapeJson(b.getSourceDirectory() != null ? b.getSourceDirectory().toString() : "")).append("\",\n");
            sb.append("    \"mode\": \"").append(escapeJson(b.getMode())).append("\",\n");
            sb.append("    \"reverted\": ").append(b.isReverted()).append(",\n");
            sb.append("    \"actions\": [\n");
            List<OrganizeAction> actions = b.getActions();
            for (int j = 0; j < actions.size(); j++) {
                OrganizeAction a = actions.get(j);
                sb.append("      {\n");
                sb.append("        \"source\": \"").append(escapeJson(a.getSourcePath().toString())).append("\",\n");
                sb.append("        \"destination\": \"").append(escapeJson(a.getDestinationPath().toString())).append("\",\n");
                sb.append("        \"category\": \"").append(escapeJson(a.getCategory().name())).append("\",\n");
                sb.append("        \"size\": ").append(a.getFileSize()).append(",\n");
                sb.append("        \"status\": \"").append(escapeJson(a.getStatus().name())).append("\"\n");
                sb.append("      }");
                if (j < actions.size() - 1) sb.append(",");
                sb.append("\n");
            }
            sb.append("    ]\n");
            sb.append("  }");
            if (i < batches.size() - 1) sb.append(",");
            sb.append("\n");
        }
        sb.append("]");
        return sb.toString();
    }

    public static List<OrganizeBatch> parseBatchesJson(String json) {
        List<OrganizeBatch> list = new ArrayList<>();
        if (json == null || json.isBlank()) return list;

        int idx = 0;
        while ((idx = json.indexOf('{', idx)) != -1) {
            int endObj = findClosingBrace(json, idx);
            if (endObj == -1) break;
            String obj = json.substring(idx, endObj + 1);

            if (obj.contains("\"batchId\"")) {
                try {
                    String batchId = extractString(obj, "batchId");
                    String tsStr = extractString(obj, "timestamp");
                    Instant timestamp = tsStr != null ? Instant.parse(tsStr) : Instant.now();
                    String srcDir = extractString(obj, "sourceDirectory");
                    Path sourcePath = srcDir != null && !srcDir.isEmpty() ? Paths.get(srcDir) : Paths.get(".");
                    String mode = extractString(obj, "mode");
                    boolean reverted = extractBoolean(obj, "reverted", false);

                    OrganizeBatch batch = new OrganizeBatch(batchId, timestamp, sourcePath, mode, reverted);

                    int actIdx = obj.indexOf("\"actions\"");
                    if (actIdx != -1) {
                        int actStart = obj.indexOf('[', actIdx);
                        int actEnd = obj.indexOf(']', actStart);
                        if (actStart != -1 && actEnd != -1) {
                            String actArray = obj.substring(actStart, actEnd + 1);
                            int aIdx = 0;
                            while ((aIdx = actArray.indexOf('{', aIdx)) != -1) {
                                int aEnd = findClosingBrace(actArray, aIdx);
                                if (aEnd == -1) break;
                                String aObj = actArray.substring(aIdx, aEnd + 1);
                                String src = extractString(aObj, "source");
                                String dst = extractString(aObj, "destination");
                                long sz = extractLong(aObj, "size", 0L);
                                String st = extractString(aObj, "status");
                                if (src != null && dst != null) {
                                    OrganizeAction action = new OrganizeAction(Paths.get(src), Paths.get(dst), null, sz);
                                    if (st != null) {
                                        try { action.setStatus(OrganizeAction.Status.valueOf(st)); } catch (Exception ignored) {}
                                    }
                                    batch.addAction(action);
                                }
                                aIdx = aEnd + 1;
                            }
                        }
                    }
                    list.add(batch);
                } catch (Exception ignored) {}
            }
            idx = endObj + 1;
        }
        return list;
    }

    private static int findClosingBrace(String s, int openIndex) {
        int depth = 0;
        boolean inQuotes = false;
        for (int i = openIndex; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '"' && (i == 0 || s.charAt(i - 1) != '\\')) {
                inQuotes = !inQuotes;
            } else if (!inQuotes) {
                if (c == '{') depth++;
                else if (c == '}') {
                    depth--;
                    if (depth == 0) return i;
                }
            }
        }
        return -1;
    }

    private static String extractString(String obj, String key) {
        String pattern = "\"" + key + "\":";
        int kIdx = obj.indexOf(pattern);
        if (kIdx == -1) return null;
        int valStart = kIdx + pattern.length();
        while (valStart < obj.length() && Character.isWhitespace(obj.charAt(valStart))) valStart++;
        if (valStart < obj.length() && obj.charAt(valStart) == '"') {
            valStart++;
            StringBuilder sb = new StringBuilder();
            boolean escaped = false;
            for (int i = valStart; i < obj.length(); i++) {
                char c = obj.charAt(i);
                if (escaped) {
                    if (c == 'n') sb.append('\n');
                    else if (c == 'r') sb.append('\r');
                    else if (c == 't') sb.append('\t');
                    else sb.append(c);
                    escaped = false;
                } else if (c == '\\') {
                    escaped = true;
                } else if (c == '"') {
                    return sb.toString();
                } else {
                    sb.append(c);
                }
            }
        }
        return null;
    }

    private static int extractInt(String obj, String key, int def) {
        String pattern = "\"" + key + "\":";
        int kIdx = obj.indexOf(pattern);
        if (kIdx == -1) return def;
        int valStart = kIdx + pattern.length();
        while (valStart < obj.length() && Character.isWhitespace(obj.charAt(valStart))) valStart++;
        int valEnd = valStart;
        while (valEnd < obj.length() && (Character.isDigit(obj.charAt(valEnd)) || obj.charAt(valEnd) == '-')) valEnd++;
        try {
            return Integer.parseInt(obj.substring(valStart, valEnd));
        } catch (Exception e) {
            return def;
        }
    }

    private static long extractLong(String obj, String key, long def) {
        String pattern = "\"" + key + "\":";
        int kIdx = obj.indexOf(pattern);
        if (kIdx == -1) return def;
        int valStart = kIdx + pattern.length();
        while (valStart < obj.length() && Character.isWhitespace(obj.charAt(valStart))) valStart++;
        int valEnd = valStart;
        while (valEnd < obj.length() && (Character.isDigit(obj.charAt(valEnd)) || obj.charAt(valEnd) == '-')) valEnd++;
        try {
            return Long.parseLong(obj.substring(valStart, valEnd));
        } catch (Exception e) {
            return def;
        }
    }

    private static boolean extractBoolean(String obj, String key, boolean def) {
        String pattern = "\"" + key + "\":";
        int kIdx = obj.indexOf(pattern);
        if (kIdx == -1) return def;
        int valStart = kIdx + pattern.length();
        while (valStart < obj.length() && Character.isWhitespace(obj.charAt(valStart))) valStart++;
        if (obj.startsWith("true", valStart)) return true;
        if (obj.startsWith("false", valStart)) return false;
        return def;
    }
}

