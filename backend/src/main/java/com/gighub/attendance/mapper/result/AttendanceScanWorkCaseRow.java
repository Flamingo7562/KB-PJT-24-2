package com.gighub.attendance.mapper.result;

import com.gighub.work.domain.WorkCaseStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** 스캔 본 처리가 잠근 근무의 현재 상태와 시간창입니다. */
@Getter
@Setter
public class AttendanceScanWorkCaseRow {

    private Long workCaseId;
    private Long workerId;
    private Long workplaceId;
    private WorkCaseStatus status;
    private LocalDateTime startsAt;
    private LocalDateTime endsAt;
}
