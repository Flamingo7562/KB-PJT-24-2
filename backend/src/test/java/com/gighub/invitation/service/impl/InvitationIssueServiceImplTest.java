package com.gighub.invitation.service.impl;

import com.gighub.auth.security.AuthPrincipal;
import com.gighub.common.exception.ConflictException;
import com.gighub.common.exception.ResourceNotFoundException;
import com.gighub.common.exception.RoleMismatchException;
import com.gighub.common.exception.WorkCaseLockedException;
import com.gighub.invitation.config.InvitationLinkFactory;
import com.gighub.invitation.config.InvitationProperties;
import com.gighub.invitation.mapper.InvitationMapper;
import com.gighub.invitation.mapper.param.InvitationInsertParam;
import com.gighub.invitation.mapper.result.InvitationRow;
import com.gighub.invitation.mapper.result.InvitationWorkCaseLockRow;
import com.gighub.invitation.mapper.result.InvitationWorkCaseRow;
import com.gighub.invitation.service.InvitationIssueResult;
import com.gighub.invitation.token.InvitationTokenCodec;
import com.gighub.member.domain.UserRole;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 초대 발급의 상태 판정, 재조회 계약과 잠금 순서를 확인합니다.
 */
class InvitationIssueServiceImplTest {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private static final long WORK_CASE_ID = 7L;
    private static final long OWNER_ID = 3L;
    private static final long NEW_INVITATION_ID = 41L;
    private static final LocalDateTime STARTS_AT = LocalDateTime.of(2026, 8, 20, 10, 0);
    private static final String WEB_ORIGIN = "https://app.example.com";

    private final InvitationTokenCodec codec = new InvitationTokenCodec(
            InvitationProperties.of(
                    "issue-test-invitation-secret-0123456789", null, WEB_ORIGIN)
    );
    private final InvitationLinkFactory linkFactory = new InvitationLinkFactory(
            InvitationProperties.of(
                    "issue-test-invitation-secret-0123456789", null, WEB_ORIGIN)
    );
    private final StubInvitationMapper mapper = new StubInvitationMapper();

    @Test
    void issuesNewInvitationCopyingTermsVersionAndExpiryFromTheLockedWorkCase() {
        mapper.workCase = draftWorkCase(3);

        InvitationIssueResult result = service(STARTS_AT.minusDays(1L))
                .issue(owner(), WORK_CASE_ID);

        assertTrue(result.isCreated(), "새 초대는 201이어야 합니다.");
        assertEquals(
                WEB_ORIGIN + "/invitations/" + codec.deriveToken(NEW_INVITATION_ID),
                result.getResponse().getInviteUrl()
        );
        assertEquals(STARTS_AT.atZone(SEOUL).toInstant(), result.getResponse().getExpiresAt());

        // 조건 Version과 만료는 잠근 근무에서만 복사합니다.
        assertEquals(1, mapper.inserted.size());
        assertEquals(3, mapper.inserted.get(0).getExpectedTermsVersion());
        assertEquals(STARTS_AT, mapper.inserted.get(0).getExpiresAt());
        // 저장된 것은 Hash뿐입니다.
        assertTrue(codec.matches(
                codec.deriveToken(NEW_INVITATION_ID), mapper.storedTokenHashes.get(0)));
    }

    @Test
    void lockOrderIsWorkCaseThenInvitation() {
        mapper.workCase = draftWorkCase(1);

        service(STARTS_AT.minusDays(1L)).issue(owner(), WORK_CASE_ID);

        // #154 조건 수정 흐름도 근무 행을 먼저 잠급니다. 순서가 어긋나면 교착이 납니다.
        assertEquals(
                List.of("lockWorkCase", "expireOverdue", "findActivePending", "insert"),
                mapper.calls.subList(0, 4)
        );
    }

    @Test
    void existingActiveInvitationOfTheSameVersionReturnsTheSameLinkWithoutChangingTheRow() {
        mapper.workCase = draftWorkCase(3);
        mapper.activePending = pendingInvitation(11L, 3, STARTS_AT);

        InvitationIssueResult result = service(STARTS_AT.minusDays(1L))
                .issue(owner(), WORK_CASE_ID);

        assertFalse(result.isCreated(), "기존 활성 초대 재조회는 200이어야 합니다.");
        assertEquals(
                WEB_ORIGIN + "/invitations/" + codec.deriveToken(11L),
                result.getResponse().getInviteUrl()
        );
        assertEquals(STARTS_AT.atZone(SEOUL).toInstant(), result.getResponse().getExpiresAt());
        assertTrue(mapper.inserted.isEmpty(), "행을 새로 만들면 안 됩니다.");
        assertTrue(mapper.revokedAt.isEmpty(), "만료를 바꾸거나 철회하면 안 됩니다.");
    }

    @Test
    void stalePendingFromAnOlderVersionIsRevokedBeforeIssuingTheCurrentOne() {
        mapper.workCase = draftWorkCase(4);
        mapper.activePending = pendingInvitation(11L, 3, STARTS_AT);

        InvitationIssueResult result = service(STARTS_AT.minusDays(1L))
                .issue(owner(), WORK_CASE_ID);

        assertTrue(result.isCreated());
        assertEquals(1, mapper.revokedAt.size(), "이전 Version의 활성 초대를 철회해야 합니다.");
        assertEquals(4, mapper.inserted.get(0).getExpectedTermsVersion());
    }

    @Test
    void overduePendingIsExpiredBeforeTheActiveSlotIsChecked() {
        mapper.workCase = draftWorkCase(1);

        service(STARTS_AT.minusHours(1L)).issue(owner(), WORK_CASE_ID);

        assertEquals(List.of(STARTS_AT.minusHours(1L)), mapper.expiredBefore);
        assertTrue(mapper.calls.indexOf("expireOverdue") < mapper.calls.indexOf("findActivePending"));
    }

    @Test
    void nonOwnerRoleIsRejectedBeforeTheWorkCaseIsEvenLocked() {
        mapper.workCase = draftWorkCase(1);

        assertThrows(
                RoleMismatchException.class,
                () -> service(STARTS_AT.minusDays(1L)).issue(worker(), WORK_CASE_ID)
        );
        assertTrue(mapper.calls.isEmpty(), "역할 거절이 잠금보다 앞서야 합니다.");
    }

    @Test
    void missingAndOtherOwnersWorkCaseLookIdentical() {
        mapper.workCase = null;
        ResourceNotFoundException missing = assertThrows(
                ResourceNotFoundException.class,
                () -> service(STARTS_AT.minusDays(1L)).issue(owner(), WORK_CASE_ID)
        );

        mapper.workCase = draftWorkCase(1).toBuilder().employerId(99L).build();
        ResourceNotFoundException otherOwner = assertThrows(
                ResourceNotFoundException.class,
                () -> service(STARTS_AT.minusDays(1L)).issue(owner(), WORK_CASE_ID)
        );

        assertEquals(missing.getStatus(), otherOwner.getStatus());
        assertEquals(missing.getCode(), otherOwner.getCode());
        assertEquals(missing.getMessage(), otherOwner.getMessage());
    }

    @Test
    void matchedNonDraftAndStartedWorkCasesShareOneLockedError() {
        mapper.workCase = draftWorkCase(1).toBuilder().workerId(9L).build();
        assertLocked(STARTS_AT.minusDays(1L));

        mapper.workCase = draftWorkCase(1).toBuilder().status("ACCEPTED").build();
        assertLocked(STARTS_AT.minusDays(1L));

        mapper.workCase = draftWorkCase(1).toBuilder().status("CANCELED").build();
        assertLocked(STARTS_AT.minusDays(1L));

        // 시작 시각과 같은 순간부터는 발급할 수 없습니다.
        mapper.workCase = draftWorkCase(1);
        assertLocked(STARTS_AT);

        assertTrue(mapper.inserted.isEmpty());
    }

    @Test
    void unreproducibleActiveLinkEndsWithTheApprovedConflict() {
        mapper.workCase = draftWorkCase(1);
        // Key 교체로 어느 Secret으로도 재현할 수 없게 된 활성 초대입니다.
        mapper.activePending = InvitationRow.builder()
                .id(11L)
                .workCaseId(WORK_CASE_ID)
                .tokenHash(codec.hash("token-issued-with-a-retired-secret"))
                .status("PENDING")
                .expectedTermsVersion(1)
                .expiresAt(STARTS_AT)
                .build();

        ConflictException failure = assertThrows(
                ConflictException.class,
                () -> service(STARTS_AT.minusDays(1L)).issue(owner(), WORK_CASE_ID)
        );
        assertEquals("초대 상태를 다시 확인해 주세요.", failure.getMessage());
    }

    @Test
    void noFailureMessageEchoesAnIssuedToken() {
        mapper.workCase = draftWorkCase(1).toBuilder().status("ACCEPTED").build();
        String token = codec.deriveToken(NEW_INVITATION_ID);

        WorkCaseLockedException failure = assertThrows(
                WorkCaseLockedException.class,
                () -> service(STARTS_AT.minusDays(1L)).issue(owner(), WORK_CASE_ID)
        );

        assertFalse(failure.getMessage().contains(token));
    }

    private void assertLocked(LocalDateTime now) {
        WorkCaseLockedException failure = assertThrows(
                WorkCaseLockedException.class,
                () -> service(now).issue(owner(), WORK_CASE_ID)
        );
        assertEquals("초대를 발급할 수 없는 근무입니다.", failure.getMessage());
    }

    private InvitationIssueServiceImpl service(LocalDateTime now) {
        return new InvitationIssueServiceImpl(
                mapper, codec, linkFactory, Clock.fixed(now.atZone(SEOUL).toInstant(), SEOUL));
    }

    private static InvitationWorkCaseLockRow draftWorkCase(int termsVersion) {
        return InvitationWorkCaseLockRow.builder()
                .workCaseId(WORK_CASE_ID)
                .employerId(OWNER_ID)
                .workerId(null)
                .status("DRAFT")
                .termsVersion(termsVersion)
                .startsAt(STARTS_AT)
                .build();
    }

    private InvitationRow pendingInvitation(
            long invitationId,
            int expectedTermsVersion,
            LocalDateTime expiresAt) {
        return InvitationRow.builder()
                .id(invitationId)
                .workCaseId(WORK_CASE_ID)
                .tokenHash(codec.hash(codec.deriveToken(invitationId)))
                .status("PENDING")
                .expectedTermsVersion(expectedTermsVersion)
                .expiresAt(expiresAt)
                .build();
    }

    private static AuthPrincipal owner() {
        return new AuthPrincipal(OWNER_ID, UserRole.OWNER, "김사장");
    }

    private static AuthPrincipal worker() {
        return new AuthPrincipal(11L, UserRole.WORKER, "김알바");
    }

    /** 호출 순서까지 확인해야 해서 Mock 대신 직접 만든 Stub을 씁니다. */
    private static final class StubInvitationMapper implements InvitationMapper {

        private final List<String> calls = new ArrayList<>();
        private final List<InvitationInsertParam> inserted = new ArrayList<>();
        private final List<byte[]> storedTokenHashes = new ArrayList<>();
        private final List<LocalDateTime> expiredBefore = new ArrayList<>();
        private final List<LocalDateTime> revokedAt = new ArrayList<>();

        private InvitationWorkCaseLockRow workCase;
        private InvitationRow activePending;

        @Override
        public InvitationWorkCaseLockRow lockWorkCaseForIssue(long workCaseId) {
            calls.add("lockWorkCase");
            return workCase;
        }

        @Override
        public int insertPending(InvitationInsertParam param) {
            calls.add("insert");
            param.setId(NEW_INVITATION_ID);
            inserted.add(param);
            return 1;
        }

        @Override
        public int updateTokenHash(long invitationId, byte[] tokenHash) {
            calls.add("updateTokenHash");
            storedTokenHashes.add(tokenHash);
            return 1;
        }

        @Override
        public InvitationRow findByTokenHashForUpdate(byte[] tokenHash) {
            throw new UnsupportedOperationException();
        }

        @Override
        public InvitationRow findActivePendingByWorkCaseIdForUpdate(long workCaseId) {
            calls.add("findActivePending");
            return activePending;
        }

        @Override
        public InvitationWorkCaseRow findWorkCaseForInvitation(long workCaseId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int markExpired(long invitationId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int expireOverduePending(long workCaseId, LocalDateTime now) {
            calls.add("expireOverdue");
            expiredBefore.add(now);
            return 0;
        }

        @Override
        public int revokePendingByWorkCaseId(long workCaseId, LocalDateTime at) {
            calls.add("revoke");
            revokedAt.add(at);
            activePending = null;
            return 1;
        }
    }
}
