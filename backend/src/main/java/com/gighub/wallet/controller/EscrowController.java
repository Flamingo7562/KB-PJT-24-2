package com.gighub.wallet.controller;

import com.gighub.auth.security.AuthPrincipals;
import com.gighub.common.api.ApiResponse;
import com.gighub.common.exception.ForbiddenException;
import com.gighub.settlement.dto.SettlementApproveResponse;
import com.gighub.settlement.service.SettlementService;
import com.gighub.settlement.service.command.SettlementApproveCommand;
import com.gighub.settlement.service.result.SettlementResult;
import com.gighub.wallet.dto.EscrowHoldRequest;
import com.gighub.wallet.dto.EscrowHoldResponse;
import com.gighub.wallet.service.EscrowService;
import com.gighub.wallet.service.command.EscrowHoldCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequiredArgsConstructor
public class EscrowController {

    private final EscrowService escrowService;
    private final SettlementService settlementService;

     //예치 API (근로자가 초대를 수락할 때 호출).
     //TODO(후속): token에서 employer/worker/workCase/amount를 서버가 도출하도록 교체.
    @PostMapping("/api/invites/{token}/accept")
    public ResponseEntity<ApiResponse<EscrowHoldResponse>> hold(
            @PathVariable String token,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody EscrowHoldRequest request,
            Authentication authentication) {

        Long loginUserId = AuthPrincipals.resolve(authentication).getUserId();
        // 예치는 근로자가 수락하는 시점이므로 workerId와 대조한다.
        if (!loginUserId.equals(request.getWorkerId())) {
            throw new ForbiddenException("알바생 본인 계정으로만 수락할 수 있습니다.");
        }

        escrowService.hold(EscrowHoldCommand.builder()
                .employerId(request.getEmployerId())
                .workerId(request.getWorkerId())
                .workCaseId(request.getWorkCaseId())
                .amount(request.getAmount())
                .idempotencyKey(idempotencyKey)
                .build());

        return ResponseEntity.ok(ApiResponse.of(EscrowHoldResponse.of("에스크로 예치 완료")));
    }

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
