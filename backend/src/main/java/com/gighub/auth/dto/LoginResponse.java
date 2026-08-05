package com.gighub.auth.dto;

import com.gighub.member.domain.UserRole;
import lombok.Getter;

/** 로그인 성공 후 화면 진입에 필요한 최소 사용자 상태입니다. */
@Getter
public final class LoginResponse {

    private final UserRole role;
    private final String name;
    private final boolean needsWorkplaceSetup;

    public LoginResponse(UserRole role, String name, boolean needsWorkplaceSetup) {
        this.role = role;
        this.name = name;
        this.needsWorkplaceSetup = needsWorkplaceSetup;
    }
}
