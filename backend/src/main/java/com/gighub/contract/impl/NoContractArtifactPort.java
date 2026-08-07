package com.gighub.contract.impl;

import com.gighub.contract.ContractArtifactCommand;
import com.gighub.contract.ContractArtifactHandle;
import com.gighub.contract.ContractArtifactPort;
import org.springframework.stereotype.Component;

/**
 * 계약서 파일 생성이 구현되기 전까지 쓰는 빈 구현입니다.
 *
 * <p>수락 Aggregate의 나머지(매칭·계약 Snapshot·예치·정산)는 파일 없이도 완결되고, 수락
 * 응답 {@code {workCaseId, escrowStatus}}도 파일에 의존하지 않습니다. 그래서 파일 생성이
 * 준비될 때까지 이 구현으로 흐름을 열어 둡니다.</p>
 *
 * <p>TODO(#157): PDF 렌더링, 비공개 Storage 승격, {@code documents}·
 * {@code document_versions}·{@code document_signatures}·{@code document_shares} 행 생성을
 * 담은 구현으로 교체합니다. 그때 이 클래스는 삭제합니다.</p>
 */
@Component
public class NoContractArtifactPort implements ContractArtifactPort {

    @Override
    public ContractArtifactHandle prepare(ContractArtifactCommand command) {
        return ContractArtifactHandle.nothing();
    }

    @Override
    public void promote(ContractArtifactHandle handle) {
        // 승격할 파일이 없습니다.
    }

    @Override
    public void discardPending(long workCaseId) {
        // 정리할 임시 Object가 없습니다.
    }
}
