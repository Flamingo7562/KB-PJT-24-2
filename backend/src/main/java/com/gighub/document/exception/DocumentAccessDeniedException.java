package com.gighub.document.exception;

import com.gighub.common.exception.ForbiddenException;

/** 문서 당사자가 아닌 Principal의 파일 접근을 나타냅니다(DOC-011). */
public class DocumentAccessDeniedException extends ForbiddenException {

    public DocumentAccessDeniedException(String message) {
        super(message);
    }
}
