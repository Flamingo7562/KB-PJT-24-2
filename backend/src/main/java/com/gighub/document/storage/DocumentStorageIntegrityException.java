package com.gighub.document.storage;

/** 계약 문서 파일 쓰기·승격·읽기 과정에서 발견한 무결성 위반을 나타냅니다. */
public class DocumentStorageIntegrityException extends RuntimeException {

    public DocumentStorageIntegrityException(String message) {
        super(message);
    }

    public DocumentStorageIntegrityException(String message, Throwable cause) {
        super(message, cause);
    }
}
