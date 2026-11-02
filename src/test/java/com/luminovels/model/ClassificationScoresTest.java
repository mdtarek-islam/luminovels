package com.luminovels.model;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ClassificationScoresTest {

    @Test
    void perfectPredictionsScoreOne() {
        boolean[][] sets = {{true, false}, {false, true}};
        ClassificationScores s = ClassificationScores.fromSets(sets, sets);
        assertEquals(1.0, s.macroF1(), 1e-12);
        assertEquals(1.0, s.microF1(), 1e-12);
        assertEquals(1.0, s.exactMatch(), 1e-12);
    }

    @Test
    void handComputedF1() {
        boolean[][] actual = {{true, true}, {false, true}};
        boolean[][] predicted = {{true, false}, {true, true}};
        // genre 0: tp1 fp1 fn0 -> 2/3. genre 1: tp1 fp0 fn1 -> 2/3. pooled tp2 fp1 fn1 -> 4/6.
        ClassificationScores s = ClassificationScores.fromSets(actual, predicted);
        assertEquals(2.0 / 3.0, s.macroF1(), 1e-12);
        assertEquals(2.0 / 3.0, s.microF1(), 1e-12);
        assertEquals(0.0, s.exactMatch(), 1e-12);
    }

    @Test
    void sigmoidFallsBackToArgmaxWhenNothingPasses() {
        boolean[] set = ClassificationScores.predictedSet(new double[] {0.1, 0.4, 0.2}, OutputActivation.SIGMOID);
        assertArrayEquals(new boolean[] {false, true, false}, set);
        boolean[] two = ClassificationScores.predictedSet(new double[] {0.6, 0.5, 0.2}, OutputActivation.SIGMOID);
        assertArrayEquals(new boolean[] {true, true, false}, two);
    }

    @Test
    void nearestRankPercentiles() {
        long[] ms = new long[100];
        for (int i = 0; i < 100; i++) {
            ms[i] = (i + 1) * 1_000_000L;
        }
        LatencyStats stats = LatencyStats.fromNanos(ms);
        assertEquals(50.0, stats.p50Ms(), 1e-9);
        assertEquals(95.0, stats.p95Ms(), 1e-9);
        assertEquals(100, stats.samples());
    }
}
