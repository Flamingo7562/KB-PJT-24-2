package com.gighub.work.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import com.gighub.auth.security.AuthPrincipal;
import com.gighub.common.exception.ResourceNotFoundException;
import com.gighub.common.exception.RoleMismatchException;
import com.gighub.common.exception.ValidationException;
import com.gighub.common.exception.WorkCaseLockedException;
import com.gighub.member.domain.UserRole;
import com.gighub.work.domain.WorkCaseStatus;
import com.gighub.work.mapper.WorkCaseMapper;
import com.gighub.work.mapper.param.WorkCaseInsertParam;
import com.gighub.work.mapper.param.WorkCaseTermsUpdateParam;
import com.gighub.work.mapper.result.OwnedWorkplaceSnapshotRow;
import com.gighub.work.mapper.result.WorkCaseLockRow;
import com.gighub.work.service.command.WorkCaseCreateCommand;
import com.gighub.work.service.command.WorkCaseUpdateCommand;
import com.gighub.work.service.impl.WorkCaseServiceImpl;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class WorkCaseServiceImplTest {

    private static final Long OWNER_ID = 7L;
    private static final Long WORKPLACE_ID = 5L;
    private static final Long WORK_CASE_ID = 101L;

    private final WorkCaseMapper workCaseMapper = mock(WorkCaseMapper.class);
    private final WorkCaseServiceImpl service = new WorkCaseServiceImpl(workCaseMapper);

    // ---------- create ----------

    @Test
    void createRejectsNonOwner() {
        assertThrows(
                RoleMismatchException.class,
                () -> service.create(worker(), validCreateCommand()));

        verifyNoInteractions(workCaseMapper);
    }

    @Test
    void createRejectsWhenWorkplaceIsNotOwnedActive() {
        when(workCaseMapper.findOwnedActiveWorkplace(WORKPLACE_ID, OWNER_ID)).thenReturn(null);

        assertThrows(
                ResourceNotFoundException.class,
                () -> service.create(owner(), validCreateCommand()));

        verify(workCaseMapper, never()).insert(any());
    }

    @Test
    void createRejectsEndTimeNotAfterStartTime() {
        when(workCaseMapper.findOwnedActiveWorkplace(WORKPLACE_ID, OWNER_ID))
                .thenReturn(snapshot());

        WorkCaseCreateCommand command = WorkCaseCreateCommand.builder()
                .workplaceId(WORKPLACE_ID)
                .title("주말 홀 서빙")
                .workDate(LocalDate.of(2026, 8, 10))
                .startTime(LocalTime.of(18, 0))
                .endTime(LocalTime.of(9, 0))
                .breakMinutes(60)
                .breakPaid(false)
                .dailyWage(120_000L)
                .build();

        assertThrows(ValidationException.class, () -> service.create(owner(), command));

        verify(workCaseMapper, never()).insert(any());
    }

    @Test
    void createStoresCombinedAddressAndServerOwnedColumns() {
        when(workCaseMapper.findOwnedActiveWorkplace(WORKPLACE_ID, OWNER_ID))
                .thenReturn(snapshot());
        doAnswer(invocation -> {
            invocation.getArgument(0, WorkCaseInsertParam.class).setWorkCaseId(WORK_CASE_ID);
            return 1;
        }).when(workCaseMapper).insert(any(WorkCaseInsertParam.class));

        Long workCaseId = service.create(owner(), validCreateCommand());

        assertEquals(WORK_CASE_ID, workCaseId);

        ArgumentCaptor<WorkCaseInsertParam> captor = ArgumentCaptor.forClass(WorkCaseInsertParam.class);
        verify(workCaseMapper).insert(captor.capture());

        WorkCaseInsertParam param = captor.getValue();
        assertEquals(OWNER_ID, param.getEmployerId());
        assertEquals(WORKPLACE_ID, param.getWorkplaceId());
        assertEquals("서울 강남구 테헤란로 1 2층", param.getWorkplaceAddress());
        assertEquals(LocalDateTime.of(2026, 8, 10, 9, 0), param.getStartsAt());
        assertEquals(LocalDateTime.of(2026, 8, 10, 18, 0), param.getEndsAt());
    }

    // ---------- update ----------

    @Test
    void updateRejectsWhenWorkCaseNotOwnedByPrincipal() {
        when(workCaseMapper.lockById(WORK_CASE_ID)).thenReturn(lockRow(99L, WorkCaseStatus.DRAFT));

        assertThrows(
                ResourceNotFoundException.class,
                () -> service.update(owner(), validUpdateCommand()));

        verify(workCaseMapper, never()).updateDraftTerms(any());
    }

    @Test
    void updateRejectsWhenNotDraft() {
        when(workCaseMapper.lockById(WORK_CASE_ID))
                .thenReturn(lockRow(OWNER_ID, WorkCaseStatus.ACCEPTED));

        assertThrows(
                WorkCaseLockedException.class,
                () -> service.update(owner(), validUpdateCommand()));

        verify(workCaseMapper, never()).updateDraftTerms(any());
    }

    @Test
    void updateBumpsVersionAndRevokesPendingInvitations() {
        when(workCaseMapper.lockById(WORK_CASE_ID))
                .thenReturn(lockRow(OWNER_ID, WorkCaseStatus.DRAFT));

        service.update(owner(), validUpdateCommand());

        ArgumentCaptor<WorkCaseTermsUpdateParam> captor =
                ArgumentCaptor.forClass(WorkCaseTermsUpdateParam.class);
        verify(workCaseMapper).updateDraftTerms(captor.capture());
        verify(workCaseMapper).revokePendingInvitations(WORK_CASE_ID);

        assertEquals(WORK_CASE_ID, captor.getValue().getWorkCaseId());
    }

    // ---------- delete ----------

    @Test
    void deleteHardDeletesDraftWithoutInvitationHistory() {
        when(workCaseMapper.lockById(WORK_CASE_ID))
                .thenReturn(lockRow(OWNER_ID, WorkCaseStatus.DRAFT));
        when(workCaseMapper.countInvitations(WORK_CASE_ID)).thenReturn(0);

        service.delete(owner(), WORK_CASE_ID);

        verify(workCaseMapper).deleteDraft(WORK_CASE_ID);
        verify(workCaseMapper, never()).cancelDraft(anyLong());
        verify(workCaseMapper, never()).revokePendingInvitations(anyLong());
    }

    @Test
    void deleteCancelsDraftWithInvitationHistory() {
        when(workCaseMapper.lockById(WORK_CASE_ID))
                .thenReturn(lockRow(OWNER_ID, WorkCaseStatus.DRAFT));
        when(workCaseMapper.countInvitations(WORK_CASE_ID)).thenReturn(2);

        service.delete(owner(), WORK_CASE_ID);

        verify(workCaseMapper).revokePendingInvitations(WORK_CASE_ID);
        verify(workCaseMapper).cancelDraft(WORK_CASE_ID);
        verify(workCaseMapper, never()).deleteDraft(anyLong());
    }

    @Test
    void deleteRejectsWhenNotDraft() {
        when(workCaseMapper.lockById(WORK_CASE_ID))
                .thenReturn(lockRow(OWNER_ID, WorkCaseStatus.CANCELED));

        assertThrows(WorkCaseLockedException.class, () -> service.delete(owner(), WORK_CASE_ID));

        verify(workCaseMapper, never()).deleteDraft(anyLong());
        verify(workCaseMapper, never()).cancelDraft(anyLong());
    }

    // ---------- fixtures ----------

    private AuthPrincipal owner() {
        return new AuthPrincipal(OWNER_ID, UserRole.OWNER, "김사장");
    }

    private AuthPrincipal worker() {
        return new AuthPrincipal(OWNER_ID, UserRole.WORKER, "이알바");
    }

    private OwnedWorkplaceSnapshotRow snapshot() {
        return OwnedWorkplaceSnapshotRow.builder()
                .workplaceId(WORKPLACE_ID)
                .workplaceName("강남점")
                .roadAddress("서울 강남구 테헤란로 1")
                .detailAddress("2층")
                .latitude(new BigDecimal("37.1234567"))
                .longitude(new BigDecimal("127.1234567"))
                .radiusMeters(new BigDecimal("100.00"))
                .build();
    }

    private WorkCaseLockRow lockRow(Long employerId, WorkCaseStatus status) {
        return WorkCaseLockRow.builder()
                .workCaseId(WORK_CASE_ID)
                .employerId(employerId)
                .workplaceId(WORKPLACE_ID)
                .workerId(null)
                .status(status)
                .termsVersion(1)
                .build();
    }

    private WorkCaseCreateCommand validCreateCommand() {
        return WorkCaseCreateCommand.builder()
                .workplaceId(WORKPLACE_ID)
                .title("주말 홀 서빙")
                .workDate(LocalDate.of(2026, 8, 10))
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(18, 0))
                .breakMinutes(60)
                .breakPaid(false)
                .dailyWage(120_000L)
                .build();
    }

    private WorkCaseUpdateCommand validUpdateCommand() {
        return WorkCaseUpdateCommand.builder()
                .workCaseId(WORK_CASE_ID)
                .title("주말 홀 서빙(수정)")
                .workDate(LocalDate.of(2026, 8, 10))
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(19, 0))
                .breakMinutes(30)
                .breakPaid(true)
                .dailyWage(130_000L)
                .build();
    }
}
