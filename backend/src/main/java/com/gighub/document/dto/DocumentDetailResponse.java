package com.gighub.document.dto;

import lombok.Getter;

import java.util.List;
import java.util.Objects;

/**
 * 문서 Metadata와 Version 목록을 함께 반환하는 문서 상세 응답입니다.
 *
 * <p>공개 Builder를 두면 {@code of()}의 방어 복사를 건너뛰고 호출자가 들고 있는 가변
 * 목록을 그대로 담을 수 있으므로, {@code PageResponse}처럼 생성자에서 복사하고 생성 경로를
 * {@code of()} 하나로 둡니다.</p>
 */
@Getter
public final class DocumentDetailResponse {

    private final Document document;
    private final List<DocumentVersion> versions;

    private DocumentDetailResponse(Document document, List<DocumentVersion> versions) {
        this.document = Objects.requireNonNull(document, "document");
        this.versions = List.copyOf(Objects.requireNonNull(versions, "versions"));
    }

    public static DocumentDetailResponse of(Document document, List<DocumentVersion> versions) {
        return new DocumentDetailResponse(document, versions);
    }
}
