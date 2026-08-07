package com.gighub.work.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorkCaseTimesTest {

    @Test
    void combinesWithoutShiftingTheWallClockValue() {
        LocalDateTime combined = WorkCaseTimes.combine(
                LocalDate.of(2026, 8, 10),
                LocalTime.of(9, 0)
        );

        assertEquals(LocalDateTime.of(2026, 8, 10, 9, 0), combined);
    }

    @Test
    void doesNotInferNextDayForOvernightInput() {
        LocalDate workDate = LocalDate.of(2026, 8, 10);
        LocalDateTime startsAt = WorkCaseTimes.combine(workDate, LocalTime.of(22, 0));
        LocalDateTime endsAt = WorkCaseTimes.combine(workDate, LocalTime.of(6, 0));

        assertEquals(LocalDateTime.of(2026, 8, 10, 6, 0), endsAt);
        assertFalse(WorkCaseTimes.endsAfterStart(startsAt, endsAt));
    }

    @Test
    void rejectsNullInput() {
        assertThrows(
                NullPointerException.class,
                () -> WorkCaseTimes.combine(null, LocalTime.of(9, 0))
        );
        assertThrows(
                NullPointerException.class,
                () -> WorkCaseTimes.combine(LocalDate.of(2026, 8, 10), null)
        );
    }

    @Test
    void detectsNonPositiveDuration() {
        LocalDateTime startsAt = LocalDateTime.of(2026, 8, 10, 9, 0);

        assertTrue(WorkCaseTimes.endsAfterStart(startsAt, startsAt.plusHours(8)));
        assertFalse(WorkCaseTimes.endsAfterStart(startsAt, startsAt));
        assertFalse(WorkCaseTimes.endsAfterStart(startsAt, startsAt.minusMinutes(1)));
    }
}
