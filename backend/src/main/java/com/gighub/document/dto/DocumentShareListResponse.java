package com.gighub.document.dto;

import lombok.Builder;
import lombok.Value;

import java.util.List;

/**
 * 문서 공유 현황 목록 응답입니다.
 *
 * <p>승인 명세의 공유 응답 필드 구조는 아직 확정되지 않았습니다. 이 클래스는 기존
 * {@code items} payload를 그대로 유지한 채 Envelope만 명시적 타입으로 바꿉니다. 페이지가
 * 필요해지면 공통 {@code PageResponse}로 교체합니다.</p>
 */
@Value
@Builder
public class DocumentShareListResponse {

    List<DocumentShare> items;

    public static DocumentShareListResponse of(List<DocumentShare> items) {
        return DocumentShareListResponse.builder().items(List.copyOf(items)).build();
    }
}
