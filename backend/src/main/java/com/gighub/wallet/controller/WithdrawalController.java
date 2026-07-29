package com.gighub.wallet.controller;

import com.gighub.wallet.dto.WithdrawalRequest;
import com.gighub.wallet.service.WithdrawalService;
import com.gighub.wallet.service.command.WithdrawalCommand;
import com.gighub.wallet.service.result.WithdrawalResult;
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
public class WithdrawalController {
    private static final String LOGIN_USER = "LOGIN_USER";

    private final WithdrawalService withdrawalService;

    //Mock 계좌로 출금 (사장과 알바생 모두 사용)
    @PostMapping("/api/wallet/withdrawal-requests")
    public ResponseEntity<Map<String, Object>> withdraw(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody WithdrawalRequest request,
            HttpSession session) {

        Long loginUserId = (Long) session.getAttribute(LOGIN_USER);
        if (loginUserId == null) {
            return ResponseEntity.status(401)
                    .body(Map.of("code", "AUTH_REQUIRED", "message", "로그인이 필요합니다."));
        }

        WithdrawalResult result = withdrawalService.withdraw(WithdrawalCommand.builder()
                .userId(loginUserId)
                .linkedAccountId(request.getBankAccountId())
                .amount(request.getAmount())
                .idempotencyKey(idempotencyKey)
                .build());

        // 잔액은 응답에 포함하지 않는다. 최신 잔액은 GET /api/wallet으로 조회한다.
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("withdrawalRequestId", result.getWithdrawalRequestId());
        data.put("status", result.getStatus());
        data.put("bankTransactionId", result.getBankTransactionId());

        if (result.isReplayed()) {
            return ResponseEntity.ok()
                    .header("Idempotency-Replayed", "true")
                    .body(Map.of("data", data));
        }
        return ResponseEntity.status(201).body(Map.of("data", data));
    }
}
