package com.gighub.attendance.mapper.result;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/** READY 진입에 필요한 Aggregate별 검사 결과입니다. */
@Getter
@Setter
public class AttendanceReadinessCheckRow {

    private Boolean workerAssigned;
    private Boolean invitationAccepted;
    private Boolean contractMatched;
    private Boolean escrowHeld;
    private Boolean settlementWaiting;
    private Boolean workplaceLocated;
    private Boolean signedContractRecorded;

    /** 감사 로그에는 좌표나 문서 Key 대신 안정적인 실패 Code만 남깁니다. */
    public List<String> failureReasons() {
        List<String> reasons = new ArrayList<>();
        addIfFalse(reasons, workerAssigned, "WORKER_ASSIGNMENT_MISSING");
        addIfFalse(reasons, invitationAccepted, "ACCEPTED_INVITATION_MISMATCH");
        addIfFalse(reasons, contractMatched, "CONTRACT_SNAPSHOT_MISMATCH");
        addIfFalse(reasons, escrowHeld, "ESCROW_NOT_HELD");
        addIfFalse(reasons, settlementWaiting, "SETTLEMENT_NOT_WAITING");
        addIfFalse(reasons, workplaceLocated, "WORKPLACE_LOCATION_MISSING");
        addIfFalse(reasons, signedContractRecorded, "SIGNED_CONTRACT_METADATA_MISMATCH");
        return List.copyOf(reasons);
    }

    public boolean isComplete() {
        return failureReasons().isEmpty();
    }

    private void addIfFalse(List<String> reasons, Boolean value, String reason) {
        if (!Boolean.TRUE.equals(value)) {
            reasons.add(reason);
        }
    }
}
