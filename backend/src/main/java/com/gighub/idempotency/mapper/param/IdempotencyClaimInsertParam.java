package com.gighub.idempotency.mapper.param;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 멱등 Claim 선점 INSERT 파라미터입니다.
 *
 * <p>{@code PROCESSING} 상태의 행만 만들므로 응답 Snapshot 필드를 두지 않습니다. 스키마의
 * {@code ck_idempotency_requests_lifecycle}도 {@code PROCESSING}에는 응답 값이 없어야 한다고
 * 강제합니다.</p>
 *
 * <p>{@code requestFingerprint}는 검증을 통과한 정규화 값으로 만든 32byte Hash입니다. 원본
 * 입력은 담지 않습니다.</p>
 */
@Getter
@Builder
public class IdempotencyClaimInsertParam {

    /** MyBatis가 생성 Key를 되돌려 쓰기 위해 이 필드만 가변입니다. */
    @Setter
    private Long id;

    private final Long userId;
    private final String operationCode;
    private final String idempotencyKey;
    private final byte[] requestFingerprint;
    private final LocalDateTime expiresAt;
}
