package com.gighub.workplace.service;

import com.gighub.auth.security.AuthPrincipal;
import com.gighub.workplace.service.command.WorkplaceCreateCommand;

/** 사업장 등록과 소유 사업장 조회의 승인 규칙을 적용합니다. */
public interface WorkplaceService {

    /**
     * 인증 OWNER의 사업장을 등록하고 생성된 식별자를 반환합니다.
     *
     * @param principal 소유자를 결정하는 인증 Principal. 요청 Body는 소유자를 정할 수 없습니다.
     * @param command   검증을 통과한 등록 입력
     * @return 생성된 사업장 식별자
     */
    Long create(AuthPrincipal principal, WorkplaceCreateCommand command);
}
