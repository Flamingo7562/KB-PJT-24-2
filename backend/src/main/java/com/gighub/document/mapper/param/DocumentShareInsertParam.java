package com.gighub.document.mapper.param;

import lombok.Builder;
import lombok.Getter;

/** {@code document_shares} 삽입 Parameter입니다. */
@Getter
@Builder
public class DocumentShareInsertParam {
    private final Long documentId;
    private final Long workCaseId;
    private final Long sharedWithUserId;
    private final String purpose;
}
