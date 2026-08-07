package com.gighub.contract;

/**
 * 근로계약 최종본 파일의 생성·승격·정리 경계입니다.
 *
 * <p>파일 쓰기는 DB Transaction과 달리 Rollback되지 않습니다. 그래서 승인 결정
 * {@code DEC-CONTRACT-FILE-COMMIT}은 순서를 이렇게 못박았고, 이 인터페이스가 그 순서를
 * 그대로 옮긴 것입니다.</p>
 *
 * <ol>
 *   <li>{@link #prepare}는 수락 Transaction <b>안에서</b> 부릅니다. 결정적 임시 Key에 파일을
 *   쓰고 DB에는 최종 Key를 기록합니다. 여기서 실패하면 수락 전체가 Rollback됩니다.</li>
 *   <li>{@link #promote}는 Commit <b>뒤에</b> 부릅니다. 임시 Object를 최종 위치로 멱등하게
 *   옮깁니다.</li>
 *   <li>{@link #discardPending}은 Rollback <b>뒤에</b> 부릅니다. 남은 임시 Object를 최선
 *   노력으로 지웁니다.</li>
 * </ol>
 *
 * <p>{@code prepare}만 예외를 던질 수 있습니다. {@code promote}와 {@code discardPending}은
 * 이미 확정된 수락을 뒤집을 수 없으므로 실패를 밖으로 던지지 않고 구현이 스스로 기록합니다.
 * 승격에 실패해도 계약과 수락 200은 그대로 유지되고, 조회 시점에 임시 Object로 Fallback해
 * 사용자는 같은 파일을 받습니다.</p>
 */
public interface ContractArtifactPort {

    /**
     * 계약서 파일을 임시 Key에 쓰고 문서 관련 행을 만듭니다.
     *
     * @param command 대상 계약과 공유 수락 시각
     * @return Commit 뒤 승격할 대상. 만들 파일이 없으면 {@link ContractArtifactHandle#nothing()}
     */
    ContractArtifactHandle prepare(ContractArtifactCommand command);

    /** 임시 Object를 최종 위치로 멱등하게 옮깁니다. 실패해도 예외를 던지지 않습니다. */
    void promote(ContractArtifactHandle handle);

    /**
     * Rollback으로 버려진 임시 Object를 최선 노력으로 지웁니다.
     *
     * <p>문서 식별자는 Rollback과 함께 사라지므로 근무 식별자만 받습니다. 임시 Key가 근무
     * 단위로 결정되어 있어 그것만으로 정리할 수 있습니다.</p>
     */
    void discardPending(long workCaseId);
}
