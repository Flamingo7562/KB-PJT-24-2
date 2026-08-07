package com.gighub.document.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 당사자 파일 접근에 필요한 최소 Version 정보입니다. */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentFileVersion {
    private Long id;
    private Long documentId;
    private Long workCaseId;
    private Integer versionNo;
    private String versionType;
    private String storageKey;
    private String mimeType;
    private byte[] checksum;
}
