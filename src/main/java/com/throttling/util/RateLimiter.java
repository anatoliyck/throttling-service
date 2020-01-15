package com.throttling.util;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class RateLimiter {

    private final int permits;
    private final Semaphore semaphore;
    private final AtomicBoolean sync = new AtomicBoolean();
    private volatile long syncTime;

    private RateLimiter(int permits) {
        if (permits < 0) {
            throw new IllegalArgumentException("Permits should more than zero!");
        }
        this.permits = permits;
        this.semaphore = new Semaphore(permits);
    }

    public boolean acquire() {
        if (TimeUnit.MILLISECONDS.convert(System.nanoTime() - syncTime, TimeUnit.NANOSECONDS) >= 1000
                && sync.compareAndSet(false, true)) {
            int toRelease = permits - semaphore.availablePermits();
            if (toRelease > 0) {
                semaphore.release(toRelease);
            }
            syncTime = System.nanoTime();
            sync.set(false);
        }

        return semaphore.tryAcquire();
    }

    public static RateLimiter create(int permits) {
        return new RateLimiter(permits);
    }
}
