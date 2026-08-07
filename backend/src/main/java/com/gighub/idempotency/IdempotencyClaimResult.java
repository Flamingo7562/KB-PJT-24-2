package com.gighub.idempotency;

import java.util.Objects;

/**
 * Claim 선점 결과입니다. 본 처리에 진입할지, 저장된 응답을 Replay할지를 구분합니다.
 *
 * <p>Replay는 저장한 응답을 그대로 돌려주는 것이라 현재 도메인 상태를 다시 확인하지
 * 않습니다. 두 경우를 호출부가 상태 값으로 추측하지 않도록 결과 타입으로 나눕니다.</p>
 */
public final class IdempotencyClaimResult {

    private final Long claimId;
    private final Integer responseHttpStatus;
    private final String responseBody;

    private IdempotencyClaimResult(Long claimId, Integer responseHttpStatus, String responseBody) {
        this.claimId = claimId;
        this.responseHttpStatus = responseHttpStatus;
        this.responseBody = responseBody;
    }

    /** 이 요청이 Claim을 선점했으므로 본 처리에 진입합니다. */
    public static IdempotencyClaimResult started(long claimId) {
        return new IdempotencyClaimResult(claimId, null, null);
    }

    /** 같은 요청이 이미 성공했으므로 저장한 응답을 그대로 반환합니다. */
    public static IdempotencyClaimResult replay(int responseHttpStatus, String responseBody) {
        return new IdempotencyClaimResult(
                null,
                responseHttpStatus,
                Objects.requireNonNull(responseBody, "responseBody")
        );
    }

    public boolean isReplay() {
        return claimId == null;
    }

    /**
     * @return 선점한 Claim 식별자
     * @throws IllegalStateException Replay 결과에서 호출한 경우
     */
    public long getClaimId() {
        if (claimId == null) {
            throw new IllegalStateException("Replay 결과에는 선점한 Claim이 없습니다.");
        }
        return claimId;
    }

    public int getResponseHttpStatus() {
        if (responseHttpStatus == null) {
            throw new IllegalStateException("선점 결과에는 저장된 응답이 없습니다.");
        }
        return responseHttpStatus;
    }

    public String getResponseBody() {
        if (responseBody == null) {
            throw new IllegalStateException("선점 결과에는 저장된 응답이 없습니다.");
        }
        return responseBody;
    }
}
