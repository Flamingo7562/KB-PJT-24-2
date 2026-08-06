package com.gighub.bank.exception;

import com.gighub.common.exception.ForbiddenException;

/** 계좌 미존재·비활성·PIN 불일치를 구분해 노출하지 않고 승인된 공통 403으로 변환합니다. */
public class BankAccountForbiddenException extends ForbiddenException {

    public BankAccountForbiddenException(String message) {
        // 세 실패 사유를 같은 문구로 처리해 계좌 상태나 존재 여부를 추론할 수 없게 합니다(DEC-BANK-ERROR-CATALOG).
        super("계좌를 사용할 수 없습니다.");
    }
}
