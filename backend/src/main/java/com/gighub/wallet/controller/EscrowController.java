package com.gighub.wallet.controller;

import com.gighub.wallet.dto.EscrowLockRequest;
import com.gighub.wallet.dto.EscrowReleaseRequest;
import com.gighub.wallet.service.EscrowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;

@RestController
public class EscrowController {
    private final EscrowService escrowService;

    @Autowired
    public EscrowController(EscrowService escrowService){
        this.escrowService = escrowService;
    }

    // 예치 API(근로자가 계약 수락 시 호출)
    @PostMapping("/api/invites/{token}/accept")
    public ResponseEntity<Map<String, Object>> lockFunds(@PathVariable String token, @RequestHeader("Idempotency-Key") String idempotencyKey, @RequestBody EscrowLockRequest request, HttpSession session){
        // 1. 세션에서 로그인한 유저 ID 확인 (회원가입/로그인 파트에서 'LOGIN_USER' 키로 세션에 저장했다고 가정)
        Long loggedInUserId = (Long) session.getAttribute("LOGIN_USER");

        // 2. 권한 검증: 비로그인 상태이거나, 요청한 사장님 ID와 로그인한 유저 ID가 다르면 차단
        if (loggedInUserId == null || !loggedInUserId.equals(request.getWorkerId())) {
            return ResponseEntity.status(401).body(Map.of(
                    "status", 401,
                    "message", "권한이 없습니다. 알바생 계정으로 로그인해주세요."
            ));
        }

        // 3. 비즈니스 로직 실행 및 자체 예외 처리 (전역 예외 처리 도입 전 임시)
        try {
            request.setIdempotencyKey(idempotencyKey); // 서비스로 넘기기 위해 세팅
            escrowService.lockFunds(request);

            // 성공 응답은 "data" 객체로 감싸기
            return ResponseEntity.ok(Map.of("data", Map.of(
                    "message", "에스크로 예치가 성공적으로 완료되었습니다."
            )));
        } catch (RuntimeException e) {
            return ResponseEntity.status(409).body(Map.of(
                    "code", "INSUFFICIENT_WALLET_BALANCE",
                    "message", e.getMessage()
            ));
        }
    }

    // 정산 API (수동 정산 또는 자동 정산 시 호출)
    @PostMapping("/api/work-cases/{workCaseId}/settlement/approve")
    public ResponseEntity<Map<String, Object>> releaseFunds(@PathVariable Long workCaseId, @RequestHeader("Idempotency-Key") String idempotencyKey, HttpSession session){
        // 1. 세션에서 로그인한 유저 ID 확인
        Long loggedInUserId = (Long) session.getAttribute("LOGIN_USER");

        // 2. 권한 검증 (정산은 사장님 권한)
        if (loggedInUserId == null) {
            return ResponseEntity.status(401).body(Map.of(
                    "code", "AUTH_REQUIRED",
                    "message", "권한이 없습니다. 올바른 계정으로 로그인해주세요."
            ));
        }

        // 3. 비즈니스 로직 실행 및 자체 예외 처리
        try {
            escrowService.releaseFunds(workCaseId, loggedInUserId, idempotencyKey);

            return ResponseEntity.ok(Map.of("data", Map.of(
                    "status", "COMPLETED"
            )));
        } catch (RuntimeException e) {
            return ResponseEntity.status(409).body(Map.of(
                    "code", "SETTLEMENT_ERROR", "message", e.getMessage()
            ));
        }
    }
}
