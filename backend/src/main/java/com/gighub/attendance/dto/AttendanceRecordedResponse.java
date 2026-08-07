package com.gighub.attendance.dto;

import lombok.Getter;

import java.time.Instant;

/** 출근 또는 퇴근이 실제로 기록된 성공 응답입니다. */
@Getter
public final class AttendanceRecordedResponse implements AttendanceScanResponse {

    private final String result = "RECORDED";
    private final Long workCaseId;
    private final String scanType;
    private final Instant recordedAt;
    private final Boolean isLate;
    private final Integer lateMinutes;
    private final Instant earlyCheckoutConfirmedAt;
    private final Instant settlementDueAt;

    public AttendanceRecordedResponse(
            Long workCaseId,
            String scanType,
            Instant recordedAt,
            Boolean isLate,
            Integer lateMinutes,
            Instant earlyCheckoutConfirmedAt,
            Instant settlementDueAt) {
        this.workCaseId = workCaseId;
        this.scanType = scanType;
        this.recordedAt = recordedAt;
        this.isLate = isLate;
        this.lateMinutes = lateMinutes;
        this.earlyCheckoutConfirmedAt = earlyCheckoutConfirmedAt;
        this.settlementDueAt = settlementDueAt;
    }
}
