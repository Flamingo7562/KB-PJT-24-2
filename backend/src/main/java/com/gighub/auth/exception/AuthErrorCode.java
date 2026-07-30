package com.gighub.auth.exception;

import org.springframework.http.HttpStatus;

/**
 * 인증 관련 오류 코드. TODO(#116): 공통 ErrorCode 계약이 dev에 병합되면
 * 이 enum은 그 인터페이스를 구현하도록 교체하고, {@link AuthException}과
 * {@code AuthJsonResponseWriter}도 공통 응답 DTO를 사용하도록 바꾼다.
 */
public enum AuthErrorCode {

    AUTH_REQUIRED(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다."),
    SESSION_EXPIRED(HttpStatus.UNAUTHORIZED, "세션이 만료되었습니다. 다시 로그인해주세요."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "아이디 또는 비밀번호가 올바르지 않습니다."),
    ROLE_MISMATCH(HttpStatus.FORBIDDEN, "선택한 역할과 계정 역할이 일치하지 않습니다."),
    CSRF_TOKEN_INVALID(HttpStatus.FORBIDDEN, "요청을 검증할 수 없습니다. 새로고침 후 다시 시도해주세요."),
    RESOURCE_FORBIDDEN(HttpStatus.FORBIDDEN, "요청한 리소스에 접근할 권한이 없습니다."),
    VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "입력값을 확인해주세요."),
    LOGIN_ID_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 사용 중인 아이디입니다."),
    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다.");

    private final HttpStatus httpStatus;
    private final String defaultMessage;

    AuthErrorCode(HttpStatus httpStatus, String defaultMessage) {
        this.httpStatus = httpStatus;
        this.defaultMessage = defaultMessage;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }
}
