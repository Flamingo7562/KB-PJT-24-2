package com.gighub.invitation.exception;

import com.gighub.common.api.ApiErrorCode;
import com.gighub.common.exception.ApiException;
import org.springframework.http.HttpStatus;

/**
 * 초대가 기대한 조건 Version과 현재 근무 조건이 어긋난 경우를 409로 반환합니다.
 *
 * <p>이 상태에서 이전 Snapshot을 보여 주면 WORKER가 더 이상 유효하지 않은 조건으로 근무를
 * 확정할 수 있으므로, 조건을 노출하지 않고 확정을 막습니다.</p>
 */
public class InvitationTermsChangedException extends ApiException {

    private static final String MESSAGE = "근무 조건이 변경되어 초대를 사용할 수 없습니다.";

    public InvitationTermsChangedException() {
        super(HttpStatus.CONFLICT, ApiErrorCode.INVITATION_TERMS_CHANGED, MESSAGE);
    }
}
