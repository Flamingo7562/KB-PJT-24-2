package com.gighub.work.service;

import java.time.LocalDate;

import com.gighub.auth.security.AuthPrincipal;
import com.gighub.common.api.PageResponse;
import com.gighub.work.domain.WorkCaseStatus;
import com.gighub.work.dto.WorkCaseListItemResponse;
import com.gighub.work.dto.WorkCaseSummaryResponse;
import com.gighub.work.service.command.WorkCaseCreateCommand;
import com.gighub.work.service.command.WorkCaseUpdateCommand;

/** OWNER 근무 {@code DRAFT} 생성·조건 수정·삭제·조회의 승인 규칙을 적용합니다. */
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

    /**
     * 인증 OWNER가 소유·관리하는 사업장의 상태별 근무 건수를 요약합니다.
     *
     * @param principal   소유권을 결정하는 인증 Principal
     * @param workplaceId 대상 사업장 식별자
     * @return 8개 상태를 모두 포함하는 요약. 데이터가 없는 상태는 0
     */
    WorkCaseSummaryResponse summary(AuthPrincipal principal, Long workplaceId);

    /**
     * 인증 OWNER가 소유·관리하는 사업장의 근무 목록을 정렬이 고정된 순서로 조회합니다.
     *
     * @param principal   소유권을 결정하는 인증 Principal
     * @param workplaceId 대상 사업장 식별자
     * @param keyword     제목 또는 매칭 WORKER 이름 부분 일치. 없으면 전체
     * @param status      단일 상태 필터. 없으면 전체 상태
     * @param from        {@code workDate} 하한(포함). 없으면 하한 없음
     * @param to          {@code workDate} 상한(포함). 없으면 상한 없음
     * @param page        0-based Page 번호
     * @param size        Page 크기
     * @return 승인된 Page Envelope payload. 근무가 없으면 빈 {@code content}
     */
    PageResponse<WorkCaseListItemResponse> list(
            AuthPrincipal principal,
            Long workplaceId,
            String keyword,
            WorkCaseStatus status,
            LocalDate from,
            LocalDate to,
            int page,
            int size);
}
