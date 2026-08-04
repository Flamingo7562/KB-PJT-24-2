package com.gighub.bank.exception;

import com.gighub.common.exception.ForbiddenException;

/** 타인 소유 여부를 구분해 노출하지 않고 승인된 공통 403으로 변환합니다. */
public class BankAccountForbiddenException extends ForbiddenException {

    public BankAccountForbiddenException(String message) {
        // 조회 실패와 타인 소유를 같은 문구로 처리해 계좌 존재 여부를 추론할 수 없게 합니다.
        super("계좌에 접근할 수 없습니다.");
    }
}
