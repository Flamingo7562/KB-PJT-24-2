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
public class Document {
    private Long id;
    private Long createdByUserId;
    private Long ownerUserId;
    private Long workCaseId;
    private String documentType;
    private String status;
    private LocalDate issuedOn;
    private LocalDate expiresOn;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
