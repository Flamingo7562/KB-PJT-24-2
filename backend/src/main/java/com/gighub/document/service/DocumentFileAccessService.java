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
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Arrays;

/**
 * 계약 당사자 파일 접근을 승인 범위대로 검증하고 {@code document_access_logs}에 기록합니다
 * (DOC-011).
 *
 * <p>존재하지 않는 문서는 감사 대상 접근이 아니므로 기록 없이
 * {@link DocumentNotFoundException}을 던진다. 조회된 문서의 상태·서명본·당사자 여부와
 * 파일 무결성에 따른 허용·거부 결정은 {@code document_access_logs}에 남긴다.</p>
 */
@Service
@RequiredArgsConstructor
public class DocumentFileAccessService {

    private static final Logger log = LoggerFactory.getLogger(DocumentFileAccessService.class);

    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String RESULT_ALLOWED = "ALLOWED";
    private static final String RESULT_DENIED = "DENIED";
    private static final String DENIAL_PARTY_ACCESS = "PARTY_ACCESS_DENIED";
    private static final String DENIAL_DOCUMENT_UNAVAILABLE = "DOCUMENT_UNAVAILABLE";
    private static final String DENIAL_SIGNED_VERSION_UNAVAILABLE = "SIGNED_VERSION_UNAVAILABLE";
    private static final String DENIAL_FILE_UNAVAILABLE = "FILE_UNAVAILABLE";
    private static final String DENIAL_CHECKSUM_MISMATCH = "CHECKSUM_MISMATCH";
    private static final String ACTION_VIEW = "CONTRACT_FILE_VIEW";
    private static final String ACTION_DOWNLOAD = "CONTRACT_FILE_DOWNLOAD";
    private static final String CONTRACT_DOCUMENT_TYPE = "EMPLOYMENT_CONTRACT";

    private final DocumentQueryMapper documentQueryMapper;
    private final DocumentAccessMapper documentAccessMapper;
    private final DocumentStorageAdapter storageAdapter;

    public DocumentFileResult loadFile(Long documentId, Long principalUserId, String mode) {
        Document document = documentQueryMapper.findDocumentById(documentId);
        if (document == null) {
            throw new DocumentNotFoundException("문서를 찾을 수 없습니다.");
        }

        DocumentFileVersion version = documentAccessMapper.findSignedVersionForAccess(documentId);
        if (!CONTRACT_DOCUMENT_TYPE.equals(document.getDocumentType())
                || !STATUS_ACTIVE.equals(document.getStatus())) {
            denyIntegrity(documentId, version, principalUserId, mode,
                    DENIAL_DOCUMENT_UNAVAILABLE, "계약 문서를 사용할 수 없습니다.");
        }
        if (version == null) {
            denyIntegrity(documentId, null, principalUserId, mode,
                    DENIAL_SIGNED_VERSION_UNAVAILABLE, "서명된 계약 문서를 사용할 수 없습니다.");
        }

        if (!documentAccessMapper.isContractParty(documentId, principalUserId)) {
            logAccess(documentId, version.getId(), principalUserId, mode,
                    RESULT_DENIED, DENIAL_PARTY_ACCESS);
            throw new DocumentAccessDeniedException("문서에 접근할 권한이 없습니다.");
        }

        byte[] content = readVerifiedContent(version, principalUserId, mode);
        logAccess(documentId, version.getId(), principalUserId, mode, RESULT_ALLOWED, null);

        return DocumentFileResult.builder()
                .content(content)
                .mimeType(version.getMimeType())
                .fileName(fileName(document, version))
                .build();
    }

    private void logAccess(
            Long documentId, Long versionId, Long actorUserId, String mode,
            String result, String denialReason) {
        int inserted = documentAccessMapper.insertAccessLog(DocumentAccessLogParam.builder()
                .documentId(documentId)
                .documentVersionId(versionId)
                .actorUserId(actorUserId)
                .action(actionFor(mode))
                .result(result)
                .denialReason(denialReason)
                .build());
        if (inserted != 1) {
            throw new IllegalStateException("계약 문서 접근 감사를 기록하지 못했습니다.");
        }
    }

    private String actionFor(String mode) {
        return "download".equals(mode) ? ACTION_DOWNLOAD : ACTION_VIEW;
    }

    private byte[] readVerifiedContent(
            DocumentFileVersion version, Long principalUserId, String mode) {
        boolean checksumMismatch = false;

        try {
            if (storageAdapter.exists(version.getStorageKey())) {
                byte[] content = storageAdapter.read(version.getStorageKey());
                if (matchesChecksum(content, version.getChecksum())) {
                    return content;
                }
                checksumMismatch = true;
            }
        } catch (DocumentStorageIntegrityException failure) {
            log.warn("계약 문서 최종 Object를 읽지 못했습니다. documentId={}",
                    version.getDocumentId(), failure);
        }

        String pendingKey = ContractStorageKeys.pendingKey(
                version.getWorkCaseId(), version.getDocumentId(), version.getVersionNo());
        try {
            if (storageAdapter.exists(pendingKey)) {
                byte[] pendingContent = storageAdapter.read(pendingKey);
                if (matchesChecksum(pendingContent, version.getChecksum())) {
                    promoteFallbackQuietly(version, pendingKey);
                    return pendingContent;
                }
                checksumMismatch = true;
            }
        } catch (DocumentStorageIntegrityException failure) {
            log.warn("계약 문서 임시 Object를 읽지 못했습니다. documentId={}",
                    version.getDocumentId(), failure);
        }

        String reason = checksumMismatch
                ? DENIAL_CHECKSUM_MISMATCH : DENIAL_FILE_UNAVAILABLE;
        denyIntegrity(version.getDocumentId(), version, principalUserId, mode, reason,
                "계약 문서 파일 무결성을 확인할 수 없습니다.");
        throw new IllegalStateException("도달할 수 없는 코드입니다.");
    }

    private void promoteFallbackQuietly(DocumentFileVersion version, String pendingKey) {
        try {
            storageAdapter.promote(pendingKey, version.getStorageKey(), version.getChecksum());
        } catch (DocumentStorageIntegrityException failure) {
            // 검증된 임시 Bytes는 반환하되 다음 조회에서도 복구를 다시 시도할 수 있게 남겨 둔다.
            log.warn("계약 문서 조회 중 재승격에 실패했습니다. documentId={}",
                    version.getDocumentId(), failure);
        }
    }

    private boolean matchesChecksum(byte[] content, byte[] expectedChecksum) {
        return Arrays.equals(Sha256.digest(content), expectedChecksum);
    }

    private void denyIntegrity(
            Long documentId, DocumentFileVersion version, Long principalUserId,
            String mode, String reason, String message) {
        logAccess(documentId, version == null ? null : version.getId(), principalUserId,
                mode, RESULT_DENIED, reason);
        throw new DocumentStorageIntegrityException(message);
    }

    // Storage Key나 내부 ID를 노출하지 않는 사용자용 파일명을 만든다.
    private String fileName(Document document, DocumentFileVersion version) {
        String base = CONTRACT_DOCUMENT_TYPE.equals(document.getDocumentType())
                ? "근로계약서" : document.getDocumentType();
        return base + "_v" + version.getVersionNo() + ".pdf";
    }
}
