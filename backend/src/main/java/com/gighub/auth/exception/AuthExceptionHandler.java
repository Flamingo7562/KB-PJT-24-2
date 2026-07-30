package com.gighub.auth.exception;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * TODO(#116): 공통 전역 예외 처리기가 병합되면 이 클래스는 제거하고 그 계약으로 교체한다.
 *
 * <p>{@code @RestControllerAdvice}는 Controller와 같은 Context(Servlet)에 있어야
 * {@code ExceptionHandlerExceptionResolver}가 인식한다 — 그래서 컴포넌트 스캔이 아니라
 * {@link com.gighub.config.WebMvcConfig}에서 {@code @Bean}으로 명시 등록한다.</p>
 */
@RestControllerAdvice
public class AuthExceptionHandler {

    @ExceptionHandler(AuthException.class)
    public ResponseEntity<Map<String, Object>> handleAuthException(AuthException e) {
        Map<String, Object> body = base(e.getErrorCode().name(), e.getMessage());
        if (e instanceof AuthValidationException) {
            body.put("fieldErrors", toFieldErrorMaps(((AuthValidationException) e).getFieldErrors()));
        }
        return ResponseEntity.status(e.getErrorCode().getHttpStatus()).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleBeanValidation(MethodArgumentNotValidException e) {
        List<FieldErrorItem> fieldErrors = new ArrayList<>();
        for (FieldError fieldError : e.getBindingResult().getFieldErrors()) {
            fieldErrors.add(new FieldErrorItem(fieldError.getField(), String.valueOf(fieldError.getDefaultMessage())));
        }

        Map<String, Object> body = base(AuthErrorCode.VALIDATION_FAILED.name(),
                AuthErrorCode.VALIDATION_FAILED.getDefaultMessage());
        body.put("fieldErrors", toFieldErrorMaps(fieldErrors));
        return ResponseEntity.status(AuthErrorCode.VALIDATION_FAILED.getHttpStatus()).body(body);
    }

    private List<Map<String, String>> toFieldErrorMaps(List<FieldErrorItem> fieldErrors) {
        List<Map<String, String>> mapped = new ArrayList<>();
        for (FieldErrorItem item : fieldErrors) {
            Map<String, String> entry = new LinkedHashMap<>();
            entry.put("field", item.getField());
            entry.put("reason", item.getReason());
            mapped.add(entry);
        }
        return mapped;
    }

    private Map<String, Object> base(String code, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", code);
        body.put("message", message);
        body.put("traceId", UUID.randomUUID().toString());
        return body;
    }
}
