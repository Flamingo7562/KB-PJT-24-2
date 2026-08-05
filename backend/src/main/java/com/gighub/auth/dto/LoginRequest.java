package com.gighub.auth.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.gighub.auth.validation.Utf8ByteLength;
import com.gighub.member.domain.UserRole;

/** 승인된 로그인 입력과 정규화·검증 계약입니다. */
public class LoginRequest {

    @NotBlank(message = "로그인 아이디는 필수입니다.")
    @Size(max = 50, message = "로그인 아이디는 50자 이하여야 합니다.")
    private String loginId;

    @NotBlank(message = "비밀번호는 필수입니다.")
    @Size(min = 8, max = 64, message = "비밀번호는 8자 이상 64자 이하여야 합니다.")
    @Utf8ByteLength(max = 72, message = "비밀번호는 UTF-8 기준 72byte 이하여야 합니다.")
    private String password;

    @NotNull(message = "예상 역할은 필수입니다.")
    private UserRole expectedRole;

    public String getLoginId() {
        return loginId;
    }

    public void setLoginId(String loginId) {
        this.loginId = AuthNormalizer.normalizeIdentity(loginId);
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public UserRole getExpectedRole() {
        return expectedRole;
    }

    public void setExpectedRole(UserRole expectedRole) {
        this.expectedRole = expectedRole;
    }

    /** 로그인 또한 승인되지 않은 필드를 조용히 수용하지 않습니다. */
    @JsonAnySetter
    public void rejectUnknownField(String fieldName, Object value) {
        throw new IllegalArgumentException("허용되지 않은 로그인 필드입니다: " + fieldName);
    }
}
