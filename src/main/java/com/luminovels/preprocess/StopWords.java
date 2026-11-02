package com.luminovels.preprocess;

import java.util.Set;

/**
 * English stop words, plus "can" and "source", which the project report found in nearly every synopsis.
 */
public final class StopWords {

    private static final Set<String> DEFAULT = Set.of(
            "a", "about", "above", "after", "again", "against", "all", "also", "am", "an", "and", "any",
            "are", "as", "at", "be", "because", "been", "before", "being", "below", "between", "both",
            "but", "by", "can", "could", "did", "do", "does", "doing", "down", "during", "each", "even",
            "few", "for", "from", "further", "get", "gets", "had", "has", "have", "having", "he", "her",
            "here", "hers", "herself", "him", "himself", "his", "how", "however", "if", "in", "into", "is",
            "it", "its", "itself", "just", "may", "me", "might", "more", "most", "much", "must", "my",
            "myself", "no", "nor", "not", "now", "of", "off", "on", "once", "one", "only", "or", "other",
            "our", "ours", "ourselves", "out", "over", "own", "same", "she", "should", "so", "some",
            "source", "still", "such", "than", "that", "the", "their", "theirs", "them", "themselves",
            "then", "there", "these", "they", "this", "those", "through", "to", "too", "under", "until",
            "up", "upon", "very", "was", "we", "were", "what", "when", "where", "which", "while", "who",
            "whom", "why", "will", "with", "within", "without", "would", "yet", "you", "your", "yours",
            "yourself", "yourselves");

    private final Set<String> words;

    public StopWords(Set<String> words) {
        this.words = Set.copyOf(words);
    }

    public static StopWords defaults() {
        return new StopWords(DEFAULT);
    }

    public boolean contains(String token) {
        return words.contains(token);
    }
}
