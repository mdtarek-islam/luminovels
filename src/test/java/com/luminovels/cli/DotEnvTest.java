package com.luminovels.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DotEnvTest {

    @TempDir
    Path dir;

    private Path write(String name, String text) throws Exception {
        Path p = dir.resolve(name);
        Files.writeString(p, text);
        return p;
    }

    @Test
    void parsesQuotesCommentsAndExport() throws Exception {
        Path f = write("a", "# note\nA=1\nexport B=\"two\"\nC='three'\n\nbad line\n");
        Map<String, String> m = DotEnv.parse(f);
        assertEquals("1", m.get("A"));
        assertEquals("two", m.get("B"));
        assertEquals("three", m.get("C"));
        assertEquals(3, m.size());
    }

    @Test
    void realEnvironmentWinsOverFiles() throws Exception {
        Path f = write("a", "KEY=file\n");
        assertEquals("real", DotEnv.lookup("KEY", Map.of("KEY", "real"), List.of(f)));
    }

    @Test
    void firstFileWinsAndMissingFileIsSkipped() throws Exception {
        Path first = write("a", "KEY=first\n");
        Path second = write("b", "KEY=second\nOTHER=x\n");
        Path missing = dir.resolve("none");
        assertEquals("first", DotEnv.lookup("KEY", Map.of(), List.of(missing, first, second)));
        assertEquals("x", DotEnv.lookup("OTHER", Map.of(), List.of(first, second)));
        assertNull(DotEnv.lookup("ABSENT", Map.of(), List.of(first, second)));
    }
}
