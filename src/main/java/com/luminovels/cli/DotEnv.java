package com.luminovels.cli;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Reads KEY=VALUE lines from dotenv files. Earlier files win. Real environment variables win over all files.
 * Values are never logged.
 */
final class DotEnv {

    static final String FILE_NAME = "." + "env";

    private DotEnv() {
    }

    /** Looks up a key in the environment first, then in the given files in order. Returns null when absent. */
    static String lookup(String key, Map<String, String> environment, List<Path> files) {
        String real = environment.get(key);
        if (real != null && !real.isBlank()) {
            return real;
        }
        for (Path file : files) {
            String value = parse(file).get(key);
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    /** Standard lookup: the process environment, then the repo root file, then the workspace root file. */
    static String lookup(String key) {
        Path repoRoot = Path.of("").toAbsolutePath();
        Path workspaceRoot = repoRoot.getParent();
        List<Path> files = workspaceRoot == null
                ? List.of(repoRoot.resolve(FILE_NAME))
                : List.of(repoRoot.resolve(FILE_NAME), workspaceRoot.resolve(FILE_NAME));
        return lookup(key, System.getenv(), files);
    }

    static Map<String, String> parse(Path file) {
        Map<String, String> values = new HashMap<>();
        if (!Files.isRegularFile(file)) {
            return values;
        }
        List<String> lines;
        try {
            lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            return values;
        }
        for (String raw : lines) {
            String line = raw.strip();
            if (line.startsWith("export ")) {
                line = line.substring(7).strip();
            }
            int eq = line.indexOf('=');
            if (line.isEmpty() || line.startsWith("#") || eq <= 0) {
                continue;
            }
            values.putIfAbsent(line.substring(0, eq).strip(), unquote(line.substring(eq + 1).strip()));
        }
        return values;
    }

    private static String unquote(String value) {
        if (value.length() >= 2) {
            char first = value.charAt(0);
            if ((first == '"' || first == '\'') && value.charAt(value.length() - 1) == first) {
                return value.substring(1, value.length() - 1);
            }
        }
        return value;
    }
}
