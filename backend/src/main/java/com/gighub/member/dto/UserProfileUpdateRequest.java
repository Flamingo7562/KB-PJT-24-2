package com.gighub.member.dto;

import javax.validation.constraints.Pattern;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.gighub.auth.dto.AuthNormalizer;

/** 승인 명세의 내 프로필 수정 입력입니다. 전화번호만 허용합니다. */
public final class UserProfileUpdateRequest {

    @Pattern(regexp = "^0\\d{8,10}$", message = "전화번호 형식이 올바르지 않습니다.")
    private final String phone;

    @JsonCreator
    public UserProfileUpdateRequest(@JsonProperty("phone") String phone) {
        this.phone = AuthNormalizer.normalizePhone(phone);
    }

    public String getPhone() {
        return phone;
    }

    /** 명세에 없는 프로필 수정 필드는 조용히 무시하지 않고 요청 오류로 처리합니다. */
    @JsonAnySetter
    public void rejectUnknownField(String fieldName, Object value) {
        throw new IllegalArgumentException("허용되지 않은 프로필 수정 필드입니다: " + fieldName);
    }
}