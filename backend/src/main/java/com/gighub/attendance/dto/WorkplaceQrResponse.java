package com.gighub.attendance.dto;

import java.time.Instant;

import lombok.Getter;

/** 승인된 고정 QR 조회 응답입니다. */
@Getter
public final class WorkplaceQrResponse {

    private final Long workplaceId;
    private final String qrToken;
    private final Instant createdAt;

    public WorkplaceQrResponse(Long workplaceId, String qrToken, Instant createdAt) {
        this.workplaceId = workplaceId;
        this.qrToken = qrToken;
        this.createdAt = createdAt;
    }
}
