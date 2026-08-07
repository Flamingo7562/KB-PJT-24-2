package com.gighub.idempotency.mapper.result;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 저장된 멱등 Claim 한 건입니다.
 *
 * <p>{@code status}는 DB 값을 그대로 담습니다. 허용 값은
 * {@code ck_idempotency_requests_status}가 {@code PROCESSING}과 {@code COMPLETED}로
 * 제한합니다.</p>
 *
 * <p>{@code responseBody}는 최초 성공의 응답 JSON 원문입니다. Replay는 이 값을 그대로 다시
 * 내보내며 현재 도메인 상태를 다시 조회하지 않습니다.</p>
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IdempotencyClaimRow {

    private Long id;
    private Long userId;
    private String operationCode;
    private byte[] requestFingerprint;
    private String status;
    private Integer responseHttpStatus;
    private String responseBody;
    private LocalDateTime expiresAt;
}
