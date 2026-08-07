package com.gighub.idempotency.exception;

import com.gighub.common.api.ApiErrorCode;
import com.gighub.common.exception.ApiException;
import org.springframework.http.HttpStatus;

/**
 * 같은 Key를 내용이 다른 요청에 다시 사용한 경우를 409로 반환합니다.
 *
 * <p>이름이 비슷한 {@code com.gighub.wallet.exception.IdempotencyKeyReusedException}은 충전·
 * 출금이 Claim 도입 전부터 쓰던 도메인 예외입니다. 두 경로가 아직 공존하므로 Import를
 * 헷갈리지 않도록 이름을 구분했습니다.</p>
 */
public class IdempotencyClaimKeyReusedException extends ApiException {

    private static final String MESSAGE = "같은 멱등 키를 다른 요청에 사용할 수 없습니다.";

    public IdempotencyClaimKeyReusedException() {
        super(HttpStatus.CONFLICT, ApiErrorCode.IDEMPOTENCY_KEY_REUSED, MESSAGE);
    }
}
