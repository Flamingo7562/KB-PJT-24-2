package com.gighub.document.dto;

import lombok.Builder;
import lombok.Value;

import java.util.List;

/** 문서 Metadata와 Version 목록을 함께 반환하는 문서 상세 응답입니다. */
@Value
@Builder
public class DocumentDetailResponse {

    Document document;
    List<DocumentVersion> versions;

    public static DocumentDetailResponse of(Document document, List<DocumentVersion> versions) {
        return DocumentDetailResponse.builder()
                .document(document)
                .versions(List.copyOf(versions))
                .build();
    }
}
