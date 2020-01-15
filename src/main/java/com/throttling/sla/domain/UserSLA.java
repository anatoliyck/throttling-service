package com.throttling.sla.domain;

public class UserSLA {

    private final String user;
    private final int rps;

    public UserSLA(String user, int rps) {
        this.user = user;
        this.rps = rps;
    }

    public String getUser() {
        return this.user;
    }

    public int getRps() {
        return this.rps;
    }
}
