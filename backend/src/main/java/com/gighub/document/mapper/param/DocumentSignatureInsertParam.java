package com.gighub.document.mapper.param;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/** {@code document_signatures} 삽입 Parameter입니다. */
@Getter
@Builder
public class DocumentSignatureInsertParam {
    private final Long documentId;
    private final Long sourceVersionId;
    private final Long signedVersionId;
    private final Long signerUserId;
    private final byte[] sourceChecksum;
    private final byte[] signedChecksum;
    private final String typedName;
    private final String signatureMethod;
    private final LocalDateTime consentedAt;
    private final LocalDateTime signedAt;
}
