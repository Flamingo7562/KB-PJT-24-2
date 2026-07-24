package com.gighub.wallet.service;

import com.gighub.wallet.service.command.FundingCommand;
import com.gighub.wallet.service.result.FundingResult;

public interface FundingService {
    // Mock 계좌에서 지갑으로 충전, 같은 멱등 키의 요청은 저장된 결과를 재응답
    FundingResult fund(FundingCommand command);
}
