package com.gighub.wallet.exception;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Map;

@RestControllerAdvice
public class WalletExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(WalletExceptionHandler.class);

    // 잔액부족
    @ExceptionHandler(InsufficientWalletBalanceException.class)
    public ResponseEntity<Map<String, Object>> handleInsufficientBalance(
            InsufficientWalletBalanceException e) {
        return body(409, "INSUFFICIENT_WALLET_BALANCE", e.getMessage());
    }

    // 이미 배치/정산된건, held가 아닌 에스크로, 배정되지 않은 알바생, 존재하지 않는 근무 건 등
    @ExceptionHandler(InvalidEscrowStateException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidEscrowState(
            InvalidEscrowStateException e) {
        return body(409, "INVALID_ESCROW_STATE", e.getMessage());
    }

    /** 멱등 키 또는 work_case UNIQUE 충돌 */
    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<Map<String, Object>> handleDuplicate(DuplicateKeyException e) {
        return body(409, "DUPLICATE_REQUEST", "이미 처리된 요청입니다.");
    }

    // Idempotency-Key 헤더 누락, 또는 @Valid 검증 실패
    @ExceptionHandler({MissingRequestHeaderException.class, MethodArgumentNotValidException.class})
    public ResponseEntity<Map<String, Object>> handleBadRequest(Exception e) {
        return body(400, "INVALID_REQUEST", e.getMessage());
    }

    /** 안전망. 비즈니스 예외가 아닌 버그를 409로 위장하지 않는다. */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntime(RuntimeException e) {
        log.error("처리되지 않은 예외", e);
        return body(500, "INTERNAL_ERROR", "처리 중 오류가 발생했습니다.");
    }

    // 공통 응답 포멧
    private ResponseEntity<Map<String, Object>> body(int status, String code, String message) {
        return ResponseEntity.status(status)
                .body(Map.of("code", code, "message", String.valueOf(message)));
    }
}