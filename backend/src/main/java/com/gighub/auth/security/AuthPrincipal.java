package com.gighub.auth.security;

import java.io.Serializable;
import java.util.Objects;

import com.gighub.member.domain.UserRole;

/** Session SecurityContext에 저장하는 최소 인증 사용자 정보입니다. */
public final class AuthPrincipal implements Serializable {

    private static final long serialVersionUID = 1L;

    private final Long userId;
    private final UserRole role;
    private final String name;

    public AuthPrincipal(Long userId, UserRole role, String name) {
        this.userId = Objects.requireNonNull(userId, "userId");
        this.role = Objects.requireNonNull(role, "role");
        this.name = Objects.requireNonNull(name, "name");
    }

    public Long getUserId() {
        return userId;
    }

    public UserRole getRole() {
        return role;
    }

    public String getName() {
        return name;
    }
}
