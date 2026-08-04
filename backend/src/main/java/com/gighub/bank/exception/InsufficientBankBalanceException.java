package com.gighub.bank.exception;

import com.gighub.common.api.ApiErrorCode;
import com.gighub.common.exception.ApiException;
import org.springframework.http.HttpStatus;

public class InsufficientBankBalanceException extends ApiException {

    public InsufficientBankBalanceException(String message) {
        // 세부 금융 오류 카탈로그가 열려 있으므로 현재 승인된 공통 충돌 코드만 공개합니다.
        super(HttpStatus.CONFLICT, ApiErrorCode.CONFLICT, message);
    }
}
