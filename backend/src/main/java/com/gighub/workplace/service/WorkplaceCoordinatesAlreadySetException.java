package com.gighub.workplace.service;

import com.gighub.common.api.ApiErrorCode;
import com.gighub.common.exception.ApiException;
import org.springframework.http.HttpStatus;

/** 이미 확정된 사업장 좌표를 다른 값으로 바꾸려는 요청입니다. */
public class WorkplaceCoordinatesAlreadySetException extends ApiException {

    public WorkplaceCoordinatesAlreadySetException() {
        super(
                HttpStatus.CONFLICT,
                ApiErrorCode.WORKPLACE_COORDINATES_ALREADY_SET,
                "사업장 위치가 이미 확정되어 변경할 수 없습니다.");
    }
}
