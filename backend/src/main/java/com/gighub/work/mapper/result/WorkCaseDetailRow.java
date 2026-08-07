package com.gighub.work.mapper.result;

import java.time.LocalDateTime;

import com.gighub.work.domain.WorkCaseStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * 근무 상세 조회의 본문 행입니다.
 *
 * <p>{@code employerId}는 당사자 판정에만 쓰고 응답에 노출하지 않습니다. 좌표·인증 반경·
 * 전화번호는 API_SPEC 4.0.0이 상세 응답에서 제외했으므로 애초에 조회하지 않습니다.</p>
 */
@Getter
@Builder(toBuilder = true)
@AllArgsConstructor
public class WorkCaseDetailRow {

    private final Long workCaseId;
    private final String title;
    private final LocalDateTime startsAt;
    private final LocalDateTime endsAt;
    private final Integer breakMinutes;
    private final Boolean breakPaid;
    private final Long dailyWage;
    private final WorkCaseStatus status;
    private final Integer termsVersion;
    private final String workplaceName;
    private final String workplaceAddress;
    private final Long employerId;
    private final Long workerId;
    private final String workerName;
}
