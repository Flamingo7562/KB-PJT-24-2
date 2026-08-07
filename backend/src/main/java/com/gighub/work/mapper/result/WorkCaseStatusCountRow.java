package com.gighub.work.mapper.result;

import com.gighub.work.domain.WorkCaseStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * 상태별 근무 건수 집계 행 하나입니다.
 *
 * <p>{@code GROUP BY}는 실제로 존재하는 상태만 돌려주므로 건수가 0인 상태는 결과에 없습니다.
 * 8개 상태를 모두 채우는 일은 이 행을 받는 쪽이
 * {@link WorkCaseStatus#values()}로 처리합니다.</p>
 */
@Getter
@Builder(toBuilder = true)
@AllArgsConstructor
public class WorkCaseStatusCountRow {

    private final WorkCaseStatus status;
    private final Long caseCount;
}
