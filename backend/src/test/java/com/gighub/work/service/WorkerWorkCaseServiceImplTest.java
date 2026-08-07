package com.gighub.work.service.impl;

import com.gighub.auth.security.AuthPrincipal;
import com.gighub.common.api.PageResponse;
import com.gighub.common.exception.RoleMismatchException;
import com.gighub.member.domain.UserRole;
import com.gighub.settlement.domain.SettlementStatus;
import com.gighub.work.domain.WorkCaseStatus;
import com.gighub.work.dto.WorkerHomeResponse;
import com.gighub.work.dto.WorkerWorkCaseResponse;
import com.gighub.work.mapper.WorkerWorkCaseMapper;
import com.gighub.work.mapper.result.WorkerWorkCaseRow;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class WorkerWorkCaseServiceImplTest {

    private static final Long WORKER_ID = 9L;
    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    private final WorkerWorkCaseMapper mapper = mock(WorkerWorkCaseMapper.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-08-07T03:00:00Z"), SEOUL);
    private final WorkerWorkCaseServiceImpl service = new WorkerWorkCaseServiceImpl(mapper, clock);

    @Test
    void homeUsesSeoulTodayAndPreviousDayLingeringWindow() {
        when(mapper.findToday(
                WORKER_ID,
                LocalDateTime.of(2026, 8, 6, 0, 0),
                LocalDateTime.of(2026, 8, 7, 0, 0),
                LocalDateTime.of(2026, 8, 8, 0, 0)))
                .thenReturn(row());

        WorkerHomeResponse response = service.home(worker());

        assertEquals(101L, response.getTodayWorkCase().getWorkCaseId());
        assertEquals(295_550L, response.getTodayWorkCase().getExpectedNetAmount());
        assertEquals(2L, response.getTodayWorkCase().getAttendance().getLateMinutes());
        assertEquals(true, response.getTodayWorkCase().getAttendance().getIsLate());
    }

    @Test
    void homeReturnsNullWhenThereIsNoCandidate() {
        WorkerHomeResponse response = service.home(worker());

        assertNull(response.getTodayWorkCase());
    }

    @Test
    void listUsesCommonPagingAndMapsStoredStatuses() {
        when(mapper.findPage(WORKER_ID, 20, 20L)).thenReturn(List.of(row()));
        when(mapper.count(WORKER_ID)).thenReturn(21L);

        PageResponse<WorkerWorkCaseResponse> response = service.list(worker(), 1, 20);

        assertEquals(1, response.getContent().size());
        assertEquals(WorkCaseStatus.IN_PROGRESS, response.getContent().get(0).getStatus());
        assertEquals(SettlementStatus.SCHEDULED, response.getContent().get(0).getSettlementStatus());
        assertEquals(2, response.getPage().getTotalPages());
    }

    @Test
    void queryRejectsOwnerBeforeStorageAccess() {
        AuthPrincipal owner = new AuthPrincipal(7L, UserRole.OWNER, "사장");

        assertThrows(RoleMismatchException.class, () -> service.home(owner));
        assertThrows(RoleMismatchException.class, () -> service.list(owner, 0, 20));

        verifyNoInteractions(mapper);
    }

    @Test
    void listPassesCalculatedOffsetToMapper() {
        when(mapper.findPage(WORKER_ID, 25, 50L)).thenReturn(List.of());

        service.list(worker(), 2, 25);

        ArgumentCaptor<Long> offset = ArgumentCaptor.forClass(Long.class);
        verify(mapper).findPage(org.mockito.ArgumentMatchers.eq(WORKER_ID),
                org.mockito.ArgumentMatchers.eq(25), offset.capture());
        assertEquals(50L, offset.getValue());
    }

    private AuthPrincipal worker() {
        return new AuthPrincipal(WORKER_ID, UserRole.WORKER, "근로자");
    }

    private WorkerWorkCaseRow row() {
        return WorkerWorkCaseRow.builder()
                .workCaseId(101L)
                .title("주말 홀 서빙")
                .workplaceName("카페 봄")
                .startsAt(LocalDateTime.of(2026, 8, 7, 10, 0))
                .endsAt(LocalDateTime.of(2026, 8, 7, 18, 0))
                .breakMinutes(60)
                .breakPaid(false)
                .dailyWage(300_000L)
                .status(WorkCaseStatus.IN_PROGRESS)
                .checkedInAt(LocalDateTime.of(2026, 8, 7, 10, 1, 1))
                .escrowStatus("HELD")
                .settlementStatus(SettlementStatus.SCHEDULED)
                .settlementDueAt(LocalDateTime.of(2026, 8, 8, 18, 0))
                .build();
    }
}
