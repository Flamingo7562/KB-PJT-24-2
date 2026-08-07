package com.gighub.attendance.exception;

import com.gighub.common.api.ApiErrorCode;
import com.gighub.common.exception.ApiException;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/** 승인된 근태 오류 Code와 선택적인 감사 거부 사유를 함께 전달합니다. */
@Getter
public class AttendanceException extends ApiException {

    private final String failureReason;

    private AttendanceException(
            HttpStatus status,
            ApiErrorCode code,
            String message,
            String failureReason) {
        super(status, code, message);
        this.failureReason = failureReason;
    }

    public static AttendanceException qrInvalid() {
        return new AttendanceException(
                HttpStatus.UNPROCESSABLE_ENTITY,
                ApiErrorCode.QR_INVALID,
                "유효하지 않은 QR입니다.",
                null);
    }

    public static AttendanceException qrRevoked() {
        return new AttendanceException(
                HttpStatus.GONE,
                ApiErrorCode.QR_REVOKED,
                "폐기된 QR입니다. 사업장의 현재 QR을 다시 스캔해 주세요.",
                null);
    }

    public static AttendanceException workplaceLocationRequired() {
        return new AttendanceException(
                HttpStatus.CONFLICT,
                ApiErrorCode.WORKPLACE_LOCATION_REQUIRED,
                "사업장 출퇴근 위치가 확정되지 않았습니다.",
                null);
    }

    public static AttendanceException workCaseNotFound() {
        return new AttendanceException(
                HttpStatus.NOT_FOUND,
                ApiErrorCode.ATTENDANCE_WORK_CASE_NOT_FOUND,
                "처리할 근무를 찾을 수 없습니다.",
                null);
    }

    public static AttendanceException workCaseAmbiguous() {
        return new AttendanceException(
                HttpStatus.CONFLICT,
                ApiErrorCode.ATTENDANCE_WORK_CASE_AMBIGUOUS,
                "처리할 근무가 둘 이상입니다.",
                null);
    }

    public static AttendanceException alreadyCompleted() {
        return new AttendanceException(
                HttpStatus.CONFLICT,
                ApiErrorCode.ATTENDANCE_ALREADY_COMPLETED,
                "이미 출퇴근이 완료된 근무입니다.",
                null);
    }

    public static AttendanceException locationInvalid(String failureReason) {
        return new AttendanceException(
                HttpStatus.UNPROCESSABLE_ENTITY,
                ApiErrorCode.LOCATION_INVALID,
                "현재 위치를 다시 측정해 주세요.",
                failureReason);
    }

    public static AttendanceException outsideRadius() {
        return new AttendanceException(
                HttpStatus.UNPROCESSABLE_ENTITY,
                ApiErrorCode.OUTSIDE_WORKPLACE_RADIUS,
                "사업장 출퇴근 반경 밖입니다.",
                "OUTSIDE_RADIUS");
    }

    public static AttendanceException stateConflict(String failureReason) {
        return new AttendanceException(
                HttpStatus.CONFLICT,
                ApiErrorCode.ATTENDANCE_STATE_CONFLICT,
                "근무 상태가 변경되었습니다. 다시 확인해 주세요.",
                failureReason);
    }

    public static AttendanceException temporarilyUnavailable() {
        return new AttendanceException(
                HttpStatus.SERVICE_UNAVAILABLE,
                ApiErrorCode.ATTENDANCE_TEMPORARILY_UNAVAILABLE,
                "출퇴근 처리가 지연되고 있습니다. 같은 요청으로 다시 시도해 주세요.",
                null);
    }
}
