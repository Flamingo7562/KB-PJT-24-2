package com.gighub.work.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WorkCaseEscrowContext {
    private Long workCaseId;
    private Long employerId;
    private Long workerId;
    private Long agreedWage;
    private String status;
}
