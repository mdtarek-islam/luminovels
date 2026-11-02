package com.luminovels.model;

import com.luminovels.nlp.SparseVector;

import java.util.List;

/**
 * Held-out scores over genre label sets.
 *
 * <p>Predicted set: the arg-max genre for softmax. For sigmoid, every genre with probability of at least
 * 0.5, or the arg-max genre when none reaches 0.5. Top-1 accuracy counts a hit when the arg-max genre is a
 * true genre. Macro F1 averages over genres that occur in the truth or in the predictions. Micro F1 pools counts.
 *
 * @param top1Accuracy share of samples whose arg-max genre is a true genre
 * @param macroF1 mean per-genre F1
 * @param microF1 F1 over pooled counts
 * @param exactMatch share of samples whose predicted set equals the true set
 */
public record ClassificationScores(double top1Accuracy, double macroF1, double microF1, double exactMatch) {

    public static final double SIGMOID_THRESHOLD = 0.5;

    public static ClassificationScores evaluate(NeuralNetwork net, List<SparseVector> xs, List<double[]> truth) {
        int n = xs.size();
        boolean[][] actual = new boolean[n][];
        boolean[][] predicted = new boolean[n][];
        int hits = 0;
        for (int s = 0; s < n; s++) {
            double[] p = net.predict(xs.get(s));
            actual[s] = toSet(truth.get(s));
            predicted[s] = predictedSet(p, net.activation());
            if (truth.get(s)[Metrics.argmax(p)] > 0.0) {
                hits++;
            }
        }
        ClassificationScores sets = fromSets(actual, predicted);
        return new ClassificationScores(n == 0 ? 0.0 : (double) hits / n, sets.macroF1, sets.microF1, sets.exactMatch);
    }

    static boolean[] predictedSet(double[] p, OutputActivation activation) {
        boolean[] set = new boolean[p.length];
        if (activation == OutputActivation.SIGMOID) {
            boolean any = false;
            for (int g = 0; g < p.length; g++) {
                set[g] = p[g] >= SIGMOID_THRESHOLD;
                any |= set[g];
            }
            if (any) {
                return set;
            }
        }
        set[Metrics.argmax(p)] = true;
        return set;
    }

    private static boolean[] toSet(double[] y) {
        boolean[] set = new boolean[y.length];
        for (int g = 0; g < y.length; g++) {
            set[g] = y[g] > 0.0;
        }
        return set;
    }

    /** Computes macro F1, micro F1 and exact match from label sets. Top-1 accuracy is left at 0. */
    public static ClassificationScores fromSets(boolean[][] actual, boolean[][] predicted) {
        int n = actual.length;
        if (n == 0) {
            return new ClassificationScores(0, 0, 0, 0);
        }
        int labels = actual[0].length;
        long[] tp = new long[labels];
        long[] fp = new long[labels];
        long[] fn = new long[labels];
        int exact = 0;
        for (int s = 0; s < n; s++) {
            boolean same = true;
            for (int g = 0; g < labels; g++) {
                if (actual[s][g] && predicted[s][g]) {
                    tp[g]++;
                } else if (predicted[s][g]) {
                    fp[g]++;
                } else if (actual[s][g]) {
                    fn[g]++;
                }
                same &= actual[s][g] == predicted[s][g];
            }
            if (same) {
                exact++;
            }
        }
        double macroSum = 0.0;
        int counted = 0;
        long tpAll = 0;
        long fpAll = 0;
        long fnAll = 0;
        for (int g = 0; g < labels; g++) {
            tpAll += tp[g];
            fpAll += fp[g];
            fnAll += fn[g];
            if (tp[g] + fp[g] + fn[g] > 0) {
                macroSum += f1(tp[g], fp[g], fn[g]);
                counted++;
            }
        }
        return new ClassificationScores(0.0, counted == 0 ? 0.0 : macroSum / counted,
                f1(tpAll, fpAll, fnAll), (double) exact / n);
    }

    private static double f1(long tp, long fp, long fn) {
        long denominator = 2 * tp + fp + fn;
        return denominator == 0 ? 0.0 : 2.0 * tp / denominator;
    }
}
