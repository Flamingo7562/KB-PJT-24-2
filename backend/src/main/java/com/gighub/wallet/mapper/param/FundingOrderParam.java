package com.gighub.wallet.mapper.param;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Builder
public class FundingOrderParam { // INSERT 파라미터
    // useGeneratedKeys가 insert 이후 이 필드에 직접 채우므로 setter를 유지한다.
    @Setter
    private Long id;
    private Long employerId;
    private Long linkedAccountId;
    private Long expectedAmount;
    private String idempotencyKey;
}
