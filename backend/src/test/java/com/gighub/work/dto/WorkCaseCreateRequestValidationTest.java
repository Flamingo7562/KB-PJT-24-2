package com.gighub.work.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorkCaseCreateRequestValidationTest {

    private static final String TITLE = "주말 홀 서빙";
    private static final LocalDate WORK_DATE = LocalDate.of(2026, 8, 10);
    private static final LocalTime START_TIME = LocalTime.of(9, 0);
    private static final LocalTime END_TIME = LocalTime.of(18, 0);
    private static final Integer BREAK_MINUTES = 60;
    private static final Boolean BREAK_PAID = Boolean.FALSE;
    private static final Long DAILY_WAGE = 120_000L;

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void acceptsApprovedInputAndTrimsTitle() {
        WorkCaseCreateRequest request = withTitle("  주말 홀 서빙  ");

        assertTrue(validator.validate(request).isEmpty());
        assertEquals(TITLE, request.getTitle());
    }

    /** 생성자 인자 순서가 뒤바뀌면 여기서 잡힙니다. 두 시각은 타입이 같아 컴파일러가 못 잡습니다. */
    @Test
    void keepsStartAndEndTimeInDeclaredOrder() {
        WorkCaseCreateRequest request = valid();

        assertEquals(START_TIME, request.getStartTime());
        assertEquals(END_TIME, request.getEndTime());
    }

    @Test
    void rejectsBlankTitleLikeMissingTitle() {
        assertHasViolation(withTitle("   "));
        assertHasViolation(withTitle(null));
    }

    @Test
    void rejectsNonPositiveWage() {
        assertHasViolation(withDailyWage(0L));
        assertHasViolation(withDailyWage(-1L));
    }

    @Test
    void rejectsBreakMinutesOutsideColumnRange() {
        assertHasViolation(withBreakMinutes(-1));
        assertHasViolation(withBreakMinutes(65536));
        assertTrue(validator.validate(withBreakMinutes(0)).isEmpty());
    }

    @Test
    void requiresEveryApprovedField() {
        assertHasViolation(withWorkDate(null));
        assertHasViolation(withTimes(null, END_TIME));
        assertHasViolation(withTimes(START_TIME, null));
        assertHasViolation(withBreakMinutes(null));
        assertHasViolation(withBreakPaid(null));
        assertHasViolation(withDailyWage(null));
    }

    @Test
    void rejectsServerOwnedFields() {
        assertThrows(
                IllegalArgumentException.class,
                () -> valid().rejectUnknownField("status", "ACCEPTED")
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> valid().rejectUnknownField("termsVersion", 5)
        );
    }

    private void assertHasViolation(WorkCaseCreateRequest request) {
        Set<ConstraintViolation<WorkCaseCreateRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
    }

    private WorkCaseCreateRequest valid() {
        return withTitle(TITLE);
    }

    private WorkCaseCreateRequest withTitle(String title) {
        return new WorkCaseCreateRequest(
                title, WORK_DATE, START_TIME, END_TIME, BREAK_MINUTES, BREAK_PAID, DAILY_WAGE);
    }

    private WorkCaseCreateRequest withWorkDate(LocalDate workDate) {
        return new WorkCaseCreateRequest(
                TITLE, workDate, START_TIME, END_TIME, BREAK_MINUTES, BREAK_PAID, DAILY_WAGE);
    }

    private WorkCaseCreateRequest withTimes(LocalTime startTime, LocalTime endTime) {
        return new WorkCaseCreateRequest(
                TITLE, WORK_DATE, startTime, endTime, BREAK_MINUTES, BREAK_PAID, DAILY_WAGE);
    }

    private WorkCaseCreateRequest withBreakMinutes(Integer breakMinutes) {
        return new WorkCaseCreateRequest(
                TITLE, WORK_DATE, START_TIME, END_TIME, breakMinutes, BREAK_PAID, DAILY_WAGE);
    }

    private WorkCaseCreateRequest withBreakPaid(Boolean breakPaid) {
        return new WorkCaseCreateRequest(
                TITLE, WORK_DATE, START_TIME, END_TIME, BREAK_MINUTES, breakPaid, DAILY_WAGE);
    }

    private WorkCaseCreateRequest withDailyWage(Long dailyWage) {
        return new WorkCaseCreateRequest(
                TITLE, WORK_DATE, START_TIME, END_TIME, BREAK_MINUTES, BREAK_PAID, dailyWage);
    }
}
