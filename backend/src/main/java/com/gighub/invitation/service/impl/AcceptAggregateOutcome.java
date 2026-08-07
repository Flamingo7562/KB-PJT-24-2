package com.gighub.invitation.service.impl;

import com.gighub.contract.ContractArtifactHandle;
import com.gighub.invitation.dto.InvitationAcceptResponse;

import java.util.Objects;

/**
 * Commit된 수락의 결과와, Commit 뒤에 이어서 처리할 계약서 파일 표식입니다.
 *
 * <p>파일 승격은 Transaction 밖에서 일어나야 해서 실행기가 응답만 돌려주면 승격 대상을 알 수
 * 없습니다. 두 값을 함께 돌려주고 호출부가 순서를 지킵니다.</p>
 */
final class AcceptAggregateOutcome {

    private final InvitationAcceptResponse response;
    private final ContractArtifactHandle artifact;

    AcceptAggregateOutcome(
            InvitationAcceptResponse response, ContractArtifactHandle artifact) {
        this.response = Objects.requireNonNull(response, "response");
        this.artifact = Objects.requireNonNull(artifact, "artifact");
    }

    InvitationAcceptResponse getResponse() {
        return response;
    }

    ContractArtifactHandle getArtifact() {
        return artifact;
    }
}
