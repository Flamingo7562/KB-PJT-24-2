package com.gighub.work.service;

import com.gighub.auth.security.AuthPrincipal;
import com.gighub.work.service.command.WorkCaseCreateCommand;
import com.gighub.work.service.command.WorkCaseUpdateCommand;

/** OWNER 근무 {@code DRAFT} 생성·조건 수정·삭제의 승인 규칙을 적용합니다. */
public interface WorkCaseService {

    /**
     * 인증 OWNER가 소유한 {@code ACTIVE} 사업장에 근무 {@code DRAFT}를 등록합니다.
     *
     * @param principal 소유권을 결정하는 인증 Principal. 요청 Body는 소유자를 정할 수 없습니다.
     * @param command   검증을 통과한 등록 입력
     * @return 생성된 근무 Case 식별자
     */
    Long create(AuthPrincipal principal, WorkCaseCreateCommand command);

    /**
     * {@code DRAFT} 근무의 조건을 교체하고 조건 Version을 1 증가시킵니다.
     *
     * <p>같은 트랜잭션에서 현재 활성 {@code PENDING} 초대를 철회합니다.</p>
     *
     * @param principal 소유권을 결정하는 인증 Principal
     * @param command   검증을 통과한 수정 입력
     */
    void update(AuthPrincipal principal, WorkCaseUpdateCommand command);

    /**
     * {@code DRAFT} 근무를 삭제하거나 취소합니다.
     *
     * <p>초대 이력이 없으면 물리 삭제하고, 있으면 활성 {@code PENDING} 초대를 철회한 뒤
     * {@code CANCELED}로 전이합니다.</p>
     *
     * @param principal 소유권을 결정하는 인증 Principal
     * @param workCaseId 대상 근무 Case 식별자
     */
    void delete(AuthPrincipal principal, Long workCaseId);
}
