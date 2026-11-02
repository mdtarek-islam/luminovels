package com.luminovels.ingest;

/**
 * Pauses the current thread. Tests inject a recorder so no real time passes.
 */
@FunctionalInterface
public interface Sleeper {

    void sleep(long millis) throws InterruptedException;

    static Sleeper system() {
        return Thread::sleep;
    }
}
