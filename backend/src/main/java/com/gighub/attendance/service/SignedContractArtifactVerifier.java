package com.gighub.attendance.service;

import com.gighub.document.mapper.ContractDocumentWriteMapper;
import com.gighub.document.mapper.result.ContractVersionPromotionRow;
import com.gighub.document.storage.ContractStorageKeys;
import com.gighub.document.storage.DocumentStorageAdapter;
import com.gighub.document.storage.Sha256;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/** READY 전이 전에 서명 계약서 파일을 실제로 읽고 Checksum을 검증합니다. */
@Component
public class SignedContractArtifactVerifier {

    private static final Logger log = LoggerFactory.getLogger(SignedContractArtifactVerifier.class);
    private static final String SIGNED = "SIGNED";

    private final ContractDocumentWriteMapper documentMapper;
    private final DocumentStorageAdapter storageAdapter;

    public SignedContractArtifactVerifier(
            ContractDocumentWriteMapper documentMapper,
            DocumentStorageAdapter storageAdapter) {
        this.documentMapper = documentMapper;
        this.storageAdapter = storageAdapter;
    }

    public boolean isReadable(long workCaseId) {
        return documentMapper.findPromotionRowsByWorkCaseId(workCaseId).stream()
                .filter(row -> SIGNED.equals(row.getVersionType()))
                .anyMatch(row -> isReadable(workCaseId, row));
    }

    private boolean isReadable(long workCaseId, ContractVersionPromotionRow row) {
        if (matches(row.getStorageKey(), row.getChecksum())) {
            return true;
        }

        String pendingKey = ContractStorageKeys.pendingKey(
                workCaseId, row.getDocumentId(), row.getVersionNo());
        return matches(pendingKey, row.getChecksum());
    }

    private boolean matches(String storageKey, byte[] expectedChecksum) {
        try {
            return Arrays.equals(Sha256.digest(storageAdapter.read(storageKey)), expectedChecksum);
        } catch (RuntimeException failure) {
            // 승격 지연이나 일시적인 저장소 장애는 근무를 잘못 READY로 만들지 않고 다음 주기에 재시도합니다.
            log.debug("READY 계약서 파일 검증을 다음 주기로 미룹니다. storageKey={}", storageKey, failure);
            return false;
        }
    }
}
