package com.luminovels.preprocess;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Splits text into lower-case word tokens. Drops stop words, single characters and pure numbers.
 */
public final class Tokeniser {

    private static final Pattern SPLIT = Pattern.compile("[^\\p{L}\\p{N}]+");
    private static final Pattern NUMBER = Pattern.compile("\\p{N}+");

    private final StopWords stopWords;

    public Tokeniser(StopWords stopWords) {
        this.stopWords = stopWords;
    }

    public List<String> tokenise(String text) {
        List<String> tokens = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return tokens;
        }
        for (String raw : SPLIT.split(text.toLowerCase(Locale.ROOT))) {
            if (raw.length() < 2 || NUMBER.matcher(raw).matches() || stopWords.contains(raw)) {
                continue;
            }
            tokens.add(raw);
        }
        return tokens;
    }
}
