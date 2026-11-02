package com.luminovels.ingest;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Writes and reads the LightNovels.csv cache. Every field is quoted. List fields use "|".
 */
public final class CsvStore {

    static final String HEADER = "id,title,mean,genres,authors,alternative_titles,synopsis";
    private static final String LIST_SEPARATOR = "|";
    private static final int COLUMNS = 7;

    private CsvStore() {
    }

    public static void write(Path path, Collection<LightNovel> novels) throws IOException {
        Path parent = path.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        try (BufferedWriter out = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            out.write(HEADER);
            out.write('\n');
            for (LightNovel n : novels) {
                List<String> fields = List.of(
                        Long.toString(n.id()), n.title(), Double.toString(n.mean()),
                        join(n.genres()), join(n.authors()), join(n.alternativeTitles()), n.synopsis());
                StringBuilder line = new StringBuilder();
                for (int i = 0; i < fields.size(); i++) {
                    if (i > 0) {
                        line.append(',');
                    }
                    line.append('"').append(fields.get(i).replace("\"", "\"\"")).append('"');
                }
                out.write(line.toString());
                out.write('\n');
            }
        }
    }

    public static List<LightNovel> read(Path path) throws IOException {
        List<List<String>> rows = parse(Files.readString(path, StandardCharsets.UTF_8));
        List<LightNovel> novels = new ArrayList<>();
        for (int r = 1; r < rows.size(); r++) {
            List<String> f = rows.get(r);
            if (f.size() != COLUMNS) {
                throw new IOException("Row " + r + " has " + f.size() + " columns, expected " + COLUMNS);
            }
            novels.add(new LightNovel(Long.parseLong(f.get(0)), f.get(1), Double.parseDouble(f.get(2)),
                    split(f.get(3)), split(f.get(4)), split(f.get(5)), f.get(6)));
        }
        return novels;
    }

    /** Parses RFC 4180 style CSV, including quoted fields that hold commas, quotes and newlines. */
    static List<List<String>> parse(String text) {
        List<List<String>> rows = new ArrayList<>();
        List<String> row = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean quoted = false;
        int i = 0;
        while (i < text.length()) {
            char c = text.charAt(i);
            if (quoted) {
                if (c == '"' && i + 1 < text.length() && text.charAt(i + 1) == '"') {
                    field.append('"');
                    i++;
                } else if (c == '"') {
                    quoted = false;
                } else {
                    field.append(c);
                }
            } else if (c == '"') {
                quoted = true;
            } else if (c == ',') {
                row.add(field.toString());
                field.setLength(0);
            } else if (c == '\n') {
                row.add(field.toString());
                field.setLength(0);
                rows.add(row);
                row = new ArrayList<>();
            } else if (c != '\r') {
                field.append(c);
            }
            i++;
        }
        if (field.length() > 0 || !row.isEmpty()) {
            row.add(field.toString());
            rows.add(row);
        }
        return rows;
    }

    private static String join(List<String> values) {
        List<String> cleaned = new ArrayList<>();
        for (String v : values) {
            cleaned.add(v.replace(LIST_SEPARATOR, "/"));
        }
        return String.join(LIST_SEPARATOR, cleaned);
    }

    private static List<String> split(String value) {
        if (value.isEmpty()) {
            return List.of();
        }
        return Arrays.asList(value.split("\\|"));
    }
}
