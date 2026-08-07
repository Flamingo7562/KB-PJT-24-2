package com.gighub.document.contract;

/** 근로계약서 PDF 렌더링 실패를 나타냅니다. */
public class ContractDocumentGenerationException extends RuntimeException {

    public ContractDocumentGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}
