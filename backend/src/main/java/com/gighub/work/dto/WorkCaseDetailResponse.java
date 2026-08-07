package com.gighub.work.dto;

import java.time.Instant;
import java.time.LocalDate;

import com.gighub.common.api.ApiTimes;
import com.gighub.work.domain.WorkCaseStatus;
import com.gighub.work.mapper.result.AttendanceSummaryRow;
import com.gighub.work.mapper.result.ContractDetailRow;
import com.gighub.work.mapper.result.EscrowSummaryRow;
import com.gighub.work.mapper.result.LatestInvitationRow;
import com.gighub.work.mapper.result.SettlementSummaryRow;
import com.gighub.work.mapper.result.WorkCaseDetailRow;

import lombok.Getter;

/**
 * 근무 상세 응답입니다.
 *
 * <p>API_SPEC 4.0.0이 고정한 닫힌 필드 집합만 두고, 문서 본문·서명 증거·좌표·인증 반경·
 * 전화번호·Capability는 포함하지 않습니다. {@code worker}·{@code latestInvitation}·
 * {@code contract}·{@code escrow}·{@code settlement}는 근거 행이 없으면 객체 전체가
 * {@code null}이고, {@code attendance}만 항상 객체입니다.</p>
 */
@Getter
public final class WorkCaseDetailResponse {

    private final Long workCaseId;
    private final String title;
    private final LocalDate workDate;
    private final Instant startsAt;
    private final Instant endsAt;
    private final Integer breakMinutes;
    private final Boolean breakPaid;
    private final Long dailyWage;
    private final WorkCaseStatus status;
    private final Integer termsVersion;
    private final String workplaceName;
    private final String workplaceAddress;
    private final WorkerSummary worker;
    private final InvitationSummary latestInvitation;
    private final ContractSummary contract;
    private final AttendanceSummary attendance;
    private final EscrowSummary escrow;
    private final SettlementSummary settlement;

    private WorkCaseDetailResponse(
            WorkCaseDetailRow row,
            LatestInvitationRow invitation,
            ContractDetailRow contract,
            AttendanceSummaryRow attendance,
            EscrowSummaryRow escrow,
            SettlementSummaryRow settlement) {
        this.workCaseId = row.getWorkCaseId();
        this.title = row.getTitle();
        // workDate는 저장 컬럼이 아니라 startsAt에서 파생합니다(API_SPEC 4.0.0).
        this.workDate = row.getStartsAt().toLocalDate();
        this.startsAt = ApiTimes.toInstant(row.getStartsAt());
        this.endsAt = ApiTimes.toInstant(row.getEndsAt());
        this.breakMinutes = row.getBreakMinutes();
        this.breakPaid = row.getBreakPaid();
        this.dailyWage = row.getDailyWage();
        this.status = row.getStatus();
        this.termsVersion = row.getTermsVersion();
        this.workplaceName = row.getWorkplaceName();
        this.workplaceAddress = row.getWorkplaceAddress();
        this.worker = row.getWorkerId() == null
                ? null
                : new WorkerSummary(row.getWorkerId(), row.getWorkerName());
        this.latestInvitation = invitation == null ? null : new InvitationSummary(invitation);
        this.contract = contract == null ? null : new ContractSummary(contract);
        this.attendance = new AttendanceSummary(attendance);
        this.escrow = escrow == null ? null : new EscrowSummary(escrow);
        this.settlement = settlement == null ? null : new SettlementSummary(settlement);
    }

    public static WorkCaseDetailResponse from(
            WorkCaseDetailRow row,
            LatestInvitationRow invitation,
            ContractDetailRow contract,
            AttendanceSummaryRow attendance,
            EscrowSummaryRow escrow,
            SettlementSummaryRow settlement) {
        return new WorkCaseDetailResponse(row, invitation, contract, attendance, escrow, settlement);
    }

    @Getter
    public static final class WorkerSummary {

        private final Long workerId;
        private final String name;

        private WorkerSummary(Long workerId, String name) {
            this.workerId = workerId;
            this.name = name;
        }
    }

    @Getter
    public static final class InvitationSummary {

        private final String status;
        private final Integer termsVersion;
        private final Instant expiresAt;

        private InvitationSummary(LatestInvitationRow row) {
            this.status = row.getStatus();
            this.termsVersion = row.getTermsVersion();
            this.expiresAt = ApiTimes.toInstant(row.getExpiresAt());
        }
    }

    @Getter
    public static final class ContractSummary {

        private final Long contractId;
        private final Long documentId;
        private final Integer sourceTermsVersion;
        private final Instant acceptedAt;

        private ContractSummary(ContractDetailRow row) {
            this.contractId = row.getContractId();
            this.documentId = row.getDocumentId();
            this.sourceTermsVersion = row.getSourceTermsVersion();
            this.acceptedAt = ApiTimes.toInstant(row.getAcceptedAt());
        }
    }

    @Getter
    public static final class AttendanceSummary {

        private final Instant checkedInAt;
        private final Instant checkedOutAt;

        /**
         * 근태 기록이 하나도 없는 근무를 빈 요약으로 받습니다.
         *
         * <p>{@code findAttendanceTimestamps}는 {@code MAX(...)} 집계라 행 자체는 항상
         * 하나지만 두 컬럼이 모두 {@code NULL}입니다. MyBatis는 생성자 resultMap에서 모든
         * 컬럼이 {@code NULL}인 행을 {@code null} 객체로 매핑하므로(기본
         * {@code returnInstanceForEmptyRow=false}) 여기로 {@code null}이 들어옵니다.
         * 출근 전 근무가 대부분이라 이를 막지 않으면 상세 조회가 상시 실패합니다.</p>
         *
         * <p>{@code attendance}만 항상 객체라는 응답 계약은 그대로 유지하고, 값만 비웁니다.</p>
         */
        private AttendanceSummary(AttendanceSummaryRow row) {
            this.checkedInAt = row == null ? null : ApiTimes.toInstant(row.getCheckedInAt());
            this.checkedOutAt = row == null ? null : ApiTimes.toInstant(row.getCheckedOutAt());
        }
    }

    @Getter
    public static final class EscrowSummary {

        private final String status;
        private final Long amount;

        private EscrowSummary(EscrowSummaryRow row) {
            this.status = row.getStatus();
            this.amount = row.getAmount();
        }
    }

    @Getter
    public static final class SettlementSummary {

        private final String status;
        private final Long amount;
        private final Instant dueAt;
        private final Instant completedAt;

        private SettlementSummary(SettlementSummaryRow row) {
            this.status = row.getStatus();
            this.amount = row.getAmount();
            this.dueAt = ApiTimes.toInstant(row.getDueAt());
            this.completedAt = ApiTimes.toInstant(row.getCompletedAt());
        }
    }
}
