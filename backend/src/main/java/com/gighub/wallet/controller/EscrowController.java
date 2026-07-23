package com.gighub.wallet.controller;

import com.gighub.wallet.dto.EscrowHoldRequest;
import com.gighub.wallet.service.EscrowService;
import com.gighub.wallet.service.command.EscrowHoldCommand;
import com.gighub.wallet.service.command.EscrowReleaseCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class EscrowController {

    private static final String LOGIN_USER = "LOGIN_USER";

    private final EscrowService escrowService;

     //예치 API (근로자가 초대를 수락할 때 호출).
     //TODO(후속): token에서 employer/worker/workCase/amount를 서버가 도출하도록 교체.
    @PostMapping("/api/invites/{token}/accept")
    public ResponseEntity<Map<String, Object>> hold(
            @PathVariable String token,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody EscrowHoldRequest request,
            HttpSession session) {

        Long loginUserId = (Long) session.getAttribute(LOGIN_USER);
        if (loginUserId == null) {
            return ResponseEntity.status(401)
                    .body(Map.of("code", "AUTH_REQUIRED", "message", "로그인이 필요합니다."));
        }
        // 예치는 근로자가 수락하는 시점이므로 workerId와 대조한다.
        if (!loginUserId.equals(request.getWorkerId())) {
            return ResponseEntity.status(403)
                    .body(Map.of("code", "FORBIDDEN", "message", "알바생 본인 계정으로만 수락할 수 있습니다."));
        }

        escrowService.hold(EscrowHoldCommand.builder()
                .employerId(request.getEmployerId())
                .workerId(request.getWorkerId())
                .workCaseId(request.getWorkCaseId())
                .amount(request.getAmount())
                .idempotencyKey(idempotencyKey)
                .build());

        return ResponseEntity.ok(Map.of("data", Map.of("message", "에스크로 예치 완료")));
    }

    /** 정산 API (사장님 수동 승인 또는 향후 자동 정산 작업에서 호출). */
    @PostMapping("/api/work-cases/{workCaseId}/settlement/approve")
    public ResponseEntity<Map<String, Object>> release(
            @PathVariable Long workCaseId,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            HttpSession session) {

        Long loginUserId = (Long) session.getAttribute(LOGIN_USER);
        if (loginUserId == null) {
            return ResponseEntity.status(401)
                    .body(Map.of("code", "AUTH_REQUIRED", "message", "로그인이 필요합니다."));
        }

        // 수령자와 금액은 서비스가 DB에서 도출한다.
        escrowService.release(EscrowReleaseCommand.builder()
                .workCaseId(workCaseId)
                .employerId(loginUserId)
                .idempotencyKey(idempotencyKey)
                .build());

        return ResponseEntity.ok(Map.of("data", Map.of("status", "COMPLETED")));
    }
}