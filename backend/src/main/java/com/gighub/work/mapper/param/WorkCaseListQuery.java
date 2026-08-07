package com.gighub.work.mapper.param;

import java.time.LocalDate;

import com.gighub.work.domain.WorkCaseStatus;

import lombok.Builder;
import lombok.Getter;

/**
 * 근무 목록 조회 조건입니다.
 *
 * <p>건수 SQL과 목록 SQL이 같은 객체를 받도록 해, 두 쿼리의 {@code WHERE}가 서로 다른
 * 값으로 어긋나는 것을 막습니다. 어긋나면 마지막 Page가 비거나 총 건수가 실제와 달라집니다.</p>
 *
 * <p>{@code keyword}는 이미 trim되고 빈 문자열이면 {@code null}로 정규화된 값을 받습니다.
 * 정규화를 Service에서 한 번만 하고 그 결과를 그대로 전달합니다.</p>
 */
@Getter
@Builder
public final class WorkCaseListQuery {

    private final Long workplaceId;
    private final Long ownerUserId;
    private final String keyword;
    private final WorkCaseStatus status;
    private final LocalDate from;
    private final LocalDate to;
    private final int size;
    private final long offset;
}
