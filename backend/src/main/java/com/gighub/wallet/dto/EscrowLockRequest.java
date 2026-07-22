package com.gighub.wallet.dto;

import java.math.BigDecimal;

public class EscrowLockRequest {
    private Long employerId; // 사장님 ID
    private Long workerId;   // 알바생 ID (어느 알바생과의 계약인지 이력용)
    private Long workCaseId; // 근무 건 ID
    private BigDecimal amount; // 합의된 일급 (agreed_wage)

    // 중복 결제 방지를 위한 고유 식별자(프론트에서 생성해서 보냄)
    private String idempotencyKey;

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public Long getEmployerId() {
        return employerId;
    }

    public void setEmployerId(Long employerId) {
        this.employerId = employerId;
    }

    public Long getWorkerId() {
        return workerId;
    }

    public void setWorkerId(Long workerId) {
        this.workerId = workerId;
    }

    public Long getWorkCaseId() {
        return workCaseId;
    }

    public void setWorkCaseId(Long workCaseId) {
        this.workCaseId = workCaseId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}
