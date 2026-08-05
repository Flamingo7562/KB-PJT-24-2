package com.gighub.auth.service;

import java.util.Objects;

import com.gighub.auth.security.AuthPrincipal;

/** 자격 증명 검증 후 Session 수립에 전달하는 내부 로그인 결과입니다. */
public final class LoginResult {

    private final AuthPrincipal principal;
    private final boolean needsWorkplaceSetup;

    public LoginResult(AuthPrincipal principal, boolean needsWorkplaceSetup) {
        this.principal = Objects.requireNonNull(principal, "principal");
        this.needsWorkplaceSetup = needsWorkplaceSetup;
    }

    public AuthPrincipal getPrincipal() {
        return principal;
    }

    public boolean isNeedsWorkplaceSetup() {
        return needsWorkplaceSetup;
    }
}
