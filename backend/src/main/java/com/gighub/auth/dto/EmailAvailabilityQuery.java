package com.gighub.auth.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/** 이메일 가용성 조회 Query입니다. */
public class EmailAvailabilityQuery {

    @NotBlank(message = "이메일은 필수입니다.")
    @Size(max = 255, message = "이메일은 255자 이하여야 합니다.")
    private String email;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = AuthNormalizer.normalizeIdentity(email);
    }
}
