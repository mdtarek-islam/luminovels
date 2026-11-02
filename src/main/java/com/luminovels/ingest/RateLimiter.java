package com.luminovels.ingest;

import java.util.function.LongSupplier;

/**
 * Enforces a minimum gap between consecutive requests.
 */
public final class RateLimiter {

    private final long minIntervalMillis;
    private final LongSupplier clockMillis;
    private final Sleeper sleeper;
    private long lastMillis = Long.MIN_VALUE;

    public RateLimiter(long minIntervalMillis, LongSupplier clockMillis, Sleeper sleeper) {
        if (minIntervalMillis < 0) {
            throw new IllegalArgumentException("minIntervalMillis must be >= 0");
        }
        this.minIntervalMillis = minIntervalMillis;
        this.clockMillis = clockMillis;
        this.sleeper = sleeper;
    }

    public static RateLimiter systemClock(long minIntervalMillis) {
        return new RateLimiter(minIntervalMillis, System::currentTimeMillis, Sleeper.system());
    }

    /** Blocks until the next request is allowed, then records the request time. */
    public void acquire() throws InterruptedException {
        long now = clockMillis.getAsLong();
        if (lastMillis != Long.MIN_VALUE) {
            long wait = lastMillis + minIntervalMillis - now;
            if (wait > 0) {
                sleeper.sleep(wait);
                now += wait;
            }
        }
        lastMillis = now;
    }
}
