package com.luminovels.model;

/**
 * Output layer activation and its matching loss.
 *
 * <p>SOFTMAX pairs with categorical cross-entropy. SIGMOID pairs with binary cross-entropy,
 * the configuration in the project report. Both pairs give the output gradient p minus y.
 */
public enum OutputActivation {

    SOFTMAX,
    SIGMOID;

    private static final double EPSILON = 1e-12;

    double[] apply(double[] z) {
        double[] p = new double[z.length];
        if (this == SOFTMAX) {
            double max = Double.NEGATIVE_INFINITY;
            for (double v : z) {
                max = Math.max(max, v);
            }
            double sum = 0.0;
            for (int i = 0; i < z.length; i++) {
                p[i] = Math.exp(z[i] - max);
                sum += p[i];
            }
            for (int i = 0; i < z.length; i++) {
                p[i] /= sum;
            }
        } else {
            for (int i = 0; i < z.length; i++) {
                p[i] = 1.0 / (1.0 + Math.exp(-z[i]));
            }
        }
        return p;
    }

    double loss(double[] p, double[] y) {
        double sum = 0.0;
        for (int i = 0; i < p.length; i++) {
            if (this == SOFTMAX) {
                sum -= y[i] * Math.log(p[i] + EPSILON);
            } else {
                sum -= y[i] * Math.log(p[i] + EPSILON) + (1.0 - y[i]) * Math.log(1.0 - p[i] + EPSILON);
            }
        }
        return sum;
    }
}
