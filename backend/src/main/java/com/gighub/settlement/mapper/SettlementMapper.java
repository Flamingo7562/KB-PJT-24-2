package com.gighub.settlement.mapper;

import com.gighub.settlement.dto.SettlementSnapshot;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SettlementMapper {

    // 정산 원장: 지급 판단부터 완료까지 동일 행을 잠근다.
    SettlementSnapshot findByWorkCaseIdForUpdate(
            @Param("workCaseId") Long workCaseId);

    // 분쟁 게이트: 지급을 막는 분쟁 행과 빈 구간을 정산 완료까지 잠근다.
    List<Long> findBlockingDisputeIdsForUpdate(
            @Param("workCaseId") Long workCaseId);

    // 상태 전이: 수동 승인 가능한 WAITING 정산만 처리 중으로 바꾼다.
    int transitionWaitingToProcessing(
            @Param("settlementId") Long settlementId,
            @Param("approvedByUserId") Long approvedByUserId);

    // 상태 전이: 같은 승인자가 처리 중인 정산만 완료한다.
    int transitionProcessingToCompleted(
            @Param("settlementId") Long settlementId,
            @Param("approvedByUserId") Long approvedByUserId);
}
