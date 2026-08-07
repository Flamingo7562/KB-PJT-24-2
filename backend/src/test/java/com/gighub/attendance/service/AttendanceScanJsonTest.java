package com.gighub.attendance.service;

import com.gighub.attendance.dto.AttendanceRecordedResponse;
import com.gighub.attendance.dto.AttendanceScanResponse;
import com.gighub.attendance.service.impl.AttendanceScanJson;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AttendanceScanJsonTest {

    @Test
    void preservesApprovedFieldNamesAndNullableSuccessFieldsForReplay() {
        AttendanceScanJson json = new AttendanceScanJson();
        AttendanceRecordedResponse response = new AttendanceRecordedResponse(
                17L,
                "CHECK_IN",
                Instant.parse("2026-08-07T01:00:05Z"),
                true,
                1,
                null,
                null);

        String stored = json.writeResponseBody(response);
        AttendanceScanResponse replayed = json.readResponseBody(stored);

        assertTrue(stored.contains("\"isLate\":true"));
        assertTrue(stored.contains("\"earlyCheckoutConfirmedAt\":null"));
        assertTrue(stored.contains("\"settlementDueAt\":null"));
        assertEquals("CHECK_IN", replayed.getScanType());
        assertEquals(17L, replayed.getWorkCaseId());
    }
}
