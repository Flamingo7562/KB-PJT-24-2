package com.gighub.attendance.service.command;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;

/** 검증된 스캔 입력과 요청마다 한 번 정한 서버 판정 시각입니다. */
@Getter
@Builder
public final class AttendanceScanCommand {

    private final long workerId;
    private final long workplaceId;
    private final byte[] qrNonce;
    private final BigDecimal latitude;
    private final BigDecimal longitude;
    private final BigDecimal accuracyMeters;
    private final Instant capturedAt;
    private final boolean confirmEarlyCheckout;
    private final LocalDateTime attemptedAt;
}
