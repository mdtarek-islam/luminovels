package com.luminovels.cli;

import java.util.HashMap;
import java.util.Map;

/**
 * Parses "--key value" pairs that follow the subcommand.
 */
final class Args {

    private final Map<String, String> values = new HashMap<>();

    Args(String[] argv, int start) {
        for (int i = start; i < argv.length; i++) {
            String key = argv[i];
            if (!key.startsWith("--") || i + 1 >= argv.length) {
                throw new IllegalArgumentException("expected --key value, got " + key);
            }
            values.put(key.substring(2), argv[++i]);
        }
    }

    String get(String key, String fallback) {
        return values.getOrDefault(key, fallback);
    }

    String get(String key) {
        return values.get(key);
    }

    int getInt(String key, int fallback) {
        String v = values.get(key);
        return v == null ? fallback : Integer.parseInt(v);
    }

    long getLong(String key, long fallback) {
        String v = values.get(key);
        return v == null ? fallback : Long.parseLong(v);
    }

    double getDouble(String key, double fallback) {
        String v = values.get(key);
        return v == null ? fallback : Double.parseDouble(v);
    }
}
