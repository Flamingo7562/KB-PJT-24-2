package com.gighub.common.exception;

import com.gighub.bank.exception.BankAccountForbiddenException;
import com.gighub.bank.exception.InsufficientBankBalanceException;
import com.gighub.bank.exception.InvalidBankAccountStateException;
import com.gighub.wallet.exception.EscrowAccessDeniedException;
import com.gighub.wallet.exception.IdempotencyKeyReusedException;
import com.gighub.wallet.exception.InsufficientAvailableBalanceException;
import com.gighub.wallet.exception.InsufficientWalletBalanceException;
import com.gighub.wallet.exception.InvalidEscrowStateException;
import com.gighub.wallet.exception.InvalidFundingRequestException;
import com.gighub.wallet.exception.InvalidIdempotencyKeyException;
import com.gighub.wallet.exception.InvalidWalletStateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestControllerAdvice
public class CommonExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(CommonExceptionHandler.class);

    /** 403 - 근무 건 에스크로 접근 권한 없음 */
    @ExceptionHandler(EscrowAccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleEscrowAccessDenied(
            EscrowAccessDeniedException e) {
        return body(403, "WORK_CASE_FORBIDDEN", e.getMessage());
    }

    /** 403 - 타인 소유 계좌 접근 */
    @ExceptionHandler(BankAccountForbiddenException.class)
    public ResponseEntity<Map<String, Object>> handleBankAccountForbidden(
            BankAccountForbiddenException e) {
        return body(403, "BANK_ACCOUNT_FORBIDDEN", e.getMessage());
    }

    /** 409 - 연결 계좌 가용액 부족 (충전) */
    @ExceptionHandler(InsufficientBankBalanceException.class)
    public ResponseEntity<Map<String, Object>> handleInsufficientBank(
            InsufficientBankBalanceException e) {
        return body(409, "INSUFFICIENT_BANK_BALANCE", e.getMessage());
    }

    /** 409 - 지갑 가용액 부족 (출금) */
    @ExceptionHandler(InsufficientAvailableBalanceException.class)
    public ResponseEntity<Map<String, Object>> handleInsufficientAvailable(
            InsufficientAvailableBalanceException e) {
        return body(409, "INSUFFICIENT_AVAILABLE_BALANCE", e.getMessage());
    }

    /** 409 - 지갑 잔액 부족 (에스크로 예치) */
    @ExceptionHandler(InsufficientWalletBalanceException.class)
    public ResponseEntity<Map<String, Object>> handleInsufficientWallet(
            InsufficientWalletBalanceException e) {
        return body(409, "INSUFFICIENT_WALLET_BALANCE", e.getMessage());
    }

    /** 409 - 에스크로 상태 충돌 (이미 예치/정산됨, 전이 불가 등) */
    @ExceptionHandler(InvalidEscrowStateException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidEscrowState(
            InvalidEscrowStateException e) {
        return body(409, "INVALID_ESCROW_STATE", e.getMessage());
    }

    /** 409 - 지갑·계좌 상태 충돌 */
    @ExceptionHandler({InvalidWalletStateException.class, InvalidBankAccountStateException.class})
    public ResponseEntity<Map<String, Object>> handleStateConflict(RuntimeException e) {
        return body(409, "STATE_CONFLICT", e.getMessage());
    }

    /** 409 - 같은 멱등 키에 다른 요청 본문 */
    @ExceptionHandler(IdempotencyKeyReusedException.class)
    public ResponseEntity<Map<String, Object>> handleIdempotencyReused(
            IdempotencyKeyReusedException e) {
        return body(409, "IDEMPOTENCY_KEY_REUSED", e.getMessage());
    }

    /** 400 - 멱등 키 형식 오류 */
    @ExceptionHandler(InvalidIdempotencyKeyException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidIdempotencyKey(
            InvalidIdempotencyKeyException e) {
        return body(400, "INVALID_IDEMPOTENCY_KEY", e.getMessage());
    }

    /** 400 - 충전 요청 필수 값 또는 금액 오류 */
    @ExceptionHandler(InvalidFundingRequestException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidFundingRequest(
            InvalidFundingRequestException e) {
        return body(400, "VALIDATION_FAILED", e.getMessage());
    }

    /** 400 - Idempotency-Key 헤더 누락 */
    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<Map<String, Object>> handleMissingHeader(MissingRequestHeaderException e) {
        if ("Idempotency-Key".equalsIgnoreCase(e.getHeaderName())) {
            return body(400, "IDEMPOTENCY_KEY_REQUIRED", "Idempotency-Key 헤더가 필요합니다.");
        }
        return body(400, "VALIDATION_FAILED", e.getMessage());
    }

    /** 400 - @Valid 검증 실패. fieldErrors를 함께 반환한다 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException e) {
        List<Map<String, String>> fieldErrors = new ArrayList<>();
        e.getBindingResult().getFieldErrors().forEach(fe -> {
            Map<String, String> item = new LinkedHashMap<>();
            item.put("field", fe.getField());
            item.put("reason", String.valueOf(fe.getDefaultMessage()));
            fieldErrors.add(item);
        });

        Map<String, Object> payload = base("VALIDATION_FAILED", "입력값을 확인해 주세요.");
        payload.put("fieldErrors", fieldErrors);
        return ResponseEntity.status(400).body(payload);
    }

    /** 500 - 안전망. 내부 정보는 숨기고 traceId로 로그를 추적한다 */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntime(RuntimeException e) {
        Map<String, Object> payload = base("INTERNAL_ERROR", "처리 중 오류가 발생했습니다.");
        log.error("처리되지 않은 예외 traceId={}", payload.get("traceId"), e);
        return ResponseEntity.status(500).body(payload);
    }

    private ResponseEntity<Map<String, Object>> body(int status, String code, String message) {
        return ResponseEntity.status(status).body(base(code, message));
    }

    private Map<String, Object> base(String code, String message) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("code", code);
        payload.put("message", String.valueOf(message));
        payload.put("traceId", UUID.randomUUID().toString());
        return payload;
    }

}
