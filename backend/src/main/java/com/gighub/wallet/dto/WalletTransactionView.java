package com.gighub.wallet.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 지갑 거래 내역 조회 결과.
 *
 * <p>MyBatis가 &lt;constructor&gt; 매핑으로 생성하므로 no-args 생성자 없이 필드를 final로 고정한다.</p>
 */
@Getter
@Builder
@AllArgsConstructor
public class WalletTransactionView {
    private final Long transactionId;
    private final String type;
    private final Long amount;
    private final Long availableBefore;
    private final Long availableAfter;
    private final Long lockedBefore;
    private final Long lockedAfter;
    private final Long workCaseId;
    private final String workTitle;
    private final String workplaceName;
    private final LocalDateTime createdAt;
}
