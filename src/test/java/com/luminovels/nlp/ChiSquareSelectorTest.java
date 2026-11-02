package com.luminovels.nlp;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChiSquareSelectorTest {

    private final List<List<String>> docs = new ArrayList<>();
    private final List<double[]> labels = new ArrayList<>();
    private final List<String> vocabulary = List.of("love", "magic", "sword", "the");

    ChiSquareSelectorTest() {
        // Four documents per genre. "magic" sits in genres 0 and 1, so it overlaps two genres.
        for (int i = 0; i < 4; i++) {
            docs.add(List.of("sword", "magic", "the"));
            labels.add(new double[] {1, 0, 0});
            docs.add(List.of("love", "magic", "the"));
            labels.add(new double[] {0, 1, 0});
            docs.add(List.of("the"));
            labels.add(new double[] {0, 0, 1});
        }
    }

    @Test
    void chiSquareMatchesHandWorkedTable() {
        assertEquals(12.0, ChiSquareSelector.chiSquare(4, 0, 0, 8), 1e-12);
        assertEquals(3.0, ChiSquareSelector.chiSquare(4, 4, 0, 4), 1e-12);
        assertEquals(0.0, ChiSquareSelector.chiSquare(4, 8, 0, 0), 1e-12);
    }

    @Test
    void dropsTermsThatOverlapTooManyGenres() {
        List<String> kept = new ChiSquareSelector(10, 1, 2.0).select(docs, labels, vocabulary);
        assertFalse(kept.contains("magic"));
        assertTrue(kept.contains("sword"));
        assertTrue(kept.contains("love"));
    }

    @Test
    void keepsTopKByScoreThenAlphabetically() {
        List<String> kept = new ChiSquareSelector(2, 1, 2.0).select(docs, labels, vocabulary);
        assertEquals(List.of("love", "sword"), kept);
    }

    @Test
    void looserOverlapLimitKeepsSharedTerm() {
        List<String> kept = new ChiSquareSelector(10, 2, 2.0).select(docs, labels, vocabulary);
        assertTrue(kept.contains("magic"));
    }
}
