package com.gighub.auth.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class SessionResponse {

    private final boolean authenticated;
    private final String role;
    private final String name;
    private final Boolean needsWorkplaceSetup;

    private SessionResponse(boolean authenticated, String role, String name, Boolean needsWorkplaceSetup) {
        this.authenticated = authenticated;
        this.role = role;
        this.name = name;
        this.needsWorkplaceSetup = needsWorkplaceSetup;
    }

    public static SessionResponse authenticated(String role, String name, boolean needsWorkplaceSetup) {
        return new SessionResponse(true, role, name, needsWorkplaceSetup);
    }

    public static SessionResponse unauthenticated() {
        return new SessionResponse(false, null, null, null);
    }

    public boolean isAuthenticated() {
        return authenticated;
    }

    public String getRole() {
        return role;
    }

    public String getName() {
        return name;
    }

    public Boolean getNeedsWorkplaceSetup() {
        return needsWorkplaceSetup;
    }
}
