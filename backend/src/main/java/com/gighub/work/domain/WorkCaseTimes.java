package com.gighub.work.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Objects;

/**
 * 근무 조건 입력의 날짜와 시각을 DB 저장 값으로 결합합니다.
 *
 * <p>{@code work_cases.starts_at}과 {@code ends_at}은 시간대가 없는 {@code DATETIME(6)}이고,
 * 이 프로젝트는 그 자리에 {@code Asia/Seoul} 벽시계 값을 넣는 규약을 씁니다. JDBC URL의
 * {@code serverTimezone=Asia/Seoul}과 {@link com.gighub.common.api.ApiTimes}가 같은 전제를
 * 씁니다. 그래서 저장할 때는 시간대를 변환하지 않고 응답 경계에서만 UTC {@code Instant}로
 * 바꿉니다. 여기서 한 번 더 변환하면 같은 값이 두 번 밀립니다.</p>
 *
 * <p>API_SPEC 4.0.0이 등록·수정 결합을 {@code endsAt > startsAt} 검증으로 고정해, 야간 근무
 * (종료가 시작보다 이르거나 같은 입력을 다음 날로 보는 것)는 지원하지 않습니다. 이 클래스는
 * 판정을 직접 하지 않고 주어진 날짜와 시각을 결합하기만 하며, 결합 결과가 유효한지는 호출부가
 * {@link #endsAfterStart}로 확인합니다.</p>
 */
public final class WorkCaseTimes {

    private WorkCaseTimes() {
    }

    /**
     * 근무일과 시각을 DB에 저장할 {@code Asia/Seoul} 벽시계 값으로 결합합니다.
     *
     * @param workDate 근무일
     * @param time     같은 날짜에 결합할 시각
     * @return 시간대 변환 없이 결합한 {@code DATETIME} 저장 값
     */
    public static LocalDateTime combine(LocalDate workDate, LocalTime time) {
        Objects.requireNonNull(workDate, "workDate");
        Objects.requireNonNull(time, "time");
        return LocalDateTime.of(workDate, time);
    }

    /**
     * {@code ck_work_cases_time} CHECK와 같은 조건을 애플리케이션에서 먼저 확인합니다.
     *
     * <p>DB 제약은 최종 방어선으로 남겨 두되, 제약 위반 예외를 사용자 메시지로 되돌리는 것보다
     * 입력 단계에서 걸러 내는 편이 응답이 정확합니다.</p>
     *
     * @param startsAt 결합된 시작 시각
     * @param endsAt   결합된 종료 시각
     * @return 종료가 시작보다 뒤이면 {@code true}
     */
    public static boolean endsAfterStart(LocalDateTime startsAt, LocalDateTime endsAt) {
        Objects.requireNonNull(startsAt, "startsAt");
        Objects.requireNonNull(endsAt, "endsAt");
        return endsAt.isAfter(startsAt);
    }
}
