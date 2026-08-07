package com.gighub.attendance.service;

import com.gighub.attendance.dto.AttendanceScanResponse;
import lombok.Getter;

/** Controller가 최초 처리와 저장 응답 재생을 구분하는 결과입니다. */
@Getter
public final class AttendanceScanResult {

    private final AttendanceScanResponse response;
    private final boolean replayed;

    private AttendanceScanResult(AttendanceScanResponse response, boolean replayed) {
        this.response = response;
        this.replayed = replayed;
    }

    public static AttendanceScanResult first(AttendanceScanResponse response) {
        return new AttendanceScanResult(response, false);
    }

    public static AttendanceScanResult replayed(AttendanceScanResponse response) {
        return new AttendanceScanResult(response, true);
    }
}
