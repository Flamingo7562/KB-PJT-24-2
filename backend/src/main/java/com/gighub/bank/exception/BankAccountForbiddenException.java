package com.gighub.bank.exception;

import com.gighub.common.exception.ForbiddenException;

/** 타인 소유 여부를 구분해 노출하지 않고 승인된 공통 403으로 변환합니다. */
public class BankAccountForbiddenException extends ForbiddenException {

    public BankAccountForbiddenException(String message) {
        super(message);
    }
}
