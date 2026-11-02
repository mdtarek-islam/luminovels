package com.luminovels.ingest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CsvStoreTest {

    @TempDir
    Path dir;

    @Test
    void roundTripKeepsQuotesCommasNewlinesAndLists() throws IOException {
        LightNovel a = new LightNovel(1L, "Re:Zero, \"Starting\" Life", 8.1,
                List.of("Fantasy", "Isekai"), List.of("Tappei Nagatsuki"), List.of("Re:Zero"),
                "Line one.\nLine two, with a comma.");
        LightNovel b = new LightNovel(2L, "Empty", 0.0, List.of(), List.of(), List.of(), "");
        Path file = dir.resolve("data").resolve("LightNovels.csv");
        CsvStore.write(file, List.of(a, b));
        assertEquals(List.of(a, b), CsvStore.read(file));
    }
}
