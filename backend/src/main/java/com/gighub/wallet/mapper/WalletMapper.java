package com.gighub.wallet.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.security.core.parameters.P;

import java.math.BigDecimal;

@Mapper
public interface WalletMapper {
    // 기본 지갑
    BigDecimal getAvailableBalance(Long userId);

    int addAvailableBalance(@Param("userId") Long userId, @Param("amount") BigDecimal amount);
    int subtractAvailableBalance(@Param("userId") Long userId, @Param("amount") BigDecimal amount);

    // 에스크로 기능
    // 예치 : 사장 지갑의 사용 가능 잔액 차감, 자금 잔액 증가
    int lockEmployerFunds(@Param("userId") Long userId, @Param("amount") BigDecimal amount);
    // 정산 : 사장 지갑의 잠금 잔액 최종 차감
    int releaseLockedFunds(@Param("userId") Long userId, @Param("amount") BigDecimal amount);

    // 에스크로 이력 생성 (예치 시)
    int insertEscrowRecord(@Param("workCaseId") Long workCaseId, @Param("amount") BigDecimal amount, @Param("status") String status);

    // 에스크로 상태 변경 (정산 시)
    int updateEscrowStatus(@Param("workCaseId") Long workCaseId, @Param("status") String status);

    // 1. 비관적 락(FOR UPDATE)을 사용한 잔액 조회
    BigDecimal getAvailableBalanceForUpdate(@Param("userId") Long userId);

    // 2. 멱등성 검증: 이미 처리된 영수증인지 확인
    int countTransactionByIdempotencyKey(@Param("idempotencyKey") String idempotencyKey);

    // 잠금 잔액 조회
    BigDecimal getLockedBalance(Long userId);

    // 3. 유저 ID로 지갑 ID 조회 (원장 기록용)
    Long getWalletIdByUserId(@Param("userId") Long userId);

    // 4. 원장(Ledger) 기록
    int insertWalletTransaction(
            @Param("walletId") Long walletId,
            @Param("workCaseId") Long workCaseId,
            @Param("transactionType") String transactionType,
            @Param("amount") BigDecimal amount,
            @Param("idempotencyKey") String idempotencyKey
    );
}
