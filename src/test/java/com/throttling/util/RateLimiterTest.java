package com.throttling.util;

import net.jodah.concurrentunit.Waiter;
import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.*;

public class RateLimiterTest {

    @Test
    public void shouldAcquireTwoPermits() {
        RateLimiter rateLimiter = RateLimiter.create(2);

        Assert.assertTrue(rateLimiter.acquire());
        Assert.assertTrue(rateLimiter.acquire());
        Assert.assertFalse(rateLimiter.acquire());
    }

    @Test
    public void shouldAcquireWithSingleThread() throws TimeoutException, InterruptedException {
        RateLimiter rateLimiter = RateLimiter.create(3);

        Waiter waiter = new Waiter();

        CompletableFuture.runAsync(() -> {
            rateLimiter.acquire();
            rateLimiter.acquire();
            waiter.resume();
        });

        waiter.await(500);
        waiter.assertTrue(rateLimiter.acquire());
        waiter.assertFalse(rateLimiter.acquire());
    }

    @Test
    public void shouldAcquireWithMultipleThreads() throws InterruptedException, TimeoutException {
        RateLimiter rateLimiter = RateLimiter.create(4);

        Waiter waiter = new Waiter();
        CountDownLatch latch = new CountDownLatch(1);

        Runnable task = () -> {
            try {
                latch.await();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            waiter.assertTrue(rateLimiter.acquire());
            waiter.assertTrue(rateLimiter.acquire());
            waiter.resume();

        };

        CompletableFuture.runAsync(task);
        CompletableFuture.runAsync(task);

        latch.countDown();
        waiter.await(500, 2);
        waiter.assertFalse(rateLimiter.acquire());
    }

    @Test
    public void shouldAcquireWithTimeoutOneSecond() throws InterruptedException {
        RateLimiter rateLimiter = RateLimiter.create(1);

        Assert.assertTrue(rateLimiter.acquire());
        Assert.assertFalse(rateLimiter.acquire());

        TimeUnit.MILLISECONDS.sleep(1000);

        Assert.assertTrue(rateLimiter.acquire());
    }

    @Test
    public void shouldNotAcquireWithinOneSecond() throws InterruptedException {
        RateLimiter rateLimiter = RateLimiter.create(1);

        Assert.assertTrue(rateLimiter.acquire());
        Assert.assertFalse(rateLimiter.acquire());

        TimeUnit.MILLISECONDS.sleep(900);

        Assert.assertFalse(rateLimiter.acquire());
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowExceptionWhenPermitsIsLessZero() {
        RateLimiter.create(-1);
    }
}