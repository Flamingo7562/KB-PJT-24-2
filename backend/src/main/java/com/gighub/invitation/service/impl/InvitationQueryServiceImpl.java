package com.gighub.invitation.service.impl;

import com.gighub.auth.security.AuthPrincipal;
import com.gighub.common.api.ApiTimes;
import com.gighub.common.exception.ConflictException;
import com.gighub.common.exception.RoleMismatchException;
import com.gighub.invitation.dto.InvitationDetailResponse;
import com.gighub.invitation.exception.InvitationAlreadyAcceptedException;
import com.gighub.invitation.exception.InvitationExpiredException;
import com.gighub.invitation.exception.InvitationNotFoundException;
import com.gighub.invitation.exception.InvitationRevokedException;
import com.gighub.invitation.exception.InvitationTermsChangedException;
import com.gighub.invitation.mapper.InvitationMapper;
import com.gighub.invitation.mapper.result.InvitationRow;
import com.gighub.invitation.mapper.result.InvitationWorkCaseRow;
import com.gighub.invitation.service.InvitationQueryService;
import com.gighub.invitation.token.InvitationTokenCodec;
import com.gighub.member.domain.UserRole;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Objects;

/**
 * 초대 Token의 상태와 근무 조건 Version을 확인하고 승인된 조건만 돌려줍니다.
 *
 * <p>검증 순서가 계약의 일부입니다. 역할을 먼저 보고, 그다음 Token 형식과 존재를 하나의
 * 결과로 처리한 뒤, 상태와 조건 Version을 확인합니다. 순서가 바뀌면 인증만 한 사람이 응답
 * 차이를 통해 어떤 Token이 실재하는지 알아낼 수 있습니다.</p>
 */
@Service
public class InvitationQueryServiceImpl implements InvitationQueryService {

    /** DB의 DATETIME은 Asia/Seoul 벽시계 값이므로 비교 기준 시각도 같은 지역으로 만듭니다. */
    private static final ZoneId DATABASE_ZONE = ZoneId.of("Asia/Seoul");

    private static final String PENDING = "PENDING";
    private static final String ACCEPTED = "ACCEPTED";
    private static final String REVOKED = "REVOKED";
    private static final String EXPIRED = "EXPIRED";
    private static final String WORK_CASE_DRAFT = "DRAFT";

    private static final String UNUSABLE_INVITATION = "초대 상태를 다시 확인해 주세요.";

    private final InvitationMapper invitationMapper;
    private final InvitationTokenCodec tokenCodec;
    private final Clock clock;

    @Autowired
    public InvitationQueryServiceImpl(
            InvitationMapper invitationMapper,
            InvitationTokenCodec tokenCodec) {
        this(invitationMapper, tokenCodec, Clock.system(DATABASE_ZONE));
    }

    /** 만료 경계를 검증할 때만 고정 Clock을 주입합니다. */
    InvitationQueryServiceImpl(
            InvitationMapper invitationMapper,
            InvitationTokenCodec tokenCodec,
            Clock clock) {
        this.invitationMapper = invitationMapper;
        this.tokenCodec = tokenCodec;
        this.clock = clock;
    }

    @Override
    @Transactional
    public InvitationDetailResponse findByToken(AuthPrincipal principal, String token) {
        // 초대는 WORKER가 수락하는 흐름이므로 다른 역할에는 존재 여부조차 알리지 않습니다.
        if (principal.getRole() != UserRole.WORKER) {
            throw new RoleMismatchException("초대는 WORKER만 조회할 수 있습니다.");
        }

        // 형식이 다른 값은 저장소를 조회하지 않고 미존재와 같은 결과로 끝냅니다.
        if (!tokenCodec.isWellFormed(token)) {
            throw new InvitationNotFoundException();
        }

        InvitationRow invitation =
                invitationMapper.findByTokenHashForUpdate(tokenCodec.hash(token));
        if (invitation == null) {
            throw new InvitationNotFoundException();
        }

        requireUsableStatus(invitation);
        requireNotExpired(invitation);

        InvitationWorkCaseRow workCase = Objects.requireNonNull(
                invitationMapper.findWorkCaseForInvitation(invitation.getWorkCaseId()),
                "초대가 가리키는 근무"
        );
        requireUnchangedTerms(invitation, workCase);
        requireAcceptableWorkCase(workCase);

        return InvitationDetailResponse.of(
                workCase.getTitle(),
                workCase.getWorkplaceName(),
                ApiTimes.toInstant(workCase.getStartsAt()),
                ApiTimes.toInstant(workCase.getEndsAt()),
                workCase.getBreakMinutes(),
                workCase.getBreakPaid(),
                workCase.getDailyWage(),
                workCase.getTermsVersion(),
                ApiTimes.toInstant(invitation.getExpiresAt()),
                // TODO(#155): 배지 등급 산정(BADGE-001)이 없어 level을 채울 근거가 없습니다.
                // user_badges에는 badge_type과 evidence만 있고 level Column이 없으므로,
                // 추측한 등급을 노출하는 대신 활성 Badge 없음과 같은 null을 반환합니다.
                null
        );
    }

    /**
     * 종료된 초대는 상태별로 다른 승인 오류를 냅니다.
     *
     * <p>어떤 경우에도 근무 조건은 응답에 담기지 않으므로, 상태를 구분해도 초대 내용이
     * 새지 않습니다.</p>
     */
    private void requireUsableStatus(InvitationRow invitation) {
        String status = invitation.getStatus();
        if (ACCEPTED.equals(status)) {
            throw new InvitationAlreadyAcceptedException();
        }
        if (REVOKED.equals(status)) {
            throw new InvitationRevokedException();
        }
        if (EXPIRED.equals(status)) {
            throw new InvitationExpiredException();
        }
        if (!PENDING.equals(status)) {
            // 승인 계약에 없는 상태는 조용히 통과시키지 않고 공통 충돌로 끊습니다.
            throw new ConflictException(UNUSABLE_INVITATION);
        }
    }

    /**
     * 만료 시각을 지난 {@code PENDING}은 이 Transaction에서 {@code EXPIRED}로 확정합니다.
     *
     * <p>상태를 바꾸지 않고 오류만 내면 만료된 초대가 활성 Slot을 계속 차지해 OWNER가 새
     * 초대를 발급할 수 없습니다.</p>
     */
    private void requireNotExpired(InvitationRow invitation) {
        LocalDateTime now = LocalDateTime.now(clock);
        if (!now.isBefore(invitation.getExpiresAt())) {
            invitationMapper.markExpired(invitation.getId());
            throw new InvitationExpiredException();
        }
    }

    /**
     * 초대가 기대한 조건 Version과 현재 조건이 같아야 합니다.
     *
     * <p>다르면 이전 Snapshot을 보여 주지 않고 끊습니다. 지난 조건으로 근무를 확정하면
     * 실제 계약 내용과 화면이 어긋납니다.</p>
     */
    private void requireUnchangedTerms(InvitationRow invitation, InvitationWorkCaseRow workCase) {
        if (!workCase.getTermsVersion().equals(invitation.getExpectedTermsVersion())) {
            throw new InvitationTermsChangedException();
        }
    }

    /** 이미 당사자가 정해졌거나 확정할 수 없는 상태의 근무는 조건을 보여 주지 않습니다. */
    private void requireAcceptableWorkCase(InvitationWorkCaseRow workCase) {
        if (workCase.getWorkerId() != null) {
            throw new InvitationAlreadyAcceptedException();
        }
        if (!WORK_CASE_DRAFT.equals(workCase.getStatus())) {
            throw new ConflictException(UNUSABLE_INVITATION);
        }
    }
}
