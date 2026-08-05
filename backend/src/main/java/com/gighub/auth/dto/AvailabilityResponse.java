package com.gighub.auth.dto;

import lombok.Getter;

/** 아이디 또는 이메일 가용성 결과입니다. */
@Getter
public final class AvailabilityResponse {

    private final boolean available;

    public AvailabilityResponse(boolean available) {
        this.available = available;
    }
}
