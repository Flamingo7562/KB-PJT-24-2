package com.gighub.contract;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 계약서 파일을 만들기 위해 수락 Aggregate가 넘기는 최소 정보입니다.
 *
 * <p>조건 값을 그대로 넘기지 않습니다. 계약 Snapshot은 이미
 * {@code work_contracts}에 저장돼 있으므로 구현이 그 행을 읽으면 됩니다. 같은 값을 두 경로로
 * 전달하면 어느 쪽이 진짜 계약 내용인지 흐려집니다.</p>
 */
public final class ContractArtifactCommand {

    private final long workCaseId;
    private final long contractId;
    private final LocalDateTime acceptedAt;

    private ContractArtifactCommand(
            long workCaseId, long contractId, LocalDateTime acceptedAt) {
        this.workCaseId = workCaseId;
        this.contractId = contractId;
        this.acceptedAt = Objects.requireNonNull(acceptedAt, "acceptedAt");
    }

    public static ContractArtifactCommand of(
            long workCaseId, long contractId, LocalDateTime acceptedAt) {
        return new ContractArtifactCommand(workCaseId, contractId, acceptedAt);
    }

    public long getWorkCaseId() {
        return workCaseId;
    }

    public long getContractId() {
        return contractId;
    }

    /** Aggregate 전체가 공유하는 수락 시각입니다. 문서·서명 시각도 이 값을 씁니다. */
    public LocalDateTime getAcceptedAt() {
        return acceptedAt;
    }
}
