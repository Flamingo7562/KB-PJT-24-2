package com.gighub.work.service.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import com.gighub.auth.security.AuthPrincipal;
import com.gighub.common.api.PageRequests;
import com.gighub.common.api.PageResponse;
import com.gighub.common.exception.ResourceNotFoundException;
import com.gighub.common.exception.RoleMismatchException;
import com.gighub.common.exception.ValidationException;
import com.gighub.common.exception.WorkCaseLockedException;
import com.gighub.member.domain.UserRole;
import com.gighub.work.domain.WorkCaseAddress;
import com.gighub.work.domain.WorkCaseStatus;
import com.gighub.work.domain.WorkCaseTimes;
import com.gighub.work.dto.WorkCaseListItemResponse;
import com.gighub.work.dto.WorkCaseSummaryResponse;
import com.gighub.work.mapper.WorkCaseMapper;
import com.gighub.work.mapper.param.WorkCaseInsertParam;
import com.gighub.work.mapper.param.WorkCaseListQuery;
import com.gighub.work.mapper.param.WorkCaseTermsUpdateParam;
import com.gighub.work.mapper.result.OwnedWorkplaceSnapshotRow;
import com.gighub.work.mapper.result.WorkCaseLockRow;
import com.gighub.work.service.WorkCaseService;
import com.gighub.work.service.command.WorkCaseCreateCommand;
import com.gighub.work.service.command.WorkCaseUpdateCommand;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 승인된 근무 {@code DRAFT} 계약을 인증 Principal과 DB 현재 상태로 적용합니다. */
@Service
public class WorkCaseServiceImpl implements WorkCaseService {

    private final WorkCaseMapper workCaseMapper;

    public WorkCaseServiceImpl(WorkCaseMapper workCaseMapper) {
        this.workCaseMapper = workCaseMapper;
    }

    @Override
    @Transactional
    public Long create(AuthPrincipal principal, WorkCaseCreateCommand command) {
        requireOwner(principal);

        LocalDateTime startsAt = WorkCaseTimes.combine(command.getWorkDate(), command.getStartTime());
        LocalDateTime endsAt = WorkCaseTimes.combine(command.getWorkDate(), command.getEndTime());
        requireEndsAfterStart(startsAt, endsAt);

        // 소유권·ACTIVE 확인과 Snapshot 원본 조회를 한 쿼리로 처리합니다. 없으면 사업장이
        // 없거나 다른 OWNER 소유이거나 INACTIVE인 것이며, 세 경우를 구분해 노출하지 않습니다.
        OwnedWorkplaceSnapshotRow snapshot = workCaseMapper.findOwnedActiveWorkplace(
                command.getWorkplaceId(), principal.getUserId());
        if (snapshot == null) {
            throw new ResourceNotFoundException("등록할 수 있는 사업장을 찾을 수 없습니다.");
        }

        WorkCaseInsertParam param = WorkCaseInsertParam.builder()
                .employerId(principal.getUserId())
                .workplaceId(command.getWorkplaceId())
                .title(command.getTitle())
                .startsAt(startsAt)
                .endsAt(endsAt)
                .breakMinutes(command.getBreakMinutes())
                .breakPaid(command.getBreakPaid())
                .workplaceName(snapshot.getWorkplaceName())
                .workplaceAddress(WorkCaseAddress.combine(
                        snapshot.getRoadAddress(), snapshot.getDetailAddress()))
                .workplaceLatitude(snapshot.getLatitude())
                .workplaceLongitude(snapshot.getLongitude())
                .allowedRadiusMeters(snapshot.getRadiusMeters())
                .dailyWage(command.getDailyWage())
                .build();

        workCaseMapper.insert(param);
        // 생성 Key 회수가 깨지면 201과 함께 workCaseId=null이 조용히 나갑니다.
        // ApiResponse는 래퍼만 검사하므로 여기서 끊습니다.
        return Objects.requireNonNull(param.getWorkCaseId(), "생성된 근무 Case 식별자");
    }

    @Override
    @Transactional
    public void update(AuthPrincipal principal, WorkCaseUpdateCommand command) {
        requireOwner(principal);
        WorkCaseLockRow lock = lockOwned(principal, command.getWorkCaseId());
        requireDraft(lock);

        LocalDateTime startsAt = WorkCaseTimes.combine(command.getWorkDate(), command.getStartTime());
        LocalDateTime endsAt = WorkCaseTimes.combine(command.getWorkDate(), command.getEndTime());
        requireEndsAfterStart(startsAt, endsAt);

        WorkCaseTermsUpdateParam param = WorkCaseTermsUpdateParam.builder()
                .workCaseId(command.getWorkCaseId())
                .title(command.getTitle())
                .startsAt(startsAt)
                .endsAt(endsAt)
                .breakMinutes(command.getBreakMinutes())
                .breakPaid(command.getBreakPaid())
                .dailyWage(command.getDailyWage())
                .build();

        // 행을 이미 잠그고 DRAFT임을 확인했으므로 이 UPDATE는 반드시 1행을 바꿉니다. 0이면
        // 잠금과 갱신 사이의 가정이 깨진 것이라 방어적으로 다루지 않고 그대로 드러냅니다.
        workCaseMapper.updateDraftTerms(param);
        // 조건이 바뀌면 이전 조건으로 발급된 PENDING 초대는 더 이상 유효하지 않습니다.
        // 활성 PENDING은 근무당 하나뿐이라 Version별 조건 없이 그대로 철회합니다.
        workCaseMapper.revokePendingInvitations(command.getWorkCaseId());
    }

    @Override
    @Transactional
    public void delete(AuthPrincipal principal, Long workCaseId) {
        requireOwner(principal);
        WorkCaseLockRow lock = lockOwned(principal, workCaseId);
        requireDraft(lock);

        // DRAFT는 ACCEPTED 이후에만 만들어지는 계약·에스크로를 가질 수 없습니다. 상태 확인이
        // 곧 "계약·에스크로가 없음"의 증명이라 별도 존재 조회를 추가하지 않습니다.
        if (workCaseMapper.countInvitations(workCaseId) == 0) {
            deleteOrReportLocked(workCaseId);
            return;
        }

        workCaseMapper.revokePendingInvitations(workCaseId);
        // CANCELED 전이는 status 등 일부 컬럼만 바꾸는 UPDATE라 자식 테이블의 FK RESTRICT를
        // 건드리지 않습니다. 행 자체를 지우는 DELETE만 참조 무결성 위반 가능성이 있습니다.
        workCaseMapper.cancelDraft(workCaseId);
    }

    @Override
    @Transactional(readOnly = true)
    public WorkCaseSummaryResponse summary(AuthPrincipal principal, Long workplaceId) {
        requireOwner(principal);
        // 소유하지 않은 사업장과 근무 0건인 소유 사업장을 구분해야 하므로, 집계 전에 소유권을
        // 먼저 확인합니다. countByStatus만으로는 두 경우가 똑같이 빈 결과로 보입니다.
        if (!workCaseMapper.existsOwnedManageableWorkplace(workplaceId, principal.getUserId())) {
            throw new ResourceNotFoundException("사업장을 찾을 수 없습니다.");
        }

        return WorkCaseSummaryResponse.from(
                workCaseMapper.countByStatus(workplaceId, principal.getUserId()));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<WorkCaseListItemResponse> list(
            AuthPrincipal principal,
            Long workplaceId,
            String keyword,
            WorkCaseStatus status,
            LocalDate from,
            LocalDate to,
            int page,
            int size) {
        requireOwner(principal);
        // 역할을 먼저 확인합니다. Page 값이 잘못된 요청이라도 권한 없는 호출자에게 400을
        // 돌려주면 Endpoint의 존재와 Query 규칙을 알려주게 됩니다.
        PageRequests.validate(page, size);
        if (!workCaseMapper.existsOwnedManageableWorkplace(workplaceId, principal.getUserId())) {
            throw new ResourceNotFoundException("사업장을 찾을 수 없습니다.");
        }
        requireValidDateRange(from, to);

        WorkCaseListQuery query = WorkCaseListQuery.builder()
                .workplaceId(workplaceId)
                .ownerUserId(principal.getUserId())
                .keyword(normalizeKeyword(keyword))
                .status(status)
                .from(from)
                .to(to)
                .size(size)
                .offset(PageRequests.offset(page, size))
                .build();

        long totalElements = workCaseMapper.countByFilters(query);
        List<WorkCaseListItemResponse> content = workCaseMapper.findPageByFilters(query).stream()
                .map(WorkCaseListItemResponse::from)
                .toList();

        return PageResponse.of(content, page, size, totalElements);
    }

    /**
     * 앞뒤 공백만 제거합니다. 공백만 남는 검색어는 {@code null}로 바꿔 미지정과 같게 취급합니다.
     */
    private String normalizeKeyword(String keyword) {
        if (keyword == null) {
            return null;
        }
        String trimmed = keyword.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private void requireValidDateRange(LocalDate from, LocalDate to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new ValidationException("from은 to보다 늦을 수 없습니다.");
        }
    }

    /**
     * DRAFT는 계약·에스크로를 가질 수 없어 FK RESTRICT를 정상적으로 만나지 않지만, 그 불변식이
     * 나중에 깨지더라도 원시 SQL 예외가 그대로 노출되지 않도록 승인 오류로 변환합니다.
     */
    private void deleteOrReportLocked(Long workCaseId) {
        try {
            workCaseMapper.deleteDraft(workCaseId);
        } catch (DataIntegrityViolationException referenced) {
            throw new WorkCaseLockedException("참조 중인 근무는 삭제할 수 없습니다.");
        }
    }

    /**
     * Security 설정은 인증 여부만 강제하므로 역할 경계는 도메인에서 확인합니다.
     *
     * <p>거절 근거가 역할 하나뿐이라 {@code 403 ROLE_MISMATCH}로 응답합니다.</p>
     */
    private void requireOwner(AuthPrincipal principal) {
        if (principal.getRole() != UserRole.OWNER) {
            throw new RoleMismatchException("근무 Case는 OWNER만 관리할 수 있습니다.");
        }
    }

    /**
     * 근무 행을 잠그고 호출자가 소유자인지 확인합니다.
     *
     * <p>존재하지 않는 근무와 다른 OWNER의 근무를 같은 404로 응답합니다. 403으로 구분하면
     * "존재는 하지만 내 것이 아니다"라는 사실이 노출됩니다.</p>
     */
    private WorkCaseLockRow lockOwned(AuthPrincipal principal, Long workCaseId) {
        WorkCaseLockRow lock = workCaseMapper.lockById(workCaseId);
        if (lock == null || !lock.getEmployerId().equals(principal.getUserId())) {
            throw new ResourceNotFoundException("근무 Case를 찾을 수 없습니다.");
        }
        return lock;
    }

    private void requireDraft(WorkCaseLockRow lock) {
        if (lock.getStatus() != WorkCaseStatus.DRAFT) {
            throw new WorkCaseLockedException("DRAFT 상태의 근무만 처리할 수 있습니다.");
        }
    }

    private void requireEndsAfterStart(LocalDateTime startsAt, LocalDateTime endsAt) {
        if (!WorkCaseTimes.endsAfterStart(startsAt, endsAt)) {
            throw new ValidationException("종료 시각은 시작 시각보다 뒤여야 합니다.");
        }
    }
}
