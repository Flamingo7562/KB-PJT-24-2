package com.gighub.auth.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/** 로그인 아이디 가용성 조회 Query입니다. */
public class LoginIdAvailabilityQuery {

    @NotBlank(message = "로그인 아이디는 필수입니다.")
    @Size(max = 50, message = "로그인 아이디는 50자 이하여야 합니다.")
    private String loginId;

    public String getLoginId() {
        return loginId;
    }

    public void setLoginId(String loginId) {
        this.loginId = AuthNormalizer.normalizeIdentity(loginId);
    }
}
