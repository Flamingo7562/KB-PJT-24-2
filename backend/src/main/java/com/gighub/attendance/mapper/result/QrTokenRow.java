package com.gighub.attendance.mapper.result;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

/**
 * 활성 고정 QR 한 건의 조회 결과입니다.
 *
 * <p>완성 Token은 여기 없습니다. 응답 Token은 이 {@code tokenNonce}로부터 매번 다시
 * 서명합니다.</p>
 */
@Getter
@Setter
public class QrTokenRow {

    private Long id;
    private Long workplaceId;
    private byte[] tokenNonce;
    private LocalDateTime createdAt;
}
