package com.gighub.idempotency;

/**
 * 멱등 Claim의 선점·완료·포기를 관리합니다.
 *
 * <p>세 Method의 Transaction 경계가 서로 다르고, 그 차이가 곧 계약입니다.</p>
 *
 * <ul>
 *   <li>{@link #claim}은 본 처리와 분리된 짧은 Transaction에서 즉시 Commit합니다. 본 처리가
 *   실패해 Rollback되더라도 "이 Key는 처리 중"이라는 사실이 남아야 동시 재전송을 막습니다.</li>
 *   <li>{@link #complete}는 호출자의 Transaction에 참여합니다. 응답 저장이 본 처리와 함께
 *   Commit되지 않으면 자금은 움직였는데 Replay할 응답이 없는 상태가 생깁니다.</li>
 *   <li>{@link #abandon}은 다시 별도 Transaction입니다. 실패한 본 처리와 함께 Rollback되면
 *   Claim이 남아 같은 Key로 영원히 재시도할 수 없습니다.</li>
 * </ul>
 */
public interface IdempotencyClaimService {

    /**
     * Claim을 선점하거나, 이미 완료된 같은 요청의 저장된 응답을 돌려줍니다.
     *
     * @param userId        인증 사용자 식별자
     * @param operationCode Operation 구분자. 같은 Key라도 Operation이 다르면 충돌하지 않습니다
     * @param rawKey        요청 Header의 Key 원문
     * @param fingerprint   검증을 통과한 정규화 값으로 만든 32byte Hash
     * @return 선점했으면 {@code started}, 완료된 같은 요청이면 {@code replay}
     */
    IdempotencyClaimResult claim(
            long userId,
            String operationCode,
            String rawKey,
            byte[] fingerprint);

    /**
     * 최초 성공의 응답을 저장하며 Claim을 완료합니다. 반드시 본 처리 Transaction 안에서
     * 마지막 DB 변경으로 호출합니다.
     *
     * @param claimId            {@link #claim}이 선점한 식별자
     * @param responseHttpStatus 최초 성공 응답의 HTTP 상태
     * @param responseBody       최초 성공 응답의 JSON 원문
     */
    void complete(long claimId, int responseHttpStatus, String responseBody);

    /**
     * 본 처리에 실패한 Claim을 지워 같은 Key의 재시도를 허용합니다.
     *
     * <p>본 처리 Transaction이 <b>끝난 뒤에</b> 호출해야 합니다. 본 처리가 Claim 행을 잠근 채
     * 이 Method를 부르면 새 Transaction이 같은 행의 잠금을 기다리다 스스로 교착합니다.</p>
     *
     * @param claimId 선점했던 식별자
     */
    void abandon(long claimId);
}
