package com.gighub.invitation.mapper;

import com.gighub.invitation.mapper.param.InvitationInsertParam;
import com.gighub.invitation.mapper.result.InvitationRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

/** 근무 초대 행의 저장과 상태 전이 SQL 진입점입니다. */
@Mapper
public interface InvitationMapper {

    /**
     * 임시 Token Hash로 {@code PENDING} 초대를 만들고 생성된 식별자를 {@code param.id}에
     * 채웁니다.
     *
     * <p>Token 원문은 초대 ID에서 파생하므로 INSERT 시점에는 최종 Hash를 알 수 없습니다.
     * {@code token_hash}는 {@code NOT NULL UNIQUE}라 비워 둘 수도 없어, 충돌하지 않는 임시
     * 값으로 행을 만든 뒤 {@link #updateTokenHash(long, byte[])}로 확정합니다. 두 문장은 반드시
     * 같은 Transaction에 있어야 임시 Hash가 밖에서 보이지 않습니다.</p>
     *
     * <p>같은 Work Case에 활성 {@code PENDING}이 이미 있으면 {@code uk_work_invitations_active}가
     * 중복 예외를 던집니다. 사전 조회로는 동시 발급을 막을 수 없으므로 이 제약을 그대로
     * 활용하고 Service가 승인된 응답으로 바꿉니다.</p>
     */
    int insertPending(InvitationInsertParam param);

    /**
     * 확보한 초대 ID로 파생한 실제 Token Hash를 기록합니다.
     *
     * @param invitationId {@link #insertPending(InvitationInsertParam)}이 채운 식별자
     * @param tokenHash    Token 원문의 SHA-256 Hash 32byte
     * @return 갱신된 행 수
     */
    int updateTokenHash(
            @Param("invitationId") long invitationId,
            @Param("tokenHash") byte[] tokenHash);

    /**
     * Token Hash로 초대 한 건을 잠근 채 조회합니다.
     *
     * <p>조회 흐름이 만료된 {@code PENDING}을 같은 Transaction에서 {@code EXPIRED}로 전이하므로
     * 읽는 시점에 행을 잠급니다. 잠그지 않으면 두 요청이 같은 초대를 동시에 읽고 서로 다른
     * 판정을 내릴 수 있습니다.</p>
     *
     * <p>Token 원문이 아니라 Hash로 찾습니다. 저장소에 원문이 없고, SQL Parameter와 느린 Query
     * 로그에 원문이 남지 않게 하려는 의도도 있습니다.</p>
     *
     * @param tokenHash 요청이 전달한 Token 원문의 Hash
     * @return 일치하는 초대. 없으면 {@code null}
     */
    InvitationRow findByTokenHashForUpdate(@Param("tokenHash") byte[] tokenHash);

    /**
     * Work Case의 활성 {@code PENDING} 초대를 잠근 채 조회합니다.
     *
     * <p>{@code uk_work_invitations_active} 덕분에 결과는 최대 한 건입니다.</p>
     *
     * @param workCaseId 대상 근무 식별자
     * @return 활성 초대. 없으면 {@code null}
     */
    InvitationRow findActivePendingByWorkCaseIdForUpdate(@Param("workCaseId") long workCaseId);

    /**
     * 초대 한 건을 {@code EXPIRED}로 전이합니다.
     *
     * <p>{@code PENDING}일 때만 바뀝니다. 이미 수락·철회된 초대를 만료로 덮으면 종료 사유가
     * 사라지고, 동시에 들어온 다른 전이를 조용히 지웁니다.</p>
     *
     * @param invitationId 대상 초대 식별자
     * @return 전이된 행 수. 이미 다른 상태면 0
     */
    int markExpired(@Param("invitationId") long invitationId);

    /**
     * Work Case에서 만료 시각이 지난 {@code PENDING}을 {@code EXPIRED}로 전이합니다.
     *
     * <p>발급 흐름이 활성 초대를 찾기 전에 실행합니다. 만료된 행이 {@code PENDING}으로 남아
     * 있으면 활성 초대 Unique 제약이 새 발급을 막습니다.</p>
     *
     * @param workCaseId 대상 근무 식별자
     * @param now        Asia/Seoul 기준 현재 시각
     * @return 전이된 행 수
     */
    int expireOverduePending(
            @Param("workCaseId") long workCaseId,
            @Param("now") LocalDateTime now);

    /**
     * Work Case의 활성 {@code PENDING} 초대를 {@code REVOKED}로 전이합니다.
     *
     * <p>근무 조건 수정, 취소, 명시적 재발급이 이전 Link를 무효화할 때 사용합니다. 행을
     * 지우지 않고 상태로 남기므로 이전 Link는 미존재가 아니라 철회로 응답할 수 있고 초대
     * 이력도 보존됩니다.</p>
     *
     * <p>조건 변경 Transaction에서 함께 호출해야 합니다. 따로 실행하면 새 조건과 이전 Link가
     * 잠시 동시에 유효해집니다.</p>
     *
     * @param workCaseId 대상 근무 식별자
     * @param revokedAt  Asia/Seoul 기준 철회 시각
     * @return 전이된 행 수
     */
    int revokePendingByWorkCaseId(
            @Param("workCaseId") long workCaseId,
            @Param("revokedAt") LocalDateTime revokedAt);
}
