package com.gighub.wallet.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// 지갑 거래 내역 조회 결과
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletTransactionView {
    private Long transactionId;
    private String type;
    private Long amount;
    private Long availableAfter;
    private Long lockedAfter;
    private Long workCaseId;
    private String workTitle;
    private String workplaceName;
    private LocalDateTime createdAt;
}
