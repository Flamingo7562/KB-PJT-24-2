package com.gighub.settlement.mapper;

import com.gighub.wallet.dto.WalletTransactionSnapshot;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SettlementLedgerMapper {

    // 정산 replay: 오래된 트랜잭션 read view 대신 커밋된 최신 원장을 읽는다.
    WalletTransactionSnapshot findByIdempotencyKeyForShare(
            @Param("idempotencyKey") String idempotencyKey);
}
