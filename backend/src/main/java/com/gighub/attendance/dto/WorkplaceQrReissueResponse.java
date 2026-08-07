package com.gighub.attendance.dto;

import java.time.Instant;

import lombok.Getter;

/**
 * 승인된 고정 QR 재발급 응답입니다.
 *
 * <p>언제나 현재 활성 QR의 권위 있는 표현입니다. 요청이 몇 번 도달하든 활성 QR은 정확히
 * 하나이며 마지막 응답이 진실입니다.</p>
 */
@Getter
public final class WorkplaceQrReissueResponse {

    private final Long workplaceId;
    private final String qrToken;
    private final Instant reissuedAt;

    public WorkplaceQrReissueResponse(Long workplaceId, String qrToken, Instant reissuedAt) {
        this.workplaceId = workplaceId;
        this.qrToken = qrToken;
        this.reissuedAt = reissuedAt;
    }
}
