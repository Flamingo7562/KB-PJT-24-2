package com.gighub.wallet.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/** MyBatis가 &lt;constructor&gt; 매핑으로 생성하므로 no-args 생성자 없이 필드를 final로 고정한다. */
@Getter
@Builder(toBuilder = true)
@AllArgsConstructor
public class FundingOrder { // 조회용
    private final Long id;
    private final Long employerId;
    private final Long linkedAccountId;
    private final Long expectedAmount;
    private final Long transferredAmount;
    private final Long mockBankTransactionId;
    private final String status;
}
