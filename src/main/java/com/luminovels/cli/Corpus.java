package com.luminovels.cli;

import com.luminovels.ingest.CsvStore;
import com.luminovels.ingest.LightNovel;
import com.luminovels.preprocess.Deduplicator;
import com.luminovels.preprocess.TextPipeline;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Loaded, de-duplicated entries with their tokens, in matching order.
 *
 * @param novels entries
 * @param tokens tokens per entry
 */
record Corpus(List<LightNovel> novels, List<List<String>> tokens) {

    static Corpus load(Path csv, TextPipeline pipeline) throws IOException {
        List<LightNovel> novels = Deduplicator.byId(CsvStore.read(csv));
        List<List<String>> tokens = new ArrayList<>();
        for (LightNovel n : novels) {
            tokens.add(pipeline.tokens(n));
        }
        return new Corpus(novels, tokens);
    }
}
