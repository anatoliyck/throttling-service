package com.throttling.sla;

import com.throttling.sla.domain.UserSLA;

import java.util.concurrent.CompletableFuture;

public interface SLAService {

    CompletableFuture<UserSLA> getSlaByToken(String token);
}
