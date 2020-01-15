package com.throttling.service;

import com.throttling.sla.SLAService;
import com.throttling.sla.domain.UserSLA;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class ThrottlingServiceImpTest {

    @Test
    public void shouldAcquireLimitForGuest() {
        ThrottlingService throttlingService = new ThrottlingServiceImpl(2, new MockSLAService(Collections.emptyMap()));

        Assert.assertTrue(throttlingService.isRequestAllowed(null));
        Assert.assertTrue(throttlingService.isRequestAllowed(null));
        Assert.assertFalse(throttlingService.isRequestAllowed(null));
    }

    @Test
    public void shouldAcquireLimitForUserWithSingleToken() {
        String token = "token1";
        UserSLA sla = new UserSLA("test", 2);

        ThrottlingService throttlingService = new ThrottlingServiceImpl(0, new MockSLAService(Map.of(token, sla)));

        // guest access rejected
        Assert.assertFalse(throttlingService.isRequestAllowed(token));
        // user access
        Assert.assertTrue(throttlingService.isRequestAllowed(token));
        Assert.assertTrue(throttlingService.isRequestAllowed(token));
        Assert.assertFalse(throttlingService.isRequestAllowed(token));
        Assert.assertFalse(throttlingService.isRequestAllowed(token));
        Assert.assertFalse(throttlingService.isRequestAllowed(token));
    }

    @Test
    public void shouldAcquireLimitForUserWithMultipleTokens() {
        String token = "token1";
        String token2 = "token2";
        UserSLA sla = new UserSLA("test", 2);

        ThrottlingService throttlingService = new ThrottlingServiceImpl(0, new MockSLAService(Map.of(token, sla, token2, sla)));

        // guest access rejected (first token loading...)
        Assert.assertFalse(throttlingService.isRequestAllowed(token));
        Assert.assertFalse(throttlingService.isRequestAllowed(token2));

        // user access
        Assert.assertTrue(throttlingService.isRequestAllowed(token2));
        Assert.assertTrue(throttlingService.isRequestAllowed(token));
        Assert.assertFalse(throttlingService.isRequestAllowed(token2));
    }

    @Test
    public void shouldAcquireLimitForMultipleUsers() {
        String token = "token1";
        String token2 = "token2";

        UserSLA userSLA = new UserSLA("u1", 1);
        UserSLA userSLA2 = new UserSLA("u2", 1);

        ThrottlingService throttlingService = new ThrottlingServiceImpl(0, new MockSLAService(Map.of(token, userSLA, token2, userSLA2)));

        // guest access rejected (first token loading...)
        Assert.assertFalse(throttlingService.isRequestAllowed(token));
        Assert.assertFalse(throttlingService.isRequestAllowed(token2));

        // user access
        Assert.assertTrue(throttlingService.isRequestAllowed(token));
        Assert.assertTrue(throttlingService.isRequestAllowed(token2));
        Assert.assertFalse(throttlingService.isRequestAllowed(token));
        Assert.assertFalse(throttlingService.isRequestAllowed(token2));
    }

    @Test
    public void shouldUseOnlyGuestAccessIfUnableToLoadUserSLA() {
        ThrottlingService throttlingService = new ThrottlingServiceImpl(3, token -> CompletableFuture.failedFuture(new RuntimeException("Some exception")));

        Assert.assertTrue(throttlingService.isRequestAllowed("1"));
        Assert.assertTrue(throttlingService.isRequestAllowed("1"));
        Assert.assertTrue(throttlingService.isRequestAllowed("2"));
        Assert.assertFalse(throttlingService.isRequestAllowed("2"));
        Assert.assertFalse(throttlingService.isRequestAllowed(null));
        Assert.assertFalse(throttlingService.isRequestAllowed("3"));
    }

    @Test
    public void shouldAcquireLimitWithTimeRange() throws InterruptedException {
        ThrottlingService throttlingService = new ThrottlingServiceImpl(2, new MockSLAService(Collections.emptyMap()));

        String token = "11111";
        Assert.assertTrue(throttlingService.isRequestAllowed(token));
        Assert.assertTrue(throttlingService.isRequestAllowed(token));
        Assert.assertFalse(throttlingService.isRequestAllowed(token));

        TimeUnit.MILLISECONDS.sleep(1000);

        Assert.assertTrue(throttlingService.isRequestAllowed(null));
        Assert.assertTrue(throttlingService.isRequestAllowed(token));
        Assert.assertFalse(throttlingService.isRequestAllowed(token));
        Assert.assertFalse(throttlingService.isRequestAllowed(token));
    }

    private static class MockSLAService implements SLAService {

        private final Map<String, UserSLA> sla;

        private MockSLAService(Map<String, UserSLA> sla) {
            this.sla = sla;
        }

        @Override
        public CompletableFuture<UserSLA> getSlaByToken(String token) {
            return CompletableFuture.completedFuture(sla.get(token));
        }
    }
}