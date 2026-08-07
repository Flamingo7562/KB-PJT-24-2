package com.gighub.work.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WorkCaseAddressTest {

    @Test
    void combinesRoadAndDetailAddressWithSingleSpace() {
        String combined = WorkCaseAddress.combine("서울 강남구 테헤란로 1", "2층");

        assertEquals("서울 강남구 테헤란로 1 2층", combined);
    }

    @Test
    void trimsBothPartsBeforeCombining() {
        String combined = WorkCaseAddress.combine("  서울 강남구 테헤란로 1  ", "  2층  ");

        assertEquals("서울 강남구 테헤란로 1 2층", combined);
    }

    @Test
    void returnsRoadAddressOnlyWhenDetailIsNull() {
        String combined = WorkCaseAddress.combine("서울 강남구 테헤란로 1", null);

        assertEquals("서울 강남구 테헤란로 1", combined);
    }

    @Test
    void returnsRoadAddressOnlyWhenDetailIsBlank() {
        String combined = WorkCaseAddress.combine("서울 강남구 테헤란로 1", "   ");

        assertEquals("서울 강남구 테헤란로 1", combined);
    }

    @Test
    void rejectsNullRoadAddress() {
        assertThrows(NullPointerException.class, () -> WorkCaseAddress.combine(null, "2층"));
    }
}
