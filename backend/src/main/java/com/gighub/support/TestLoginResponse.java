package com.gighub.support;

import lombok.Getter;

/** 로컬 수동 검증에서 수립한 사용자 Session 식별자입니다. */
@Getter
public final class TestLoginResponse {

    private final Long loginUserId;

    public TestLoginResponse(Long loginUserId) {
        this.loginUserId = loginUserId;
    }
}
