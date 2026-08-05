package com.gighub.auth.security;

import com.gighub.common.exception.AuthRequiredException;
import org.springframework.security.core.Authentication;

/** 보호 API가 SecurityContext의 인증 Principal 하나만으로 사용자를 식별하도록 돕습니다. */
public final class AuthPrincipals {

    private AuthPrincipals() {
    }

    public static AuthPrincipal resolve(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthPrincipal)) {
            throw new AuthRequiredException("로그인이 필요합니다.");
        }
        return (AuthPrincipal) authentication.getPrincipal();
    }
}
