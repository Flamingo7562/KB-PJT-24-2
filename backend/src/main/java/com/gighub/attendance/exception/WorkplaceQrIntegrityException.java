package com.gighub.attendance.exception;

/**
 * {@code ACTIVE} 사업장에 활성 고정 QR이 없는 상태를 보고합니다.
 *
 * <p>{@code ApiException}을 상속하지 않습니다. 이건 사용자가 고칠 수 있는 요청 오류가 아니라
 * 저장소 불변식 위반이므로, 공통 Handler의 {@code 500 INTERNAL_ERROR}와 {@code traceId}
 * 무결성 로그로 흘러가야 합니다.</p>
 */
public class WorkplaceQrIntegrityException extends RuntimeException {

    public WorkplaceQrIntegrityException(String message) {
        super(message);
    }
}
