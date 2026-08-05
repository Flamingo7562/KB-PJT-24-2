package com.gighub.workplace.dto;

import lombok.Getter;

/** 사업장 등록 성공 후 생성된 사업장 식별자입니다. */
@Getter
public final class WorkplaceCreateResponse {

    private final Long workplaceId;

    public WorkplaceCreateResponse(Long workplaceId) {
        this.workplaceId = workplaceId;
    }
}
