package com.gighub.invitation.service.impl;

import com.gighub.auth.security.AuthPrincipal;
import com.gighub.common.exception.RoleMismatchException;
import com.gighub.idempotency.IdempotencyClaimResult;
import com.gighub.idempotency.IdempotencyClaimService;
import com.gighub.invitation.dto.InvitationAcceptResponse;
import com.gighub.invitation.exception.InvitationNotFoundException;
import com.gighub.invitation.mapper.InvitationMapper;
import com.gighub.invitation.mapper.result.InvitationRow;
import com.gighub.invitation.service.InvitationAcceptResult;
import com.gighub.invitation.service.InvitationAcceptService;
import com.gighub.invitation.token.InvitationTokenCodec;
import com.gighub.member.domain.UserRole;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * 멱등 Claim 생명주기를 감싸고 본 처리를 Transaction 실행기에 넘깁니다.
 *
 * <p>이 클래스에는 {@code @Transactional}이 없습니다. Claim 선점과 포기는 본 처리와 다른
 * Transaction이어야 하는데, 한 Method를 하나의 Transaction으로 묶으면 그 경계가 사라집니다.
 * 특히 포기는 본 처리가 <b>끝난 뒤</b>에 실행돼야 합니다. 본 처리가 Claim 행을 잠근 채
 * 새 Transaction으로 같은 행을 지우려 하면 스스로 교착합니다.</p>
 */
@Service
public class InvitationAcceptServiceImpl implements InvitationAcceptService {

    /** 멱등 저장 범위를 나누는 Operation 표지입니다. */
    private static final String OPERATION_CODE = "INVITATION_ACCEPT";

    private final InvitationMapper invitationMapper;
    private final InvitationTokenCodec tokenCodec;
    private final IdempotencyClaimService claimService;
    private final AcceptAggregateExecutor aggregateExecutor;
    private final AcceptJson acceptJson;

    public InvitationAcceptServiceImpl(
            InvitationMapper invitationMapper,
            InvitationTokenCodec tokenCodec,
            IdempotencyClaimService claimService,
            AcceptAggregateExecutor aggregateExecutor,
            AcceptJson acceptJson) {
        this.invitationMapper = invitationMapper;
        this.tokenCodec = tokenCodec;
        this.claimService = claimService;
        this.aggregateExecutor = aggregateExecutor;
        this.acceptJson = acceptJson;
    }

    @Override
    public InvitationAcceptResult accept(
            AuthPrincipal principal,
            String token,
            String rawKey) {
        if (principal.getRole() != UserRole.WORKER) {
            throw new RoleMismatchException("초대는 WORKER만 수락할 수 있습니다.");
        }
        // 형식 오류·미존재는 Claim을 만들지 않고 끝냅니다. 저장하면 같은 Key로 올바른
        // Token을 다시 시도할 수 없습니다.
        if (!tokenCodec.isWellFormed(token)) {
            throw new InvitationNotFoundException();
        }

        byte[] tokenHash = tokenCodec.hash(token);
        InvitationRow invitation = invitationMapper.findByTokenHash(tokenHash);
        if (invitation == null) {
            throw new InvitationNotFoundException();
        }

        IdempotencyClaimResult claim = claimService.claim(
                principal.getUserId(),
                OPERATION_CODE,
                rawKey,
                fingerprint(tokenHash, invitation.getExpectedTermsVersion()));
        if (claim.isReplay()) {
            return InvitationAcceptResult.replayed(
                    acceptJson.readResponseBody(claim.getResponseBody()));
        }

        return InvitationAcceptResult.first(runAggregate(principal, invitation, tokenHash, claim));
    }

    /**
     * 본 처리를 실행하고, 실패하면 Transaction이 끝난 뒤 Claim을 지웁니다.
     *
     * <p>Claim을 남기면 같은 Key로 다시 시도할 수 없습니다. 반대로 성공 Claim은 본 처리
     * Transaction 안에서 이미 완료 상태로 Commit됐으므로 여기서 손대지 않습니다.</p>
     */
    private InvitationAcceptResponse runAggregate(
            AuthPrincipal principal,
            InvitationRow invitation,
            byte[] tokenHash,
            IdempotencyClaimResult claim) {
        try {
            return aggregateExecutor.execute(
                    principal,
                    invitation.getId(),
                    invitation.getWorkCaseId(),
                    tokenHash,
                    claim.getClaimId());
        } catch (RuntimeException failure) {
            claimService.abandon(claim.getClaimId());
            throw failure;
        }
    }

    /**
     * 같은 요청인지 판정할 Fingerprint를 만듭니다.
     *
     * <p>승인 규칙은 Token Hash 소문자 Hex와 초대가 기대한 조건 Version입니다. Token 원문과
     * Header Key는 입력에 넣지 않으므로 Fingerprint를 저장해도 둘을 복원할 수 없습니다. 같은
     * Key로 다른 Token이나 다른 조건 Version을 보내면 값이 달라져 재사용으로 거절됩니다.</p>
     */
    private static byte[] fingerprint(byte[] tokenHash, int expectedTermsVersion) {
        String source = OPERATION_CODE + "\n"
                + HexFormat.of().formatHex(tokenHash) + "\n"
                + expectedTermsVersion;
        try {
            return MessageDigest.getInstance("SHA-256")
                    .digest(source.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 알고리즘을 사용할 수 없습니다.", exception);
        }
    }
}
