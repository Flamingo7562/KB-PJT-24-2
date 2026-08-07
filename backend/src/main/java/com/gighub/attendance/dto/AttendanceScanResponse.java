package com.gighub.attendance.dto;

/** 기록 성공과 조기 퇴근 확인 필요 응답이 공유하는 스캔 결과 경계입니다. */
public interface AttendanceScanResponse {

    String getResult();

    Long getWorkCaseId();

    String getScanType();
}
