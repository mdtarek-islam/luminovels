package com.luminovels.nlp;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Chi-square feature selection that also drops terms shared by too many genres.
 *
 * <p>For each term and genre the selector builds a 2 by 2 presence table and computes chi-square.
 * A term overlaps a genre when chi-square reaches {@code threshold} and the term is more common
 * inside the genre than in the corpus. Terms that overlap more than {@code maxOverlap} genres
 * carry little genre signal, so the selector drops them. It then keeps the {@code k} terms
 * with the highest single-genre chi-square.
 */
public final class ChiSquareSelector {

    /** Chi-square critical value for one degree of freedom at p = 0.05. */
    public static final double P05_THRESHOLD = 3.841;

    private final int k;
    private final int maxOverlap;
    private final double threshold;

    public ChiSquareSelector(int k, int maxOverlap, double threshold) {
        this.k = k;
        this.maxOverlap = maxOverlap;
        this.threshold = threshold;
    }

    /**
     * Selects terms.
     *
     * @param documents tokenised documents
     * @param labels binary genre vector per document, same order as documents
     * @param vocabulary candidate terms
     * @return selected terms, highest score first, ties broken alphabetically
     */
    public List<String> select(List<List<String>> documents, List<double[]> labels, List<String> vocabulary) {
        if (documents.size() != labels.size()) {
            throw new IllegalArgumentException("documents and labels differ in size");
        }
        int n = documents.size();
        int genres = labels.isEmpty() ? 0 : labels.get(0).length;
        Set<String> candidates = new HashSet<>(vocabulary);
        int[] genreTotals = new int[genres];
        Map<String, Integer> df = new HashMap<>();
        Map<String, int[]> joint = new HashMap<>();
        for (int d = 0; d < n; d++) {
            double[] y = labels.get(d);
            for (int g = 0; g < genres; g++) {
                if (y[g] > 0.0) {
                    genreTotals[g]++;
                }
            }
            for (String term : new HashSet<>(documents.get(d))) {
                if (!candidates.contains(term)) {
                    continue;
                }
                df.merge(term, 1, Integer::sum);
                int[] counts = joint.computeIfAbsent(term, t -> new int[genres]);
                for (int g = 0; g < genres; g++) {
                    if (y[g] > 0.0) {
                        counts[g]++;
                    }
                }
            }
        }
        Map<String, Double> score = new HashMap<>();
        for (Map.Entry<String, int[]> e : joint.entrySet()) {
            int termDf = df.get(e.getKey());
            double best = 0.0;
            int overlap = 0;
            for (int g = 0; g < genres; g++) {
                int a = e.getValue()[g];
                double chi = chiSquare(a, termDf - a, genreTotals[g] - a, n - termDf - genreTotals[g] + a);
                boolean positive = (double) a / termDf > (double) genreTotals[g] / n;
                if (positive && chi >= threshold) {
                    overlap++;
                }
                best = Math.max(best, chi);
            }
            if (overlap <= maxOverlap) {
                score.put(e.getKey(), best);
            }
        }
        List<String> ranked = new ArrayList<>(score.keySet());
        ranked.sort(Comparator.comparingDouble((String t) -> -score.get(t)).thenComparing(t -> t));
        return ranked.size() > k ? new ArrayList<>(ranked.subList(0, k)) : ranked;
    }

    /** Chi-square of a 2 by 2 table: a = term and genre, b = term only, c = genre only, d = neither. */
    static double chiSquare(long a, long b, long c, long d) {
        double n = a + b + c + d;
        double denom = (double) (a + c) * (b + d) * (a + b) * (c + d);
        if (denom == 0.0) {
            return 0.0;
        }
        double diff = (double) a * d - (double) b * c;
        return n * diff * diff / denom;
    }
}
