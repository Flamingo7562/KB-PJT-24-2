package com.gighub.attendance.service;

import com.gighub.attendance.mapper.AttendanceLifecycleMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AttendanceLifecycleSchedulerTest {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 7, 9, 0);

    @Mock
    private AttendanceLifecycleMapper lifecycleMapper;

    @Mock
    private AttendanceLifecycleTransitionExecutor transitionExecutor;

    @Test
    void usesApprovedBoundariesAndProcessesReadyBeforeTerminalTransitions() {
        when(lifecycleMapper.findReadyCandidateIds(NOW.plusMinutes(30), NOW.minusHours(1)))
                .thenReturn(List.of(1L));
        when(lifecycleMapper.findNoShowCandidateIds(NOW.minusHours(1)))
                .thenReturn(List.of(2L));
        when(lifecycleMapper.findCheckoutMissingCandidateIds(NOW.minusHours(2)))
                .thenReturn(List.of(3L));

        scheduler().runOnce();

        verify(lifecycleMapper).findReadyCandidateIds(NOW.plusMinutes(30), NOW.minusHours(1));
        verify(lifecycleMapper).findNoShowCandidateIds(NOW.minusHours(1));
        verify(lifecycleMapper).findCheckoutMissingCandidateIds(NOW.minusHours(2));

        InOrder order = inOrder(transitionExecutor);
        order.verify(transitionExecutor).advanceToReady(1L, NOW);
        order.verify(transitionExecutor).advanceToNoShow(2L, NOW);
        order.verify(transitionExecutor).advanceToCheckoutMissing(3L, NOW);
    }

    @Test
    void continuesWithOtherCandidatesWhenOneTransitionFails() {
        when(lifecycleMapper.findReadyCandidateIds(NOW.plusMinutes(30), NOW.minusHours(1)))
                .thenReturn(List.of(1L, 2L));
        when(lifecycleMapper.findNoShowCandidateIds(NOW.minusHours(1)))
                .thenReturn(List.of());
        when(lifecycleMapper.findCheckoutMissingCandidateIds(NOW.minusHours(2)))
                .thenReturn(List.of());
        when(transitionExecutor.advanceToReady(1L, NOW))
                .thenThrow(new IllegalStateException("temporary failure"));

        scheduler().runOnce();

        verify(transitionExecutor).advanceToReady(2L, NOW);
    }

    private AttendanceLifecycleScheduler scheduler() {
        Clock clock = Clock.fixed(NOW.atZone(SEOUL).toInstant(), SEOUL);
        return new AttendanceLifecycleScheduler(lifecycleMapper, transitionExecutor, clock);
    }
}
