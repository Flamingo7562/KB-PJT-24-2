package com.gighub.common.exception;

import com.gighub.common.api.ApiErrorCode;
import com.gighub.common.api.ApiErrorResponse;
import com.gighub.common.api.ApiFieldError;
import com.gighub.common.trace.TraceIds;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class CommonExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(CommonExceptionHandler.class);
    private static final String VALIDATION_MESSAGE = "입력값을 확인해 주세요.";
    private static final String INTERNAL_ERROR_MESSAGE =
            "서버 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.";

    /** 구체적인 도메인 타입 대신 승인된 공통 예외 계약 하나만 처리합니다. */
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiErrorResponse> handleApiException(
            ApiException exception,
            HttpServletRequest request) {
        return body(
                exception.getStatus(),
                exception.getCode(),
                exception.getMessage(),
                exception.getFieldErrors(),
                request
        );
    }

    @ExceptionHandler({
        MissingRequestHeaderException.class,
        MissingServletRequestParameterException.class
    })
    public ResponseEntity<ApiErrorResponse> handleMissingRequestValue(
            Exception exception,
            HttpServletRequest request) {
        return body(
                HttpStatus.BAD_REQUEST,
                ApiErrorCode.VALIDATION_ERROR,
                "필수 요청 값이 누락되었습니다.",
                null,
                request
        );
    }

    /** Bean Validation 필드 오류만 선택 필드로 포함하고 Spring 내부 객체는 노출하지 않습니다. */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(
            BindException exception,
            HttpServletRequest request) {
        List<ApiFieldError> fieldErrors = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> new ApiFieldError(
                        error.getField(),
                        String.valueOf(error.getDefaultMessage())
                ))
                .toList();

        return body(
                HttpStatus.BAD_REQUEST,
                ApiErrorCode.VALIDATION_ERROR,
                VALIDATION_MESSAGE,
                fieldErrors,
                request
        );
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException exception,
            HttpServletRequest request) {
        return body(
                HttpStatus.BAD_REQUEST,
                ApiErrorCode.VALIDATION_ERROR,
                "요청 파라미터 형식이 올바르지 않습니다.",
                null,
                request
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleNotReadable(
            HttpMessageNotReadableException exception,
            HttpServletRequest request) {
        return body(
                HttpStatus.BAD_REQUEST,
                ApiErrorCode.VALIDATION_ERROR,
                "요청 본문 형식이 올바르지 않습니다.",
                null,
                request
        );
    }

    /** 예상 밖의 오류는 내부 원인을 숨기고 응답과 서버 로그를 같은 traceId로 연결합니다. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(
            Exception exception,
            HttpServletRequest request) {
        String traceId = TraceIds.getOrCreate(request);
        log.error("처리되지 않은 서버 오류 traceId={}", traceId, exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                new ApiErrorResponse(
                        ApiErrorCode.INTERNAL_ERROR,
                        INTERNAL_ERROR_MESSAGE,
                        traceId,
                        null
                )
        );
    }

    private ResponseEntity<ApiErrorResponse> body(
            HttpStatus status,
            ApiErrorCode code,
            String message,
            List<ApiFieldError> fieldErrors,
            HttpServletRequest request) {
        ApiErrorResponse response = new ApiErrorResponse(
                code,
                message,
                TraceIds.getOrCreate(request),
                fieldErrors
        );
        return ResponseEntity.status(status).body(response);
    }
}
