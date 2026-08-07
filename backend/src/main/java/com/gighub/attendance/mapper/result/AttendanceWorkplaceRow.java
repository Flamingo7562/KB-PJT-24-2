package com.gighub.attendance.mapper.result;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/** 스캔 시점의 현재 사업장 좌표와 활성 QR Snapshot입니다. */
@Getter
@Setter
public class AttendanceWorkplaceRow {

    private Long workplaceId;
    private String status;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private BigDecimal radiusMeters;
    private Long qrTokenId;
    private byte[] tokenNonce;
}
