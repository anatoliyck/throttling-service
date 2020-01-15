package com.throttling.service;

public interface ThrottlingService {

    boolean isRequestAllowed(String token);
}
