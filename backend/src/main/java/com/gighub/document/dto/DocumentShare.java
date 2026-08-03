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
public class DocumentShare {
    private Long id;
    private Long documentId;
    private Long workCaseId;
    private Long sharedWithUserId;
    private String status;
    private LocalDateTime createdAt;
}
