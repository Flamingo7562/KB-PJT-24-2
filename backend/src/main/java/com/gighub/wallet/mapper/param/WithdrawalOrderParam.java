package com.gighub.wallet.mapper.param;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

// withdrawal_requests INSERT 파라미터. id는 useGeneratedKeys로 채움
@Getter
@Builder
public class WithdrawalOrderParam {
    // useGeneratedKeys가 insert 이후 이 필드에 직접 채우므로 setter를 유지한다.
    @Setter
    private Long id;
    private Long userId;
    private Long walletId;
    private Long linkedAccountId;
    private Long amount;
    private String idempotencyKey;
}
