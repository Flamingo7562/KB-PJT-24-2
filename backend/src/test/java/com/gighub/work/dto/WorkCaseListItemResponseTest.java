package com.gighub.work.dto;

import java.time.LocalDateTime;

import com.gighub.work.domain.WorkCaseStatus;
import com.gighub.work.mapper.result.WorkCaseListRow;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class WorkCaseListItemResponseTest {

    @Test
    void returnsNullWorkerWhenUnmatched() {
        WorkCaseListItemResponse response = WorkCaseListItemResponse.from(row(null, null));

        assertNull(response.getWorker());
    }

    @Test
    void returnsWorkerSummaryWhenMatched() {
        WorkCaseListItemResponse response = WorkCaseListItemResponse.from(row(42L, "이알바"));

        assertNotNull(response.getWorker());
        assertEquals(42L, response.getWorker().getWorkerId());
        assertEquals("이알바", response.getWorker().getName());
    }

    @Test
    void derivesWorkDateFromStartsAt() {
        WorkCaseListItemResponse response = WorkCaseListItemResponse.from(row(null, null));

        assertEquals(LocalDateTime.of(2026, 8, 10, 9, 0).toLocalDate(), response.getWorkDate());
    }

    private WorkCaseListRow row(Long workerId, String workerName) {
        return WorkCaseListRow.builder()
                .workCaseId(101L)
                .title("주말 홀 서빙")
                .startsAt(LocalDateTime.of(2026, 8, 10, 9, 0))
                .endsAt(LocalDateTime.of(2026, 8, 10, 18, 0))
                .dailyWage(120_000L)
                .status(WorkCaseStatus.READY)
                .workerId(workerId)
                .workerName(workerName)
                .build();
    }
}
