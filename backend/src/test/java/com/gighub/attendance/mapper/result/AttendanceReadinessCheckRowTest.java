package com.gighub.attendance.mapper.result;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AttendanceReadinessCheckRowTest {

    @Test
    void reportsOnlyIncompleteAggregateReasons() {
        AttendanceReadinessCheckRow row = completeRow();
        row.setWorkerAssigned(false);
        row.setEscrowHeld(false);
        row.setWorkplaceLocated(null);

        assertEquals(
                List.of(
                        "WORKER_ASSIGNMENT_MISSING",
                        "ESCROW_NOT_HELD",
                        "WORKPLACE_LOCATION_MISSING"),
                row.failureReasons());
    }

    @Test
    void completeAggregateHasNoFailureReason() {
        AttendanceReadinessCheckRow row = completeRow();

        assertTrue(row.isComplete());
        assertTrue(row.failureReasons().isEmpty());
    }

    private AttendanceReadinessCheckRow completeRow() {
        AttendanceReadinessCheckRow row = new AttendanceReadinessCheckRow();
        row.setWorkerAssigned(true);
        row.setInvitationAccepted(true);
        row.setContractMatched(true);
        row.setEscrowHeld(true);
        row.setSettlementWaiting(true);
        row.setWorkplaceLocated(true);
        row.setSignedContractRecorded(true);
        return row;
    }
}
