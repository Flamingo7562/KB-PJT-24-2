package com.gighub.document.service;

import lombok.Builder;
import lombok.Getter;

/** 당사자에게 스트리밍할 계약 파일 내용과 응답 Header 값입니다. */
@Getter
@Builder
public class DocumentFileResult {
    private final byte[] content;
    private final String mimeType;
    private final String fileName;
}
