package com.gighub.attendance.dto;

import lombok.Getter;

import java.time.Instant;

/** 예정 종료 전 퇴근에 사용자 확인이 필요한 응답입니다. */
@Getter
public final class AttendanceConfirmationRequiredResponse implements AttendanceScanResponse {

    private final String result = "CONFIRMATION_REQUIRED";
    private final Long workCaseId;
    private final String scanType = "CHECK_OUT";
    private final Instant scheduledEndAt;

    public AttendanceConfirmationRequiredResponse(Long workCaseId, Instant scheduledEndAt) {
        this.workCaseId = workCaseId;
        this.scheduledEndAt = scheduledEndAt;
    }
}
