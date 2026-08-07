package com.gighub.settlement.controller;

import com.gighub.auth.security.AuthPrincipals;
import com.gighub.common.api.ApiResponse;
import com.gighub.settlement.dto.SettlementApproveResponse;
import com.gighub.settlement.service.SettlementService;
import com.gighub.settlement.service.command.SettlementApproveCommand;
import com.gighub.settlement.service.result.SettlementResult;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/**
 * 정산 승인을 제공하는 Controller입니다.
 *
 * <p>이전에는 구형 예치 Endpoint와 한 Controller에 있었습니다. 예치는 초대 수락 Aggregate로
 * 옮겨졌고 남은 것은 정산뿐이라 정산 도메인으로 자리를 옮겼습니다.</p>
 */
@RestController
@RequiredArgsConstructor
public class SettlementController {

    private final SettlementService settlementService;

    /** 정산 API (사장님 수동 승인 또는 향후 자동 정산 작업에서 호출). */
    @PostMapping("/api/work-cases/{workCaseId}/settlement/approve")
    public ResponseEntity<ApiResponse<SettlementApproveResponse>> approveSettlement(
            @PathVariable Long workCaseId,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            Authentication authentication) {

        Long loginUserId = AuthPrincipals.resolve(authentication).getUserId();

        SettlementResult result =
                settlementService.approve(SettlementApproveCommand.builder()
                        .workCaseId(workCaseId)
                        .approverUserId(loginUserId)
                        .idempotencyKey(idempotencyKey)
                        .build());

        return ResponseEntity.ok(ApiResponse.of(SettlementApproveResponse.from(result)));
    }
}
