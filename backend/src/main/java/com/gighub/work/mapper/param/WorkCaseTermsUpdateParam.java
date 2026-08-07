package com.gighub.work.mapper.param;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Getter;

/**
 * {@code DRAFT} 근무의 조건 수정 파라미터입니다.
 *
 * <p>사업장 Snapshot과 소유자는 조건 수정 대상이 아니라 들어 있지 않습니다. 근무를 다른
 * 사업장으로 옮기는 것은 조건 변경이 아니라 다른 근무이므로 여기서 허용하지 않습니다.</p>
 *
 * <p>{@code terms_version} 증가는 값으로 받지 않고 Mapper XML이 현재 값 기준으로 1을
 * 더합니다. 호출부가 읽은 값으로 덮어쓰면 동시 수정에서 같은 Version이 두 번 저장됩니다.</p>
 */
@Getter
@Builder
public class WorkCaseTermsUpdateParam {

    private final Long workCaseId;
    private final String title;
    private final LocalDateTime startsAt;
    private final LocalDateTime endsAt;
    private final Integer breakMinutes;
    private final Boolean breakPaid;
    private final Long dailyWage;
}
