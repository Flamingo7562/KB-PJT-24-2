package com.gighub.attendance.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import lombok.Getter;

import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Digits;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;

/** WORKER가 QR과 브라우저 위치를 함께 보내는 출퇴근 스캔 요청입니다. */
@Getter
public final class AttendanceScanRequest {

    private String qrToken;

    @NotNull(message = "위도는 필수입니다.")
    @DecimalMin(value = "-90", message = "위도는 -90 이상이어야 합니다.")
    @DecimalMax(value = "90", message = "위도는 90 이하여야 합니다.")
    @Digits(integer = 3, fraction = 7, message = "위도는 소수점 7자리까지만 허용합니다.")
    private BigDecimal latitude;

    @NotNull(message = "경도는 필수입니다.")
    @DecimalMin(value = "-180", message = "경도는 -180 이상이어야 합니다.")
    @DecimalMax(value = "180", message = "경도는 180 이하여야 합니다.")
    @Digits(integer = 3, fraction = 7, message = "경도는 소수점 7자리까지만 허용합니다.")
    private BigDecimal longitude;

    @NotNull(message = "위치 정확도는 필수입니다.")
    @DecimalMin(value = "0", message = "위치 정확도는 0 이상이어야 합니다.")
    @Digits(integer = 8, fraction = 2, message = "위치 정확도 형식을 확인해 주세요.")
    private BigDecimal accuracyMeters;

    @NotNull(message = "위치 측정 시각은 필수입니다.")
    private Instant capturedAt;

    @NotNull(message = "조기 퇴근 확인 여부는 필수입니다.")
    private Boolean confirmEarlyCheckout;

    @JsonAnySetter
    public void rejectUnknownField(String fieldName, Object value) {
        throw new IllegalArgumentException("허용되지 않은 출퇴근 스캔 필드입니다: " + fieldName);
    }
}
