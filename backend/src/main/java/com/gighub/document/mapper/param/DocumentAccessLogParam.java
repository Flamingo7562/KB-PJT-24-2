package com.gighub.document.mapper.param;

import lombok.Builder;
import lombok.Getter;

/** {@code document_access_logs} 삽입 Parameter입니다. */
@Getter
@Builder
public class DocumentAccessLogParam {
    private final Long documentId;
    private final Long documentVersionId;
    private final Long actorUserId;
    private final String action;
    private final String result;
    private final String denialReason;
}
