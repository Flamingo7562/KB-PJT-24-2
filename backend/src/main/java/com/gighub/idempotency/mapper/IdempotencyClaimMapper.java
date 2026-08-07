package com.gighub.idempotency.mapper;

import com.gighub.idempotency.mapper.param.IdempotencyClaimInsertParam;
import com.gighub.idempotency.mapper.result.IdempotencyClaimRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

/** 멱등 요청 Claim의 선점·조회·완료·삭제 SQL 진입점입니다. */
@Mapper
public interface IdempotencyClaimMapper {

    /**
     * {@code PROCESSING} Claim을 선점하고 생성된 식별자를 {@code param.id}에 채웁니다.
     *
     * <p>같은 {@code (userId, operationCode, idempotencyKey)}가 이미 있으면
     * {@code uk_idempotency_requests_scope}가 중복 예외를 던집니다. 사전 조회 후 INSERT하면 두
     * 요청이 같은 시점에 "없음"을 확인하고 모두 진입할 수 있으므로, 선점 여부는 이 제약으로만
     * 판정합니다.</p>
     */
    int insertProcessing(IdempotencyClaimInsertParam param);

    /**
     * 같은 범위의 Claim을 잠근 채 조회합니다.
     *
     * <p>선점에 실패한 요청이 기존 Claim의 상태와 Fingerprint를 읽는 경로입니다. 잠그지 않으면
     * 완료 직전의 Claim을 {@code PROCESSING}으로 읽고 충돌로 끝낼 수 있습니다.</p>
     *
     * @return 해당 범위의 Claim. 없으면 {@code null}
     */
    IdempotencyClaimRow findForUpdate(
            @Param("userId") long userId,
            @Param("operationCode") String operationCode,
            @Param("idempotencyKey") String idempotencyKey);

    /**
     * 최초 성공의 응답 Snapshot과 함께 Claim을 {@code COMPLETED}로 전이합니다.
     *
     * <p>본 처리의 마지막 DB 변경으로 실행해 같은 Transaction에서 함께 Commit합니다. 응답을
     * 별도 Transaction에 기록하면 자금은 움직였는데 Replay할 응답이 없는 상태가 생깁니다.</p>
     *
     * <p>{@code PROCESSING}일 때만 바뀝니다. 이미 완료된 Claim의 응답을 덮어쓰면 최초 성공의
     * 결과가 사라집니다.</p>
     *
     * @return 전이된 행 수. 이미 완료됐거나 없으면 0
     */
    int complete(
            @Param("claimId") long claimId,
            @Param("responseHttpStatus") int responseHttpStatus,
            @Param("responseBody") String responseBody,
            @Param("completedAt") LocalDateTime completedAt);

    /**
     * 본 처리에 실패한 {@code PROCESSING} Claim을 삭제합니다.
     *
     * <p>실패한 Claim을 남기면 같은 Key로는 영원히 재시도할 수 없습니다. 이미 완료된 Claim은
     * 지우지 않아야 Replay가 계속 가능합니다.</p>
     *
     * @return 삭제된 행 수. 완료 상태였으면 0
     */
    int deleteProcessing(@Param("claimId") long claimId);

    /**
     * 보존 기간이 지난 Claim을 지웁니다.
     *
     * <p>만료된 Claim이 남아 있으면 같은 Key를 다시 쓰려는 요청이 옛 Fingerprint와 비교돼
     * 재사용 오류로 거절됩니다.</p>
     *
     * @param now Asia/Seoul 기준 현재 시각
     * @return 삭제된 행 수
     */
    int deleteExpired(@Param("now") LocalDateTime now);
}
