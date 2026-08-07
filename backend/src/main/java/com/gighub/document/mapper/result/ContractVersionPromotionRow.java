package com.gighub.document.mapper.result;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Commit 뒤 승격 대상 하나를 가리키는 최소 정보입니다. */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContractVersionPromotionRow {
    private Long documentId;
    private Integer versionNo;
    private String versionType;
    private String storageKey;
    private byte[] checksum;
}
