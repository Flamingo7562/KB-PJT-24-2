package com.gighub.contract;

/**
 * 임시 저장까지 마친 계약서 파일을 Commit 뒤에 승격시키기 위한 표식입니다.
 *
 * <p>저장 Key와 Checksum은 담지 않습니다. 그 값들은 구현과 저장소만 알아야 하고, 호출부를
 * 거치면 로그나 응답으로 흘러갈 수 있습니다. 호출부는 "무엇을 승격시킬지"만 들고 있으면
 * 됩니다.</p>
 */
public final class ContractArtifactHandle {

    private static final ContractArtifactHandle NOTHING = new ContractArtifactHandle(0L, 0L);

    private final long workCaseId;
    private final long contractId;

    private ContractArtifactHandle(long workCaseId, long contractId) {
        this.workCaseId = workCaseId;
        this.contractId = contractId;
    }

    public static ContractArtifactHandle of(long workCaseId, long contractId) {
        return new ContractArtifactHandle(workCaseId, contractId);
    }

    /** 승격할 파일이 없는 경우입니다. 호출부가 {@code null}을 다루지 않게 합니다. */
    public static ContractArtifactHandle nothing() {
        return NOTHING;
    }

    public boolean isNothing() {
        return this == NOTHING;
    }

    public long getWorkCaseId() {
        return workCaseId;
    }

    public long getContractId() {
        return contractId;
    }
}
