package com.gighub.wallet.controller;

import com.gighub.common.api.ApiResponse;
import com.gighub.common.exception.AuthRequiredException;
import com.gighub.wallet.dto.WithdrawalRequest;
import com.gighub.wallet.dto.WithdrawalRequestResponse;
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

@RestController
@RequiredArgsConstructor
public class WithdrawalController {
    private static final String LOGIN_USER = "LOGIN_USER";

    private final WithdrawalService withdrawalService;

    //Mock 계좌로 출금 (사장과 알바생 모두 사용)
    @PostMapping("/api/wallet/withdrawal-requests")
    public ResponseEntity<ApiResponse<WithdrawalRequestResponse>> withdraw(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody WithdrawalRequest request,
            HttpSession session) {

        Long loginUserId = (Long) session.getAttribute(LOGIN_USER);
        if (loginUserId == null) {
            throw new AuthRequiredException("로그인이 필요합니다.");
        }

        WithdrawalResult result = withdrawalService.withdraw(WithdrawalCommand.builder()
                .userId(loginUserId)
                .linkedAccountId(request.getBankAccountId())
                .amount(request.getAmount())
                .idempotencyKey(idempotencyKey)
                .build());

        // 잔액은 응답에 포함하지 않는다. 최신 잔액은 GET /api/wallet으로 조회한다.
        ApiResponse<WithdrawalRequestResponse> body =
                ApiResponse.of(WithdrawalRequestResponse.from(result));

        if (result.isReplayed()) {
            return ResponseEntity.ok()
                    .header("Idempotency-Replayed", "true")
                    .body(body);
        }
        return ResponseEntity.status(201).body(body);
    }
}
