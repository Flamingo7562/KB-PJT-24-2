package com.gighub.member.dto;

import com.gighub.member.domain.User;
import com.gighub.member.domain.UserRole;
import com.gighub.member.domain.UserStatus;
import lombok.Getter;

/** 승인 명세의 내 프로필 조회·수정 응답입니다. */
@Getter
public final class UserProfileResponse {

    private final String loginId;
    private final String email;
    private final String name;
    private final String phone;
    private final UserRole role;
    private final UserStatus status;

    private UserProfileResponse(
            String loginId,
            String email,
            String name,
            String phone,
            UserRole role,
            UserStatus status) {
        this.loginId = loginId;
        this.email = email;
        this.name = name;
        this.phone = phone;
        this.role = role;
        this.status = status;
    }

    public static UserProfileResponse from(User user) {
        return new UserProfileResponse(
                user.getLoginId(),
                user.getEmail(),
                user.getName(),
                user.getPhone(),
                user.getRole(),
                user.getStatus());
    }
}