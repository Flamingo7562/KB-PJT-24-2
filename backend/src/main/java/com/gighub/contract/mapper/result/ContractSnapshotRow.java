package com.gighub.contract.mapper.result;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 계약서 파일을 만드는 데 필요한 {@code work_contracts} 최소 조회 결과입니다. */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContractSnapshotRow {
    private Long workCaseId;
    private Long employerId;
    private Long workerId;
    private String termsSnapshotJson;
}
