package com.gighub.attendance.service;

import com.gighub.document.mapper.ContractDocumentWriteMapper;
import com.gighub.document.mapper.result.ContractVersionPromotionRow;
import com.gighub.document.storage.ContractStorageKeys;
import com.gighub.document.storage.DocumentStorageAdapter;
import com.gighub.document.storage.DocumentStorageIntegrityException;
import com.gighub.document.storage.Sha256;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SignedContractArtifactVerifierTest {

    private static final long WORK_CASE_ID = 17L;
    private static final long DOCUMENT_ID = 23L;
    private static final int VERSION_NO = 2;
    private static final String FINAL_KEY = "contracts/17/23/v2.pdf";

    @Mock
    private ContractDocumentWriteMapper documentMapper;

    @Mock
    private DocumentStorageAdapter storageAdapter;

    @Test
    void acceptsVerifiedPendingFallbackWhenPromotionIsDelayed() {
        byte[] content = "signed-contract".getBytes(StandardCharsets.UTF_8);
        stubSignedRow(Sha256.digest(content));
        when(storageAdapter.read(FINAL_KEY))
                .thenThrow(new DocumentStorageIntegrityException("missing"));
        when(storageAdapter.read(ContractStorageKeys.pendingKey(
                WORK_CASE_ID, DOCUMENT_ID, VERSION_NO)))
                .thenReturn(content);

        assertTrue(verifier().isReadable(WORK_CASE_ID));
    }

    @Test
    void rejectsArtifactWhenFinalAndPendingChecksumsDoNotMatch() {
        stubSignedRow(Sha256.digest("expected".getBytes(StandardCharsets.UTF_8)));
        when(storageAdapter.read(FINAL_KEY))
                .thenReturn("wrong-final".getBytes(StandardCharsets.UTF_8));
        when(storageAdapter.read(ContractStorageKeys.pendingKey(
                WORK_CASE_ID, DOCUMENT_ID, VERSION_NO)))
                .thenReturn("wrong-pending".getBytes(StandardCharsets.UTF_8));

        assertFalse(verifier().isReadable(WORK_CASE_ID));
    }

    private void stubSignedRow(byte[] checksum) {
        when(documentMapper.findPromotionRowsByWorkCaseId(WORK_CASE_ID))
                .thenReturn(List.of(ContractVersionPromotionRow.builder()
                        .documentId(DOCUMENT_ID)
                        .versionNo(VERSION_NO)
                        .versionType("SIGNED")
                        .storageKey(FINAL_KEY)
                        .checksum(checksum)
                        .build()));
    }

    private SignedContractArtifactVerifier verifier() {
        return new SignedContractArtifactVerifier(documentMapper, storageAdapter);
    }
}
