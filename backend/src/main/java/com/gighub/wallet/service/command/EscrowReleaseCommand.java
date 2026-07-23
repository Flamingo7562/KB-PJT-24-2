package com.gighub.wallet.service.command;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class EscrowReleaseCommand {
    Long employerId; // 로그인 사용자
    Long workCaseId;
    String idempotencyKey;
}
