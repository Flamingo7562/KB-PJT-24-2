package com.gighub.invitation.exception;

import com.gighub.common.api.ApiErrorCode;
import com.gighub.common.exception.ApiException;
import org.springframework.http.HttpStatus;

/**
 * 만료 시각을 지난 초대 Link 사용을 410으로 반환합니다.
 *
 * <p>초대의 만료 시각은 근무 시작 시각이므로, 만료는 되돌릴 수 없는 종료 상태입니다.</p>
 */
public class InvitationExpiredException extends ApiException {

    private static final String MESSAGE = "초대 링크가 만료되었습니다.";

    public InvitationExpiredException() {
        super(HttpStatus.GONE, ApiErrorCode.INVITATION_EXPIRED, MESSAGE);
    }
}
