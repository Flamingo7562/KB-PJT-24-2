package com.gighub.workplace.mapper.result;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/** 일회성 사업장 좌표 확정 시 잠근 현재 좌표입니다. */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class WorkplaceCoordinateLockRow {

    private Long id;
    private BigDecimal latitude;
    private BigDecimal longitude;
}
