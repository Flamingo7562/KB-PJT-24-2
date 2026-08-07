package com.gighub.document.storage;

/** 계약 PDF Storage Key 규칙입니다(DEC-DOCUMENT-STORAGE). */
public final class ContractStorageKeys {

    private ContractStorageKeys() {
    }

    /**
     * DB Commit 전 임시로 쓰는 Object의 Key입니다.
     *
     * <p>최종 Key와 같은 문서·Version 식별자를 사용하되 {@code .pending} 경계에 둡니다.
     * Rollback 정리는 근무 Prefix 아래의 임시 Object만 제거합니다.</p>
     */
    public static String pendingKey(long workCaseId, long documentId, int versionNo) {
        return "contracts/%d/%d/.pending/v%d.pdf"
                .formatted(workCaseId, documentId, versionNo);
    }

    /** 승격 완료된 최종 Object의 Key입니다. */
    public static String finalKey(long workCaseId, long documentId, int versionNo) {
        return "contracts/%d/%d/v%d.pdf".formatted(workCaseId, documentId, versionNo);
    }
}
