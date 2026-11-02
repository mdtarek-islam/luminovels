package com.luminovels.preprocess;

import com.luminovels.ingest.LightNovel;

import java.util.List;

/**
 * Turns one entry into tokens from its synopsis, title, alternative titles and authors.
 */
public final class TextPipeline {

    private final Normaliser normaliser;
    private final Tokeniser tokeniser;

    public TextPipeline(Normaliser normaliser, Tokeniser tokeniser) {
        this.normaliser = normaliser;
        this.tokeniser = tokeniser;
    }

    public static TextPipeline defaults() {
        return new TextPipeline(new Normaliser(), new Tokeniser(StopWords.defaults()));
    }

    public List<String> tokens(LightNovel novel) {
        String text = String.join(" ",
                novel.synopsis(),
                novel.title(),
                String.join(" ", novel.alternativeTitles()),
                String.join(" ", novel.authors()));
        return tokens(text);
    }

    public List<String> tokens(String text) {
        return tokeniser.tokenise(normaliser.normalise(text));
    }
}
