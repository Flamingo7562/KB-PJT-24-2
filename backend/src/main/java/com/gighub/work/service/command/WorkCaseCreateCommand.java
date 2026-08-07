package com.gighub.work.service.command;

import java.time.LocalDate;
import java.time.LocalTime;

import lombok.Builder;
import lombok.Getter;

/**
 * 검증을 통과한 근무 {@code DRAFT} 등록 입력을 Service로 전달하는 불변 Command입니다.
 *
 * <p>{@code workplaceId}는 URL Path에서, 나머지 필드는 요청 Body에서 옮겨 옵니다. 소유자는
 * Command가 아니라 Service가 인증 Principal에서 직접 채웁니다.</p>
 */
@Getter
@Builder
public final class WorkCaseCreateCommand {

    private final Long workplaceId;
    private final String title;
    private final LocalDate workDate;
    private final LocalTime startTime;
    private final LocalTime endTime;
    private final Integer breakMinutes;
    private final Boolean breakPaid;
    private final Long dailyWage;
}
