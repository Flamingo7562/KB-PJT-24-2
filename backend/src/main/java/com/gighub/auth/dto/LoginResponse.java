package com.gighub.auth.dto;

public class LoginResponse {

    private final String role;
    private final String name;
    private final boolean needsWorkplaceSetup;

    public LoginResponse(String role, String name, boolean needsWorkplaceSetup) {
        this.role = role;
        this.name = name;
        this.needsWorkplaceSetup = needsWorkplaceSetup;
    }

    public String getRole() {
        return role;
    }

    public String getName() {
        return name;
    }

    public boolean isNeedsWorkplaceSetup() {
        return needsWorkplaceSetup;
    }
}
