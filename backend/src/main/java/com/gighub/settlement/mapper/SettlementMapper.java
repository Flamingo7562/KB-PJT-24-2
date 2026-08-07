package com.gighub.settlement.mapper;

import com.gighub.settlement.dto.SettlementSnapshot;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.time.LocalDateTime;

@Mapper
public interface SettlementMapper {

    /**
     * 수락 시점에 정산을 예약한다.
     *
     * <p>금액은 근무의 일급과 같아야 한다. 복합 FK {@code fk_settlements_case_wage}가
     * {@code (work_case_id, amount)}를 {@code work_cases(id, agreed_wage)}와 대조하므로 다른
     * 금액으로는 예약할 수 없다.</p>
     *
     * <p>상태와 시각은 승인 계약이 정한 초기값으로 서버가 고정한다. {@code due_at}과 승인·처리·
     * 완료·실패 필드는 이후 정산 흐름이 채운다.</p>
     *
     * @return 저장된 행 수
     */
    int insertWaiting(
            @Param("workCaseId") Long workCaseId,
            @Param("amount") Long amount);

    // 정산 원장: 지급 판단부터 완료까지 동일 행을 잠근다.
    SettlementSnapshot findByWorkCaseIdForUpdate(
            @Param("workCaseId") Long workCaseId);

    // 분쟁 게이트: 지급을 막는 분쟁 행과 빈 구간을 정산 완료까지 잠근다.
    List<Long> findBlockingDisputeIdsForUpdate(
            @Param("workCaseId") Long workCaseId);

    // 정상 퇴근으로 지급 예약된 정산만 수동 승인 처리로 진입시킨다.
    int transitionScheduledToProcessing(
            @Param("settlementId") Long settlementId,
            @Param("approvedByUserId") Long approvedByUserId);

    // 상태 전이: 같은 승인자가 처리 중인 정산만 완료한다.
    int transitionProcessingToCompleted(
            @Param("settlementId") Long settlementId,
            @Param("approvedByUserId") Long approvedByUserId);

    /** 정상 퇴근과 같은 Transaction에서 24시간 뒤 지급 대상으로 예약합니다. */
    int scheduleWaiting(
            @Param("workCaseId") Long workCaseId,
            @Param("dueAt") LocalDateTime dueAt);
}
