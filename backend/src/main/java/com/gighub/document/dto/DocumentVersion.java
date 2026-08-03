package com.gighub.document.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentVersion {
    private Long id;
    private Long documentId;
    private Integer versionNo;
    private String versionType;
    private String storageKey;
    private String mimeType;
    private Long sizeBytes;
    private LocalDateTime createdAt;
}
