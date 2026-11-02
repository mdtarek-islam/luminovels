package com.luminovels.preprocess;

import java.text.Normalizer;
import java.util.regex.Pattern;

/**
 * Cleans raw text before tokenising: Unicode NFKC, HTML tags, source credits and whitespace.
 */
public final class Normaliser {

    private static final Pattern HTML_TAG = Pattern.compile("<[^>]*>");
    private static final Pattern CREDIT = Pattern.compile(
            "\\[Written by MAL Rewrite\\]|\\(Source:[^)]*\\)", Pattern.CASE_INSENSITIVE);
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    public String normalise(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        String s = Normalizer.normalize(text, Normalizer.Form.NFKC);
        s = HTML_TAG.matcher(s).replaceAll(" ");
        s = CREDIT.matcher(s).replaceAll(" ");
        return WHITESPACE.matcher(s).replaceAll(" ").trim();
    }
}
