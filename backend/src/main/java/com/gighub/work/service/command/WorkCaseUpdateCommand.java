package com.gighub.work.service.command;

import java.time.LocalDate;
import java.time.LocalTime;

import lombok.Builder;
import lombok.Getter;

/**
 * 검증을 통과한 근무 {@code DRAFT} 조건 수정 입력을 Service로 전달하는 불변 Command입니다.
 *
 * <p>{@code workCaseId}는 URL Path에서, 나머지 필드는 요청 Body에서 옮겨 옵니다. API_SPEC
 * 4.0.0은 PATCH가 일곱 필드를 모두 요구한다고 고정했으므로 부분 수정용 필드는 두지
 * 않습니다.</p>
 */
@Getter
@Builder
public final class WorkCaseUpdateCommand {

    private final Long workCaseId;
    private final String title;
    private final LocalDate workDate;
    private final LocalTime startTime;
    private final LocalTime endTime;
    private final Integer breakMinutes;
    private final Boolean breakPaid;
    private final Long dailyWage;
}
