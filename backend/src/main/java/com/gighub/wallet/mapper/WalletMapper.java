package com.gighub.wallet.mapper;

import com.gighub.wallet.dto.WalletBalanceSnapshot;
import com.gighub.wallet.dto.WalletTransactionSnapshot;
import com.gighub.wallet.mapper.param.WalletTransactionParam;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface WalletMapper {
    // 기본 지갑
    Long getAvailableBalance(@Param("userId") Long userId);

    Long getLockedBalance(@Param("userId") Long userId);

    Long getWalletIdByUserId(@Param("userId") Long userId);

    int addAvailableBalance(@Param("userId") Long userId, @Param("amount") Long amount);

    int subtractAvailableBalance(@Param("userId") Long userId, @Param("amount") Long amount);

    // 비관적 락(FOR UPDATE) 잔액 조회
    Long getAvailableBalanceForUpdate(@Param("userId") Long userId);

    // 지갑 잔액 스냅샷 조회 및 행 잠금
    WalletBalanceSnapshot getWalletSnapshotForUpdate(@Param("userId") Long userId);

    // 예치: available >= amount 인 경우에만 1행 갱신
    int lockEmployerFunds(@Param("userId") Long userId, @Param("amount") Long amount);

    // 정산: locked >= amount 인 경우에만 1행 갱신
    int releaseLockedFunds(@Param("userId") Long userId, @Param("amount") Long amount);

    // 에스크로
    String getEscrowStatusForUpdate(@Param("workCaseId") Long workCaseId);

    int insertEscrowRecord(@Param("workCaseId") Long workCaseId, @Param("amount") Long amount);

    int holdEscrow(@Param("workCaseId") Long workCaseId);

    int releaseEscrow(@Param("workCaseId") Long workCaseId);

    Long getHeldEscrowAmount(@Param("workCaseId") Long workCaseId);

    Long getEscrowIdByWorkCaseId(@Param("workCaseId") Long workCaseId);

    // 원장
    int countTransactionByIdempotencyKey(@Param("idempotencyKey") String idempotencyKey);

    WalletTransactionSnapshot findTransactionByIdempotencyKey(
            @Param("idempotencyKey") String idempotencyKey);

    WalletTransactionSnapshot findFundingTransactionSnapshot(
            @Param("fundingOrderId") Long fundingOrderId,
            @Param("employerId") Long employerId,
            @Param("idempotencyKey") String idempotencyKey);

    WalletTransactionSnapshot findEscrowHoldTransactionSnapshot(
            @Param("workCaseId") Long workCaseId,
            @Param("escrowId") Long escrowId);

    int insertWalletTransaction(WalletTransactionParam param);
}
