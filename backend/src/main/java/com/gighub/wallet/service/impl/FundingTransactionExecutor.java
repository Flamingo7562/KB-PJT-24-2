package com.gighub.wallet.service.impl;

import com.gighub.wallet.service.result.FundingResult;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.function.Supplier;

@Component
public class FundingTransactionExecutor {

    /**
     * 각 충전 시도를 독립 트랜잭션으로 실행한다.
     *
     * <p>잠금 충돌을 재시도할 때 이미 rollback된 트랜잭션을 재사용하지 않기 위한 경계다.</p>
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public FundingResult execute(Supplier<FundingResult> attempt) {
        return attempt.get();
    }
}
