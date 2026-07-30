package com.gighub.auth.security;

import java.io.Serializable;
import java.util.Objects;

/**
 * HttpSession에 저장되는 최소 인증 정보. Spring Security {@code Authentication}의
 * principal로 사용한다. name은 로그인·세션 조회 응답 계약({role, name,
 * needsWorkplaceSetup})에 필요해 포함한다 — 그 외 개인정보는 담지 않는다.
 */
public final class AuthPrincipal implements Serializable {

    private static final long serialVersionUID = 1L;

    private final Long userId;
    private final String role;
    private final String name;

    public AuthPrincipal(Long userId, String role, String name) {
        this.userId = Objects.requireNonNull(userId, "userId must not be null");
        this.role = Objects.requireNonNull(role, "role must not be null");
        this.name = Objects.requireNonNull(name, "name must not be null");
    }

    public Long getUserId() {
        return userId;
    }

    public String getRole() {
        return role;
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof AuthPrincipal)) {
            return false;
        }
        AuthPrincipal that = (AuthPrincipal) other;
        return userId.equals(that.userId) && role.equals(that.role) && name.equals(that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, role, name);
    }
}
