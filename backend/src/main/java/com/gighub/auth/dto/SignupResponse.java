package com.gighub.auth.dto;

public class SignupResponse {

    private final Long userId;

    public SignupResponse(Long userId) {
        this.userId = userId;
    }

    public Long getUserId() {
        return userId;
    }
}
