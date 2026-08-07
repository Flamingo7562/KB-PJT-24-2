package com.gighub.attendance.mapper.result;

import com.gighub.work.domain.WorkCaseStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/** 자동 상태 전이가 근무 행 잠금 안에서 다시 판단하는 최소 Snapshot입니다. */
@Getter
@Builder
@AllArgsConstructor
public class AttendanceLifecycleWorkCaseRow {

    private final Long workCaseId;
    private final WorkCaseStatus status;
    private final LocalDateTime startsAt;
    private final LocalDateTime endsAt;
}
