package com.throttling.service;

import com.throttling.sla.SLAService;
import com.throttling.util.RateLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ThrottlingServiceImpl implements ThrottlingService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ThrottlingServiceImpl.class);

    private final Map<String, String> userByToken = new ConcurrentHashMap<>();
    private final Set<String> tokenSLAInLoadState = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Map<String, RateLimiter> rateLimits = new ConcurrentHashMap<>();
    private final RateLimiter guestRateLimiter;
    private final SLAService slaService;

    public ThrottlingServiceImpl(int guestRPC, SLAService slaService) {
        this.guestRateLimiter = RateLimiter.create(guestRPC);
        this.slaService = slaService;
    }

    @Override
    public boolean isRequestAllowed(String token) {
        RateLimiter rateLimiter = guestRateLimiter;
        if (Objects.nonNull(token)) {
            String userId = userByToken.get(token);
            if (Objects.isNull(userId)) {
                loadUserRateLimiter(token);
            } else {
                RateLimiter userRateLimiter = rateLimits.get(userId);
                if (Objects.nonNull(userRateLimiter)) {
                    rateLimiter = userRateLimiter;
                }
            }
        }

        return rateLimiter.acquire();
    }

    private void loadUserRateLimiter(String token) {
        if (tokenSLAInLoadState.add(token)) {
            slaService.getSlaByToken(token)
                    .whenComplete((sla, ex) -> {
                        if (ex == null) {
                            rateLimits.putIfAbsent(sla.getUser(), RateLimiter.create(sla.getRps()));
                            userByToken.put(token, sla.getUser());
                        } else {
                            LOGGER.error("Unable to load user SLA by token!", ex);
                        }
                        tokenSLAInLoadState.remove(token);
                    });
        }
    }
}
