package com.gighub.document.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentListItem {
    private Long documentId;
    private String type;
    private String status;
    private LocalDate issuedOn;
    private LocalDate expiresOn;
    private String ownerName;
    private String source;
    private Long workCaseId;
    private LocalDateTime createdAt;
}
