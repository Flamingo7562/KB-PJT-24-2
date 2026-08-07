package com.gighub.invitation.exception;

import com.gighub.common.api.ApiErrorCode;
import com.gighub.common.exception.ApiException;
import org.springframework.http.HttpStatus;

/**
 * Token 형식 오류와 미존재 초대를 구분 없이 404로 반환합니다.
 *
 * <p>두 경우의 응답이 달라지면 Token을 대입해 보는 쪽에서 어떤 값이 실재하는지 알아낼 수
 * 있으므로 상태·코드·메시지를 하나로 맞춥니다.</p>
 */
public class InvitationNotFoundException extends ApiException {

    private static final String MESSAGE = "초대 링크를 찾을 수 없습니다.";

    public InvitationNotFoundException() {
        super(HttpStatus.NOT_FOUND, ApiErrorCode.RESOURCE_NOT_FOUND, MESSAGE);
    }
}
