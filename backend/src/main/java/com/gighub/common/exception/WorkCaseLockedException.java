package com.gighub.common.exception;

import com.gighub.common.api.ApiErrorCode;
import org.springframework.http.HttpStatus;

/** DRAFT가 아니거나 계약·에스크로가 있어 수정·삭제·초대 발급을 거부하는 근무 Case입니다. */
public class WorkCaseLockedException extends ApiException {

    public WorkCaseLockedException(String message) {
        super(HttpStatus.CONFLICT, ApiErrorCode.WORK_CASE_LOCKED, message);
    }
}
