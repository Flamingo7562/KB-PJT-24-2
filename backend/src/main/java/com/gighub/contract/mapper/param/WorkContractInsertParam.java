package com.gighub.contract.mapper.param;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 계약 Snapshot INSERT 파라미터입니다.
 *
 * <p>조건 값을 근무 행에서 그대로 복사합니다. 계약서는 확정 순간을 증명해야 하므로 이후
 * 근무 조건이 바뀌어도 이 행은 따라가지 않습니다.</p>
 *
 * <p>{@code termsSnapshotJson}은 이미 직렬화한 문자열입니다. 저장 직전에 Shape가 바뀌지 않도록
 * 조립을 서비스 계층에서 끝내고 Mapper는 값만 씁니다.</p>
 */
@Getter
@Builder
public class WorkContractInsertParam {

    /** MyBatis가 생성 Key를 되돌려 쓰기 위해 이 필드만 가변입니다. */
    @Setter
    private Long id;

    private final Long workCaseId;
    private final Long employerId;
    private final Long workerId;
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
    private final Integer sourceTermsVersion;
    private final String termsSnapshotJson;
    private final LocalDateTime acceptedAt;
}
