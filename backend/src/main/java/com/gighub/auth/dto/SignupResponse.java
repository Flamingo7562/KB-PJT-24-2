package com.gighub.auth.dto;

import lombok.Getter;

/** 가입 성공 후 생성된 사용자 식별자입니다. */
@Getter
public final class SignupResponse {

    private final Long userId;

    public SignupResponse(Long userId) {
        this.userId = userId;
    }
}
