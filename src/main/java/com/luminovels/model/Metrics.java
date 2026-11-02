package com.luminovels.model;

import com.luminovels.nlp.SparseVector;

import java.util.List;

/**
 * Evaluation helpers.
 */
public final class Metrics {

    private Metrics() {
    }

    public static int argmax(double[] values) {
        int best = 0;
        for (int i = 1; i < values.length; i++) {
            if (values[i] > values[best]) {
                best = i;
            }
        }
        return best;
    }

    /** Share of samples whose highest-probability genre is one of their true genres. */
    public static double top1Accuracy(NeuralNetwork net, List<SparseVector> xs, List<double[]> ys) {
        if (xs.isEmpty()) {
            return 0.0;
        }
        int hits = 0;
        for (int s = 0; s < xs.size(); s++) {
            if (ys.get(s)[argmax(net.predict(xs.get(s)))] > 0.0) {
                hits++;
            }
        }
        return (double) hits / xs.size();
    }
}
