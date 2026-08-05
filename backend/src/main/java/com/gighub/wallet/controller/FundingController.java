package com.gighub.wallet.controller;

import com.gighub.auth.security.AuthPrincipals;
import com.gighub.common.api.ApiResponse;
import com.gighub.wallet.service.FundingService;
import com.gighub.wallet.service.command.FundingCommand;
import com.gighub.wallet.service.result.FundingResult;
import com.gighub.wallet.dto.FundingOrderResponse;
import com.gighub.wallet.dto.FundingRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequiredArgsConstructor
public class FundingController {

    private final FundingService fundingService;

    // Mock 게좌에서 지갑으로 충전
    @PostMapping("/api/wallet/funding-orders")
    public ResponseEntity<ApiResponse<FundingOrderResponse>> fund(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody FundingRequest request,
            Authentication authentication) {
        Long loginUserId = AuthPrincipals.resolve(authentication).getUserId();

        FundingResult result = fundingService.fund(FundingCommand.builder()
                .employerId(loginUserId)
                .bankCode(request.getBankCode())
                .accountNo(request.getAccountNo())
                .pin(request.getPin())
                .amount(request.getAmount())
                .idempotencyKey(idempotencyKey)
                .build());

        ApiResponse<FundingOrderResponse> body =
                ApiResponse.of(FundingOrderResponse.from(result));

        // 최초 201, 멱등 재전송은 200 + Idempotency-Replayed
        if (result.isReplayed()) {
            return ResponseEntity.ok()
                    .header("Idempotency-Replayed", "true")
                    .body(body);
        }
        return ResponseEntity.status(201).body(body);
    }
}
