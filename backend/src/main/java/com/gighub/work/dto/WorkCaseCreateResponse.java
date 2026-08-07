package com.gighub.work.dto;

import lombok.Getter;

/** 근무 {@code DRAFT} 등록 성공 후 생성된 근무 Case 식별자입니다. */
@Getter
public final class WorkCaseCreateResponse {

    private final Long workCaseId;

    public WorkCaseCreateResponse(Long workCaseId) {
        this.workCaseId = workCaseId;
    }
}
