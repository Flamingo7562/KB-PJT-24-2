package com.gighub.work.dto;

import com.gighub.common.api.ApiTimes;
import com.gighub.settlement.domain.SettlementStatus;
import com.gighub.work.domain.WorkCaseStatus;
import com.gighub.work.mapper.result.WorkerWorkCaseRow;
import lombok.Getter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;

/** 화면 별칭 없이 저장 상태와 근태 결과를 전달하는 WORKER 근무 응답입니다. */
@Getter
public final class WorkerWorkCaseResponse {

    private static final long DAILY_TAX_FREE_AMOUNT = 150_000L;
    private static final long MINIMUM_INCOME_TAX = 1_000L;

    private final Long workCaseId;
    private final String title;
    private final String workplaceName;
    private final Instant startsAt;
    private final Instant endsAt;
    private final Integer breakMinutes;
    private final Boolean breakPaid;
    private final Long dailyWage;
    private final Long expectedNetAmount;
    private final WorkCaseStatus status;
    private final Attendance attendance;
    private final String escrowStatus;
    private final SettlementStatus settlementStatus;
    private final Instant settlementDueAt;

    private WorkerWorkCaseResponse(WorkerWorkCaseRow row) {
        this.workCaseId = row.getWorkCaseId();
        this.title = row.getTitle();
        this.workplaceName = row.getWorkplaceName();
        this.startsAt = ApiTimes.toInstant(row.getStartsAt());
        this.endsAt = ApiTimes.toInstant(row.getEndsAt());
        this.breakMinutes = row.getBreakMinutes();
        this.breakPaid = row.getBreakPaid();
        this.dailyWage = row.getDailyWage();
        this.expectedNetAmount = calculateExpectedNet(row.getDailyWage());
        this.status = row.getStatus();
        this.attendance = new Attendance(row);
        this.escrowStatus = row.getEscrowStatus();
        this.settlementStatus = row.getSettlementStatus();
        this.settlementDueAt = ApiTimes.toInstant(row.getSettlementDueAt());
    }

    public static WorkerWorkCaseResponse from(WorkerWorkCaseRow row) {
        return new WorkerWorkCaseResponse(row);
    }

    /** 프런트와 같은 일용직 원천징수 참조식을 정수 원 단위로 적용합니다. */
    private static long calculateExpectedNet(long dailyWage) {
        long taxable = Math.max(0L, dailyWage - DAILY_TAX_FREE_AMOUNT);
        long incomeTax = BigDecimal.valueOf(taxable)
                .multiply(new BigDecimal("0.027"))
                .divide(BigDecimal.TEN, 0, RoundingMode.FLOOR)
                .multiply(BigDecimal.TEN)
                .longValueExact();
        if (incomeTax < MINIMUM_INCOME_TAX) {
            return dailyWage;
        }
        long localIncomeTax = BigDecimal.valueOf(taxable)
                .multiply(new BigDecimal("0.0027"))
                .divide(BigDecimal.TEN, 0, RoundingMode.FLOOR)
                .multiply(BigDecimal.TEN)
                .longValueExact();
        return Math.subtractExact(dailyWage, Math.addExact(incomeTax, localIncomeTax));
    }

    @Getter
    public static final class Attendance {

        private final Instant checkedInAt;
        private final Instant checkedOutAt;
        private final Boolean isLate;
        private final long lateMinutes;

        private Attendance(WorkerWorkCaseRow row) {
            this.checkedInAt = ApiTimes.toInstant(row.getCheckedInAt());
            this.checkedOutAt = ApiTimes.toInstant(row.getCheckedOutAt());
            this.isLate = row.getCheckedInAt() != null
                    && row.getCheckedInAt().isAfter(row.getStartsAt());
            if (!isLate) {
                this.lateMinutes = 0L;
                return;
            }
            Duration delay = Duration.between(row.getStartsAt(), row.getCheckedInAt());
            long wholeMinutes = delay.toMinutes();
            this.lateMinutes = delay.minusMinutes(wholeMinutes).isZero()
                    ? wholeMinutes
                    : wholeMinutes + 1L;
        }
    }
}
