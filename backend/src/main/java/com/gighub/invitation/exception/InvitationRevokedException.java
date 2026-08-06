package com.gighub.invitation.exception;

import com.gighub.common.api.ApiErrorCode;
import com.gighub.common.exception.ApiException;
import org.springframework.http.HttpStatus;

/**
 * 재발급이나 근무 조건 변경으로 철회된 초대 Link 사용을 409로 반환합니다.
 */
public class InvitationRevokedException extends ApiException {

    private static final String MESSAGE = "철회된 초대 링크입니다.";

    public InvitationRevokedException() {
        super(HttpStatus.CONFLICT, ApiErrorCode.INVITATION_REVOKED, MESSAGE);
    }
}
