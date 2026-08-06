package com.gighub.work.mapper.param;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * 근무 {@code DRAFT} INSERT 파라미터입니다.
 *
 * <p>{@code employerId}는 요청 Body가 아니라 인증 Principal에서 채웁니다.
 * {@code worker_id = NULL}, {@code terms_version = 1}, {@code status = 'DRAFT'}는 서버가
 * 고정하는 계약값이라 필드로 두지 않고 Mapper XML이 직접 기록합니다. 필드로 두면 호출부가
 * 다른 값을 넣을 수 있는 경로가 생깁니다.</p>
 *
 * <p>{@code workplaceAddress}는 이미 합쳐진 문자열을 받습니다. 도로명주소와 상세주소를 어떤
 * 규칙으로 합칠지는 아직 결정되지 않아, 그 규칙을 이 계층에 담지 않고 호출부가 만든 값을
 * 저장만 합니다.</p>
 *
 * <p>{@code workCaseId}만 MyBatis가 생성 Key를 되돌려 쓰기 위해 가변이고 나머지 입력값은
 * Builder로만 정합니다.</p>
 */
@Getter
@Builder
public class WorkCaseInsertParam {

    @Setter
    private Long workCaseId;

    private final Long employerId;
    private final Long workplaceId;
    private final String title;
    private final LocalDateTime startsAt;
    private final LocalDateTime endsAt;
    private final Integer breakMinutes;
    private final Boolean breakPaid;
    private final String workplaceName;
    private final String workplaceAddress;
    private final BigDecimal workplaceLatitude;
    private final BigDecimal workplaceLongitude;
    private final BigDecimal allowedRadiusMeters;
    private final Long dailyWage;
}
