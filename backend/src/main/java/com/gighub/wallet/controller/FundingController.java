package com.gighub.wallet.controller;

import com.gighub.wallet.service.FundingService;
import com.gighub.wallet.service.command.FundingCommand;
import com.gighub.wallet.service.result.FundingResult;
import com.gighub.wallet.dto.FundingRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class FundingController {

    private static final String LOGIN_USER = "LOGIN_USER";

    private final FundingService fundingService;

    // Mock 게좌에서 지갑으로 충전
    @PostMapping("/api/wallet/funding-orders")
    public ResponseEntity<Map<String, Object>> fund(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody FundingRequest request,
            HttpSession session) {
        Long loginUserId = (Long) session.getAttribute(LOGIN_USER);
        if (loginUserId == null) {
            return ResponseEntity.status(401)
                    .body(Map.of("code", "AUTH_REQUIRED", "message", "로그인이 필요합니다."));
        }

        FundingResult result = fundingService.fund(FundingCommand.builder()
                .employerId(loginUserId)
                .linkedAccountId(request.getBankAccountId())
                .amount(request.getAmount())
                .idempotencyKey(idempotencyKey)
                .build());

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("fundingOrderId", result.getFundingOrderId());
        data.put("status", result.getStatus());
        data.put("bankTransactionId", result.getBankTransactionId());
        data.put("availableBalance", result.getAvailableBalance());
        data.put("lockedBalance", result.getLockedBalance());

        // 최초 201, 멱등 재전송은 200 + Idempotency-Replayed
        if (result.isReplayed()) {
            return ResponseEntity.ok()
                    .header("Idempotency-Replayed", "true")
                    .body(Map.of("data", data));
        }
        return ResponseEntity.status(201).body(Map.of("data", data));
    }
}
