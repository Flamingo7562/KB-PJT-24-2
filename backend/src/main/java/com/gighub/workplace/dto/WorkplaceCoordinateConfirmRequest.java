package com.gighub.workplace.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import lombok.Getter;

import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Digits;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;

/** 좌표가 없는 사업장에서 OWNER가 현장 위치를 한 번 확정하는 요청입니다. */
@Getter
public final class WorkplaceCoordinateConfirmRequest {

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
    @DecimalMax(value = "100", message = "위치 정확도는 100 이하여야 합니다.")
    @Digits(integer = 3, fraction = 2, message = "위치 정확도는 소수점 2자리까지만 허용합니다.")
    private BigDecimal accuracyMeters;

    @NotNull(message = "위치 측정 시각은 필수입니다.")
    private Instant capturedAt;

    @JsonAnySetter
    public void rejectUnknownField(String fieldName, Object value) {
        throw new IllegalArgumentException("허용되지 않은 사업장 위치 필드입니다: " + fieldName);
    }
}
