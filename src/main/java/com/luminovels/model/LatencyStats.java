package com.luminovels.model;

import java.util.Arrays;

/**
 * Latency summary in milliseconds, using nearest-rank percentiles.
 *
 * @param p50Ms median
 * @param p95Ms 95th percentile
 * @param meanMs mean
 * @param samples number of timings
 */
public record LatencyStats(double p50Ms, double p95Ms, double meanMs, int samples) {

    public static LatencyStats fromNanos(long[] nanos) {
        if (nanos.length == 0) {
            return new LatencyStats(0, 0, 0, 0);
        }
        long[] sorted = nanos.clone();
        Arrays.sort(sorted);
        double sum = 0.0;
        for (long v : sorted) {
            sum += v;
        }
        return new LatencyStats(rank(sorted, 0.50) / 1e6, rank(sorted, 0.95) / 1e6,
                sum / sorted.length / 1e6, sorted.length);
    }

    private static long rank(long[] sorted, double p) {
        int index = (int) Math.ceil(p * sorted.length) - 1;
        return sorted[Math.max(0, Math.min(sorted.length - 1, index))];
    }
}
