package com.gighub.work.mapper.result;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * 근무 상세의 근태 요약입니다.
 *
 * <p>조건부 집계 하나로 출근·퇴근을 함께 읽습니다. {@code attendance_records}의
 * {@code uk_attendance_records_success} 제약이 근무당 유형별 {@code SUCCESS} 행을 하나로
 * 제한하므로 {@code MAX}로도 값이 여러 개로 섞이지 않습니다. 근태 행이 전혀 없어도 집계
 * 쿼리는 두 필드가 모두 {@code null}인 행 하나를 돌려주므로, 이 값을 담는 쪽은 항상
 * {@code null}이 아닌 객체로 다룰 수 있습니다.</p>
 */
@Getter
@Builder(toBuilder = true)
@AllArgsConstructor
public class AttendanceSummaryRow {

    private final LocalDateTime checkedInAt;
    private final LocalDateTime checkedOutAt;
}
