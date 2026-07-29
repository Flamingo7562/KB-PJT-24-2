package com.gighub.wallet.service;

import com.gighub.wallet.service.command.WithdrawalCommand;
import com.gighub.wallet.service.result.WithdrawalResult;

public interface WithdrawalService {
    // 지갑 가용 잔액을 Mock 계좌로 출금, 같은 멱등 키의 동일 요청은 저장된 결과를 재응답한다.
    WithdrawalResult withdraw(WithdrawalCommand command);
}
