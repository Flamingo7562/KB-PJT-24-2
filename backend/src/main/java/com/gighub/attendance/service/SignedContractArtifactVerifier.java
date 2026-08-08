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
import java.util.Comparator;

/** READY 전이 전에 최종 서명 계약 파일의 존재와 Checksum을 검증합니다. */
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
                .max(Comparator.comparing(ContractVersionPromotionRow::getVersionNo))
                .map(row -> isReadable(workCaseId, row))
                .orElse(false);
    }

    private boolean isReadable(long workCaseId, ContractVersionPromotionRow row) {
        if (matches(row.getStorageKey(), row.getChecksum())) {
            return true;
        }

        // DB Commit 뒤 파일 승격이 지연된 짧은 구간은 동일 Checksum의 pending 파일로 복구합니다.
        String pendingKey = ContractStorageKeys.pendingKey(
                workCaseId, row.getDocumentId(), row.getVersionNo());
        return matches(pendingKey, row.getChecksum());
    }

    private boolean matches(String storageKey, byte[] expectedChecksum) {
        try {
            return Arrays.equals(Sha256.digest(storageAdapter.read(storageKey)), expectedChecksum);
        } catch (RuntimeException failure) {
            log.debug("READY 계약 파일 검증을 다음 주기로 미룹니다.", failure);
            return false;
        }
    }
}
