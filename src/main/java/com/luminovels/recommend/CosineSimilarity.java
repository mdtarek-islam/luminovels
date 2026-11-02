package com.luminovels.recommend;

import com.luminovels.nlp.SparseVector;

/**
 * Cosine similarity, A . B / (|A| |B|). Returns 0 when either vector is zero.
 */
public final class CosineSimilarity {

    private CosineSimilarity() {
    }

    public static double of(SparseVector a, SparseVector b) {
        double denom = a.norm() * b.norm();
        return denom == 0.0 ? 0.0 : a.dot(b) / denom;
    }

    public static double of(double[] a, double[] b) {
        if (a.length != b.length) {
            throw new IllegalArgumentException("length mismatch");
        }
        double dot = 0.0;
        double na = 0.0;
        double nb = 0.0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            na += a[i] * a[i];
            nb += b[i] * b[i];
        }
        double denom = Math.sqrt(na) * Math.sqrt(nb);
        return denom == 0.0 ? 0.0 : dot / denom;
    }
}
