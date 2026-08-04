package com.gighub.document.dto;

import lombok.Getter;

import java.util.List;
import java.util.Objects;

/**
 * 문서 공유 현황 목록 응답입니다.
 *
 * <p>승인 명세의 공유 응답 필드 구조는 아직 확정되지 않았습니다. 이 클래스는 기존
 * {@code items} payload를 그대로 유지한 채 Envelope만 명시적 타입으로 바꿉니다. 페이지가
 * 필요해지면 공통 {@code PageResponse}로 교체합니다.</p>
 *
 * <p>공개 Builder를 두면 {@code of()}의 방어 복사를 건너뛰고 호출자가 들고 있는 가변
 * 목록을 그대로 담을 수 있으므로, {@code PageResponse}처럼 생성자에서 복사하고 생성 경로를
 * {@code of()} 하나로 둡니다.</p>
 */
@Getter
public final class DocumentShareListResponse {

    private final List<DocumentShare> items;

    private DocumentShareListResponse(List<DocumentShare> items) {
        this.items = List.copyOf(Objects.requireNonNull(items, "items"));
    }

    public static DocumentShareListResponse of(List<DocumentShare> items) {
        return new DocumentShareListResponse(items);
    }
}
