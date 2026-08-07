package com.gighub.work.dto;

import lombok.Getter;

@Getter
public final class WorkerHomeResponse {

    private final WorkerWorkCaseResponse todayWorkCase;

    public WorkerHomeResponse(WorkerWorkCaseResponse todayWorkCase) {
        this.todayWorkCase = todayWorkCase;
    }
}
