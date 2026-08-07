package com.gighub.document.service;

import com.gighub.document.dto.Document;
import com.gighub.document.dto.DocumentFileVersion;
import com.gighub.document.exception.DocumentAccessDeniedException;
import com.gighub.document.exception.DocumentNotFoundException;
import com.gighub.document.mapper.DocumentAccessMapper;
import com.gighub.document.mapper.DocumentQueryMapper;
import com.gighub.document.mapper.param.DocumentAccessLogParam;
import com.gighub.document.storage.ContractStorageKeys;
import com.gighub.document.storage.DocumentStorageAdapter;
import com.gighub.document.storage.DocumentStorageIntegrityException;
import com.gighub.document.storage.Sha256;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentFileAccessServiceTest {

    private static final Long DOCUMENT_ID = 10L;
    private static final Long WORK_CASE_ID = 1L;
    private static final Long OWNER_ID = 3L;
    private static final Long PARTY_ID = 4L;
    private static final Long STRANGER_ID = 99L;
    private static final byte[] SIGNED_CONTENT = new byte[]{1, 2, 3};
    private static final String FINAL_KEY = "contracts/1/10/v2.pdf";
    private static final String PENDING_KEY =
            ContractStorageKeys.pendingKey(WORK_CASE_ID, DOCUMENT_ID, 2);

    @Mock
    private DocumentQueryMapper documentQueryMapper;
    @Mock
    private DocumentAccessMapper documentAccessMapper;
    @Mock
    private DocumentStorageAdapter storageAdapter;

    private DocumentFileAccessService service;

    @BeforeEach
    void setUp() {
        service = new DocumentFileAccessService(
                documentQueryMapper, documentAccessMapper, storageAdapter);
        lenient().when(documentAccessMapper.insertAccessLog(any())).thenReturn(1);
    }

    private Document document(String status) {
        return Document.builder()
                .id(DOCUMENT_ID)
                .ownerUserId(OWNER_ID)
                .workCaseId(WORK_CASE_ID)
                .documentType("EMPLOYMENT_CONTRACT")
                .status(status)
                .build();
    }

    private DocumentFileVersion signedVersion() {
        return DocumentFileVersion.builder()
                .id(55L)
                .documentId(DOCUMENT_ID)
                .workCaseId(WORK_CASE_ID)
                .versionNo(2)
                .versionType("SIGNED")
                .storageKey(FINAL_KEY)
                .mimeType("application/pdf")
                .checksum(Sha256.digest(SIGNED_CONTENT))
                .build();
    }

    @Test
    void contractPartyLoadsChecksumVerifiedSignedVersionAndAllowedAccessIsLogged() {
        givenActiveSignedDocument(OWNER_ID);
        when(storageAdapter.exists(FINAL_KEY)).thenReturn(true);
        when(storageAdapter.read(FINAL_KEY)).thenReturn(SIGNED_CONTENT);

        DocumentFileResult result = service.loadFile(DOCUMENT_ID, OWNER_ID, "view");

        assertArrayEquals(SIGNED_CONTENT, result.getContent());
        assertEquals("application/pdf", result.getMimeType());
        assertEquals("근로계약서_v2.pdf", result.getFileName());

        DocumentAccessLogParam log = capturedAccessLog();
        assertEquals("ALLOWED", log.getResult());
        assertEquals("CONTRACT_FILE_VIEW", log.getAction());
        assertEquals(55L, log.getDocumentVersionId());
        assertEquals(OWNER_ID, log.getActorUserId());
    }

    @Test
    void workerPartyGetsTheSameSignedBytesForDownload() {
        givenActiveSignedDocument(PARTY_ID);
        when(storageAdapter.exists(FINAL_KEY)).thenReturn(true);
        when(storageAdapter.read(FINAL_KEY)).thenReturn(SIGNED_CONTENT);

        DocumentFileResult result = service.loadFile(DOCUMENT_ID, PARTY_ID, "download");

        assertArrayEquals(SIGNED_CONTENT, result.getContent());
        assertEquals("CONTRACT_FILE_DOWNLOAD", capturedAccessLog().getAction());
    }

    @Test
    void verifiedPendingBytesAreReturnedAndPromotionIsRetried() {
        givenActiveSignedDocument(OWNER_ID);
        when(storageAdapter.exists(FINAL_KEY)).thenReturn(false);
        when(storageAdapter.exists(PENDING_KEY)).thenReturn(true);
        when(storageAdapter.read(PENDING_KEY)).thenReturn(SIGNED_CONTENT);

        DocumentFileResult result = service.loadFile(DOCUMENT_ID, OWNER_ID, "view");

        assertArrayEquals(SIGNED_CONTENT, result.getContent());
        verify(storageAdapter).promote(
                PENDING_KEY, FINAL_KEY, Sha256.digest(SIGNED_CONTENT));
        assertEquals("ALLOWED", capturedAccessLog().getResult());
    }

    @Test
    void validPendingBytesRemainAvailableWhenRepromotionFails() {
        givenActiveSignedDocument(OWNER_ID);
        when(storageAdapter.exists(FINAL_KEY)).thenReturn(false);
        when(storageAdapter.exists(PENDING_KEY)).thenReturn(true);
        when(storageAdapter.read(PENDING_KEY)).thenReturn(SIGNED_CONTENT);
        doThrow(new DocumentStorageIntegrityException("boom"))
                .when(storageAdapter).promote(any(), any(), any());

        DocumentFileResult result = service.loadFile(DOCUMENT_ID, OWNER_ID, "view");

        assertArrayEquals(SIGNED_CONTENT, result.getContent());
        assertEquals("ALLOWED", capturedAccessLog().getResult());
    }

    @Test
    void checksumMismatchIsDeniedAndNothingIsStreamed() {
        givenActiveSignedDocument(OWNER_ID);
        when(storageAdapter.exists(FINAL_KEY)).thenReturn(true);
        when(storageAdapter.read(FINAL_KEY)).thenReturn(new byte[]{9});
        when(storageAdapter.exists(PENDING_KEY)).thenReturn(false);

        assertThrows(DocumentStorageIntegrityException.class,
                () -> service.loadFile(DOCUMENT_ID, OWNER_ID, "view"));

        DocumentAccessLogParam log = capturedAccessLog();
        assertEquals("DENIED", log.getResult());
        assertEquals("CHECKSUM_MISMATCH", log.getDenialReason());
    }

    @Test
    void missingFinalAndPendingFilesAreDeniedAsUnavailable() {
        givenActiveSignedDocument(OWNER_ID);
        when(storageAdapter.exists(FINAL_KEY)).thenReturn(false);
        when(storageAdapter.exists(PENDING_KEY)).thenReturn(false);

        assertThrows(DocumentStorageIntegrityException.class,
                () -> service.loadFile(DOCUMENT_ID, OWNER_ID, "download"));

        DocumentAccessLogParam log = capturedAccessLog();
        assertEquals("FILE_UNAVAILABLE", log.getDenialReason());
        assertEquals("CONTRACT_FILE_DOWNLOAD", log.getAction());
    }

    @Test
    void strangerIsDeniedBeforeStorageAccess() {
        when(documentQueryMapper.findDocumentById(DOCUMENT_ID)).thenReturn(document("ACTIVE"));
        when(documentAccessMapper.findSignedVersionForAccess(DOCUMENT_ID))
                .thenReturn(signedVersion());
        when(documentAccessMapper.isContractParty(DOCUMENT_ID, STRANGER_ID)).thenReturn(false);

        assertThrows(DocumentAccessDeniedException.class,
                () -> service.loadFile(DOCUMENT_ID, STRANGER_ID, "view"));

        verify(storageAdapter, never()).read(any());
        DocumentAccessLogParam log = capturedAccessLog();
        assertEquals("PARTY_ACCESS_DENIED", log.getDenialReason());
    }

    @Test
    void inactiveDocumentIsAuditedAsUnavailable() {
        when(documentQueryMapper.findDocumentById(DOCUMENT_ID)).thenReturn(document("SIGNED"));
        when(documentAccessMapper.findSignedVersionForAccess(DOCUMENT_ID))
                .thenReturn(signedVersion());

        assertThrows(DocumentStorageIntegrityException.class,
                () -> service.loadFile(DOCUMENT_ID, OWNER_ID, "view"));

        DocumentAccessLogParam log = capturedAccessLog();
        assertEquals("DOCUMENT_UNAVAILABLE", log.getDenialReason());
        assertEquals(55L, log.getDocumentVersionId());
    }

    @Test
    void missingSignedVersionIsAuditedWithoutFallingBackToOriginal() {
        when(documentQueryMapper.findDocumentById(DOCUMENT_ID)).thenReturn(document("ACTIVE"));
        when(documentAccessMapper.findSignedVersionForAccess(DOCUMENT_ID)).thenReturn(null);

        assertThrows(DocumentStorageIntegrityException.class,
                () -> service.loadFile(DOCUMENT_ID, OWNER_ID, "view"));

        DocumentAccessLogParam log = capturedAccessLog();
        assertEquals("SIGNED_VERSION_UNAVAILABLE", log.getDenialReason());
        assertEquals(null, log.getDocumentVersionId());
        verify(storageAdapter, never()).read(any());
    }

    @Test
    void missingDocumentDoesNotCreateAFakeAuditRow() {
        when(documentQueryMapper.findDocumentById(DOCUMENT_ID)).thenReturn(null);

        assertThrows(DocumentNotFoundException.class,
                () -> service.loadFile(DOCUMENT_ID, OWNER_ID, "view"));

        verify(documentAccessMapper, never()).insertAccessLog(any());
    }

    @Test
    void auditFailurePreventsAFileResult() {
        givenActiveSignedDocument(OWNER_ID);
        when(storageAdapter.exists(FINAL_KEY)).thenReturn(true);
        when(storageAdapter.read(FINAL_KEY)).thenReturn(SIGNED_CONTENT);
        when(documentAccessMapper.insertAccessLog(any())).thenReturn(0);

        assertThrows(IllegalStateException.class,
                () -> service.loadFile(DOCUMENT_ID, OWNER_ID, "view"));
    }

    private void givenActiveSignedDocument(Long principalUserId) {
        when(documentQueryMapper.findDocumentById(DOCUMENT_ID)).thenReturn(document("ACTIVE"));
        when(documentAccessMapper.findSignedVersionForAccess(DOCUMENT_ID))
                .thenReturn(signedVersion());
        when(documentAccessMapper.isContractParty(DOCUMENT_ID, principalUserId)).thenReturn(true);
    }

    private DocumentAccessLogParam capturedAccessLog() {
        ArgumentCaptor<DocumentAccessLogParam> captor =
                ArgumentCaptor.forClass(DocumentAccessLogParam.class);
        verify(documentAccessMapper).insertAccessLog(captor.capture());
        return captor.getValue();
    }
}
