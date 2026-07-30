package com.gighub.wallet.mapper.param;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

// withdrawal_requests INSERT 파라미터. id는 useGeneratedKeys로 채움
@Getter
@Setter
@Builder
public class WithdrawalOrderParam {
    private Long id;
    private Long userId;
    private Long walletId;
    private Long linkedAccountId;
    private Long amount;
    private String idempotencyKey;
}
