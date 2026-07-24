package com.gighub.wallet.mapper.param;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class FundingOrderParam { // INSERT용, 가변
    private Long id;
    private Long employerId;
    private Long linkedAccountId;
    private Long expectedAmount;
    private String idempotencyKey;
}
