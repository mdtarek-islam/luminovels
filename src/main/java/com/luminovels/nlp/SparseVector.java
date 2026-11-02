package com.luminovels.nlp;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Immutable sparse vector with sorted indices.
 */
public final class SparseVector {

    private final int dimension;
    private final int[] indices;
    private final double[] values;

    private SparseVector(int dimension, int[] indices, double[] values) {
        this.dimension = dimension;
        this.indices = indices;
        this.values = values;
    }

    /** Builds a vector from index to value pairs. Zero values are dropped. */
    public static SparseVector of(int dimension, Map<Integer, Double> entries) {
        TreeMap<Integer, Double> sorted = new TreeMap<>(entries);
        List<Integer> idx = new ArrayList<>();
        List<Double> val = new ArrayList<>();
        for (Map.Entry<Integer, Double> e : sorted.entrySet()) {
            int i = e.getKey();
            if (i < 0 || i >= dimension) {
                throw new IllegalArgumentException("index " + i + " outside dimension " + dimension);
            }
            if (e.getValue() != 0.0) {
                idx.add(i);
                val.add(e.getValue());
            }
        }
        int[] indices = new int[idx.size()];
        double[] values = new double[val.size()];
        for (int k = 0; k < indices.length; k++) {
            indices[k] = idx.get(k);
            values[k] = val.get(k);
        }
        return new SparseVector(dimension, indices, values);
    }

    public static SparseVector fromDense(double[] dense) {
        Map<Integer, Double> entries = new TreeMap<>();
        for (int i = 0; i < dense.length; i++) {
            entries.put(i, dense[i]);
        }
        return of(dense.length, entries);
    }

    /** Returns the element-wise mean of the given vectors, which must share one dimension. */
    public static SparseVector mean(List<SparseVector> vectors) {
        if (vectors.isEmpty()) {
            throw new IllegalArgumentException("mean of no vectors");
        }
        int dim = vectors.get(0).dimension();
        Map<Integer, Double> sum = new TreeMap<>();
        for (SparseVector v : vectors) {
            if (v.dimension() != dim) {
                throw new IllegalArgumentException("dimension mismatch");
            }
            for (int k = 0; k < v.size(); k++) {
                sum.merge(v.index(k), v.value(k) / vectors.size(), Double::sum);
            }
        }
        return of(dim, sum);
    }

    public int dimension() {
        return dimension;
    }

    /** Number of stored non-zero entries. */
    public int size() {
        return indices.length;
    }

    public int index(int k) {
        return indices[k];
    }

    public double value(int k) {
        return values[k];
    }

    public double dot(SparseVector other) {
        double sum = 0.0;
        int a = 0;
        int b = 0;
        while (a < indices.length && b < other.indices.length) {
            if (indices[a] == other.indices[b]) {
                sum += values[a] * other.values[b];
                a++;
                b++;
            } else if (indices[a] < other.indices[b]) {
                a++;
            } else {
                b++;
            }
        }
        return sum;
    }

    public double norm() {
        return Math.sqrt(dot(this));
    }

    public double[] toDense() {
        double[] dense = new double[dimension];
        for (int k = 0; k < indices.length; k++) {
            dense[indices[k]] = values[k];
        }
        return dense;
    }
}
