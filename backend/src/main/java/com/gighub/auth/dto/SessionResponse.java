package com.gighub.auth.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.gighub.member.domain.UserRole;
import lombok.Getter;

/** 공개 Session 부트스트랩 응답입니다. */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class SessionResponse {

    private final boolean authenticated;
    private final UserRole role;
    private final String name;
    private final Boolean needsWorkplaceSetup;

    private SessionResponse(
            boolean authenticated,
            UserRole role,
            String name,
            Boolean needsWorkplaceSetup) {
        this.authenticated = authenticated;
        this.role = role;
        this.name = name;
        this.needsWorkplaceSetup = needsWorkplaceSetup;
    }

    public static SessionResponse unauthenticated() {
        return new SessionResponse(false, null, null, null);
    }

    public static SessionResponse authenticated(
            UserRole role,
            String name,
            boolean needsWorkplaceSetup) {
        return new SessionResponse(true, role, name, needsWorkplaceSetup);
    }
}
