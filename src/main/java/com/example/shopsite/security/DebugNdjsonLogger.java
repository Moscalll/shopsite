package com.example.shopsite.security;

import java.io.FileWriter;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Debug-only NDJSON logger for Cursor debug mode.
 * Writes one JSON object per line into workspace file: debug-fcad12.log
 */
final class DebugNdjsonLogger {
    private static final String LOG_PATH = "debug-fcad12.log";
    private static final String SESSION_ID = "fcad12";

    private DebugNdjsonLogger() {}

    static void log(String runId, String hypothesisId, String location, String message, Map<String, Object> data) {
        try (FileWriter fw = new FileWriter(LOG_PATH, true)) {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("sessionId", SESSION_ID);
            payload.put("runId", runId);
            payload.put("hypothesisId", hypothesisId);
            payload.put("location", location);
            payload.put("message", message);
            payload.put("data", data == null ? new LinkedHashMap<>() : data);
            payload.put("timestamp", Instant.now().toEpochMilli());
            fw.write(toJson(payload));
            fw.write("\n");
        } catch (Exception ignored) {
            // Never break runtime due to debug logging.
        }
    }

    private static String toJson(Object v) {
        if (v == null) return "null";
        if (v instanceof String) return "\"" + escape((String) v) + "\"";
        if (v instanceof Number || v instanceof Boolean) return String.valueOf(v);
        if (v instanceof Map<?, ?> m) {
            StringBuilder sb = new StringBuilder();
            sb.append("{");
            boolean first = true;
            for (Map.Entry<?, ?> e : m.entrySet()) {
                if (!first) sb.append(",");
                first = false;
                sb.append(toJson(String.valueOf(e.getKey()))).append(":").append(toJson(e.getValue()));
            }
            sb.append("}");
            return sb.toString();
        }
        return "\"" + escape(String.valueOf(v)) + "\"";
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n")
                .replace("\t", "\\t");
    }
}

