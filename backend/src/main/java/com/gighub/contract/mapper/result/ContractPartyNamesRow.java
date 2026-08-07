package com.gighub.contract.mapper.result;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 계약 Snapshot에 굳힐 당사자 이름입니다.
 *
 * <p>근무 행 잠금 조회에 {@code users}를 JOIN하지 않고 따로 읽습니다. {@code FOR UPDATE}에
 * JOIN을 넣으면 사용자 행까지 잠겨 프로필 변경 같은 무관한 흐름과 충돌합니다.</p>
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContractPartyNamesRow {

    private String employerName;
    private String workerName;
}
