package com.gighub.workplace.service.command;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;

/** 검증을 통과한 사업장 현장 위치 확정 입력입니다. */
@Getter
@Builder
public final class WorkplaceCoordinateConfirmCommand {

    private final Long workplaceId;
    private final BigDecimal latitude;
    private final BigDecimal longitude;
    private final BigDecimal accuracyMeters;
    private final Instant capturedAt;
}
