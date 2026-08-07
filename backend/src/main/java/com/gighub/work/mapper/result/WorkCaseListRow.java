package com.gighub.work.mapper.result;

import java.time.LocalDateTime;

import com.gighub.work.domain.WorkCaseStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * 근무 목록 조회 SQL의 행 하나입니다.
 *
 * <p>{@code workerId}가 {@code null}이면 미매칭입니다. API_SPEC 4.0.0은 이때 {@code worker}
 * 객체 전체를 {@code null}로 응답하도록 고정했으므로, {@code workerName}을 별도 필드로 두지
 * 않고 이 행 하나로 두 값을 함께 나릅니다.</p>
 *
 * <p>MyBatis가 &lt;constructor&gt; 매핑으로 생성하므로 필드 선언 순서가 곧 생성자 인자
 * 순서입니다. XML과 함께 바꿔야 합니다.</p>
 */
@Getter
@Builder(toBuilder = true)
@AllArgsConstructor
public class WorkCaseListRow {

    private final Long workCaseId;
    private final String title;
    private final LocalDateTime startsAt;
    private final LocalDateTime endsAt;
    private final Long dailyWage;
    private final WorkCaseStatus status;
    private final Long workerId;
    private final String workerName;
}
