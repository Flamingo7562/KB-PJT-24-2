package com.gighub.invitation.exception;

import com.gighub.common.api.ApiErrorCode;
import com.gighub.common.exception.ApiException;
import org.springframework.http.HttpStatus;

/**
 * 이미 수락되어 당사자가 정해진 초대 Link 사용을 409로 반환합니다.
 *
 * <p>초대는 첫 수락 성공자가 당사자가 되므로 이후 접근자는 수락자 정보를 알 수 없어야
 * 합니다. 메시지에 수락자나 근무 식별자를 담지 않습니다.</p>
 */
public class InvitationAlreadyAcceptedException extends ApiException {

    private static final String MESSAGE = "이미 수락된 초대 링크입니다.";

    public InvitationAlreadyAcceptedException() {
        super(HttpStatus.CONFLICT, ApiErrorCode.INVITATION_ALREADY_ACCEPTED, MESSAGE);
    }
}
