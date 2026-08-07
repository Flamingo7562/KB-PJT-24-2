package com.gighub.attendance.mapper.param;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 성공 또는 의미상 거부 근태 한 건의 저장 입력입니다. */
@Getter
@Builder
public final class AttendanceRecordInsertParam {

    private final Long workCaseId;
    private final Long workerId;
    private final Long qrTokenId;
    private final String attendanceType;
    private final LocalDateTime capturedAt;
    private final LocalDateTime attemptedAt;
    private final BigDecimal distanceMeters;
    private final BigDecimal accuracyMeters;
    private final String result;
    private final String failureReason;
    private final LocalDateTime earlyCheckoutConfirmedAt;
}
