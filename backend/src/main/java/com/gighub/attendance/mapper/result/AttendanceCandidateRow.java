package com.gighub.attendance.mapper.result;

import lombok.Getter;
import lombok.Setter;

/** 잠금 전 조회에서 찾은 스캔 대상 근무와 출퇴근 유형입니다. */
@Getter
@Setter
public class AttendanceCandidateRow {

    private Long workCaseId;
    private String scanType;
}
