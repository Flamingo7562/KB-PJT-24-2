package com.gighub.invitation.service;

import com.gighub.auth.security.AuthPrincipal;
import com.gighub.common.exception.ConflictException;
import com.gighub.common.exception.ResourceNotFoundException;
import com.gighub.common.exception.WorkCaseLockedException;
import com.gighub.config.RootConfig;
import com.gighub.invitation.dto.InvitationIssueResponse;
import com.gighub.member.domain.UserRole;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 실제 MySQL에서 발급·재발급의 잠금과 활성 초대 유일성을 검증합니다.
 *
 * <p>동시 발급이 하나의 Link로 수렴하는지는 실제 행 잠금 없이는 확인할 수 없어
 * {@code database} Tag의 Opt-in Test로 둡니다.</p>
 *
 * <p>Work Case Fixture는 SQL로 직접 만듭니다. 근무 등록 API를 거치면 이 검증이 근무 도메인의
 * 입력 규칙 변화에도 함께 깨집니다.</p>
 */
@Tag("database")
class InvitationIssueDatabaseIntegrationTest {

    @Test
    @Timeout(90)
    void issueReissueAndConcurrentCallsKeepOneActiveInvitation() throws Exception {
        try (AnnotationConfigApplicationContext context =
                     new AnnotationConfigApplicationContext(RootConfig.class)) {
            JdbcTemplate jdbcTemplate = new JdbcTemplate(context.getBean(DataSource.class));
            InvitationIssueService issueService = context.getBean(InvitationIssueService.class);

            String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
            Fixture fixture = insertWorkCaseFixture(jdbcTemplate, suffix);

            try {
                verifyIssueThenRefetchReturnsTheSameLink(jdbcTemplate, issueService, fixture);
                verifyReissueReplacesTheLinkAndPreservesHistory(jdbcTemplate, issueService, fixture);
                verifyTermsChangeMakesTheNextIssueUseTheNewVersion(
                        jdbcTemplate, issueService, fixture);
                verifyGuardsOnOwnershipAndState(jdbcTemplate, issueService, fixture);
                verifyConcurrentIssueConvergesOnOneLink(jdbcTemplate, issueService, fixture);
            } finally {
                cleanUp(jdbcTemplate, fixture);
            }
        }
    }

    /** 두 번째 발급은 행을 바꾸지 않고 같은 Link를 그대로 돌려줘야 합니다. */
    private void verifyIssueThenRefetchReturnsTheSameLink(
            JdbcTemplate jdbcTemplate,
            InvitationIssueService issueService,
            Fixture fixture) {
        InvitationIssueResponse first =
                issueService.issue(fixture.owner(), fixture.workCaseId).getResponse();
        assertTrue(first.getInviteUrl().startsWith("http"));
        assertEquals(fixture.startsAt, expiresAtOfActive(jdbcTemplate, fixture));

        Map<String, Object> afterFirst = activeInvitation(jdbcTemplate, fixture);
        InvitationIssueResponse second =
                issueService.issue(fixture.owner(), fixture.workCaseId).getResponse();

        assertEquals(first.getInviteUrl(), second.getInviteUrl(), "같은 Link를 재현해야 합니다.");
        assertEquals(first.getExpiresAt(), second.getExpiresAt());

        Map<String, Object> afterSecond = activeInvitation(jdbcTemplate, fixture);
        assertEquals(afterFirst.get("id"), afterSecond.get("id"), "행을 새로 만들면 안 됩니다.");
        assertEquals(afterFirst.get("expires_at"), afterSecond.get("expires_at"));
        assertEquals(1, countInvitations(jdbcTemplate, fixture, "PENDING"));
    }

    /** 재발급은 이전 초대를 REVOKED로 남기고 다른 Link를 만들어야 합니다. */
    private void verifyReissueReplacesTheLinkAndPreservesHistory(
            JdbcTemplate jdbcTemplate,
            InvitationIssueService issueService,
            Fixture fixture) {
        Map<String, Object> before = activeInvitation(jdbcTemplate, fixture);
        InvitationIssueResponse previous =
                issueService.issue(fixture.owner(), fixture.workCaseId).getResponse();

        InvitationIssueResponse reissued =
                issueService.reissue(fixture.owner(), fixture.workCaseId);

        assertNotEquals(previous.getInviteUrl(), reissued.getInviteUrl());
        assertEquals(1, countInvitations(jdbcTemplate, fixture, "PENDING"));
        assertEquals(1, countInvitations(jdbcTemplate, fixture, "REVOKED"));

        Map<String, Object> revoked = jdbcTemplate.queryForMap(
                "SELECT status, revoked_at, token_hash FROM work_invitations WHERE id = ?",
                before.get("id"));
        assertEquals("REVOKED", revoked.get("status"));
        assertNotNull(revoked.get("revoked_at"), "철회 시각이 남아야 합니다.");

        Map<String, Object> current = activeInvitation(jdbcTemplate, fixture);
        assertNotEquals(before.get("id"), current.get("id"), "다른 ID의 새 초대여야 합니다.");

        // 재발급 이전 Link의 Hash는 그대로 남아 조회에서 철회로 응답할 수 있어야 합니다.
        assertNotNull(revoked.get("token_hash"));

        // 활성 초대가 하나뿐이라 재발급을 반복해도 활성은 계속 하나입니다.
        issueService.reissue(fixture.owner(), fixture.workCaseId);
        assertEquals(1, countInvitations(jdbcTemplate, fixture, "PENDING"));
        assertEquals(2, countInvitations(jdbcTemplate, fixture, "REVOKED"));
    }

    /**
     * 조건이 바뀌면 이전 Version의 활성 초대는 쓰지 않고 현재 Version으로 새로 만들어야 합니다.
     *
     * <p>실제 수정 흐름(#154)은 조건 변경과 함께 활성 초대를 철회하므로 보통 이 경로에
     * 도달하지 않습니다. 그 철회가 빠지더라도 발급이 이전 조건의 Link를 되살리지 않는지
     * 확인합니다.</p>
     */
    private void verifyTermsChangeMakesTheNextIssueUseTheNewVersion(
            JdbcTemplate jdbcTemplate,
            InvitationIssueService issueService,
            Fixture fixture) {
        Map<String, Object> stale = activeInvitation(jdbcTemplate, fixture);
        jdbcTemplate.update(
                "UPDATE work_cases SET terms_version = terms_version + 1 WHERE id = ?",
                fixture.workCaseId);

        issueService.issue(fixture.owner(), fixture.workCaseId);

        Map<String, Object> current = activeInvitation(jdbcTemplate, fixture);
        assertNotEquals(stale.get("id"), current.get("id"));
        assertEquals(
                2,
                ((Number) current.get("expected_terms_version")).intValue(),
                "새 초대는 현재 조건 Version을 복사해야 합니다."
        );
        assertEquals(
                "REVOKED",
                jdbcTemplate.queryForObject(
                        "SELECT status FROM work_invitations WHERE id = ?",
                        String.class,
                        stale.get("id"))
        );

        // 재발급도 현재 Version의 활성 초대에서만 동작합니다.
        assertEquals(1, countInvitations(jdbcTemplate, fixture, "PENDING"));
    }

    /** 소유권과 상태 가드가 실제 스키마 위에서도 같은 결과를 내야 합니다. */
    private void verifyGuardsOnOwnershipAndState(
            JdbcTemplate jdbcTemplate,
            InvitationIssueService issueService,
            Fixture fixture) {
        AuthPrincipal otherOwner = new AuthPrincipal(
                fixture.ownerUserId + 1_000_000L, UserRole.OWNER, "남의사장");
        assertThrows(
                ResourceNotFoundException.class,
                () -> issueService.issue(otherOwner, fixture.workCaseId));
        assertThrows(
                ResourceNotFoundException.class,
                () -> issueService.issue(fixture.owner(), fixture.workCaseId + 9_000_000L));

        // 매칭된 근무에는 발급할 수 없습니다.
        jdbcTemplate.update(
                "UPDATE work_cases SET worker_id = ? WHERE id = ?",
                fixture.workerUserId,
                fixture.workCaseId);
        assertThrows(
                WorkCaseLockedException.class,
                () -> issueService.issue(fixture.owner(), fixture.workCaseId));
        assertThrows(
                WorkCaseLockedException.class,
                () -> issueService.reissue(fixture.owner(), fixture.workCaseId));

        jdbcTemplate.update(
                "UPDATE work_cases SET worker_id = NULL WHERE id = ?", fixture.workCaseId);

        // 활성 초대를 모두 정리하면 재발급은 교체 대상이 없어 충돌합니다.
        jdbcTemplate.update(
                "UPDATE work_invitations SET status = 'REVOKED', revoked_at = CURRENT_TIMESTAMP(6)"
                        + " WHERE work_case_id = ? AND status = 'PENDING'",
                fixture.workCaseId);
        assertThrows(
                ConflictException.class,
                () -> issueService.reissue(fixture.owner(), fixture.workCaseId));
    }

    /**
     * 사전 조회로는 막을 수 없는 경로입니다. 두 요청이 동시에 "활성 초대 없음"을 확인해도
     * 근무 행 잠금 덕분에 하나의 Link로 수렴해야 합니다.
     */
    private void verifyConcurrentIssueConvergesOnOneLink(
            JdbcTemplate jdbcTemplate,
            InvitationIssueService issueService,
            Fixture fixture) throws Exception {
        jdbcTemplate.update(
                "DELETE FROM work_invitations WHERE work_case_id = ?", fixture.workCaseId);

        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            List<Future<String>> results = List.of(
                    executor.submit(() -> attemptIssue(issueService, fixture, start)),
                    executor.submit(() -> attemptIssue(issueService, fixture, start))
            );

            start.countDown();

            String first = results.get(0).get(30L, TimeUnit.SECONDS);
            String second = results.get(1).get(30L, TimeUnit.SECONDS);

            assertEquals(first, second, "동시 발급이 같은 Link로 수렴해야 합니다.");
            assertEquals(1, countInvitations(jdbcTemplate, fixture, "PENDING"));
            assertEquals(
                    1,
                    jdbcTemplate.queryForObject(
                            "SELECT COUNT(*) FROM work_invitations WHERE work_case_id = ?",
                            Integer.class,
                            fixture.workCaseId),
                    "경쟁에서 버려진 행이 남으면 안 됩니다."
            );
        } finally {
            executor.shutdownNow();
        }
    }

    private String attemptIssue(
            InvitationIssueService issueService,
            Fixture fixture,
            CountDownLatch start) throws InterruptedException {
        start.await();
        return issueService.issue(fixture.owner(), fixture.workCaseId)
                .getResponse()
                .getInviteUrl();
    }

    private Map<String, Object> activeInvitation(JdbcTemplate jdbcTemplate, Fixture fixture) {
        return jdbcTemplate.queryForMap(
                "SELECT * FROM work_invitations WHERE work_case_id = ? AND status = 'PENDING'",
                fixture.workCaseId);
    }

    private LocalDateTime expiresAtOfActive(JdbcTemplate jdbcTemplate, Fixture fixture) {
        Object value = activeInvitation(jdbcTemplate, fixture).get("expires_at");
        return value instanceof LocalDateTime
                ? (LocalDateTime) value
                : ((java.sql.Timestamp) value).toLocalDateTime();
    }

    private int countInvitations(JdbcTemplate jdbcTemplate, Fixture fixture, String status) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM work_invitations WHERE work_case_id = ? AND status = ?",
                Integer.class,
                fixture.workCaseId,
                status);
    }

    private Fixture insertWorkCaseFixture(JdbcTemplate jdbcTemplate, String suffix) {
        Long ownerUserId = insertUser(jdbcTemplate, "qa155o" + suffix, "OWNER", "초대발급사장");
        Long workerUserId = insertUser(jdbcTemplate, "qa155w" + suffix, "WORKER", "초대발급알바");

        String businessNumber = String.format(
                "%010d", ThreadLocalRandom.current().nextLong(1_000_000_0000L));
        jdbcTemplate.update(
                "INSERT INTO workplaces (owner_user_id, business_registration_number, name, "
                        + "representative_name, road_address, phone) "
                        + "VALUES (?, ?, '강남점', '김사장', '서울 강남구 테헤란로 1', '0212345678')",
                ownerUserId,
                businessNumber);
        Long workplaceId = jdbcTemplate.queryForObject(
                "SELECT id FROM workplaces WHERE business_registration_number = ?",
                Long.class,
                businessNumber);

        // 발급은 시작 전 근무에만 가능하므로 실제 현재 시각보다 뒤에 두어야 합니다.
        // DATETIME(6) 비교라 마이크로초까지 잘라 저장값과 응답을 그대로 비교합니다.
        LocalDateTime startsAt = LocalDateTime.now()
                .plusDays(30L)
                .truncatedTo(ChronoUnit.MICROS);
        jdbcTemplate.update(
                "INSERT INTO work_cases (employer_id, workplace_id, title, starts_at, ends_at, "
                        + "break_minutes, break_paid, workplace_name, workplace_address, "
                        + "agreed_wage, terms_version, status) "
                        + "VALUES (?, ?, '주말 홀 서빙', ?, ?, 60, 0, '강남점', "
                        + "'서울 강남구 테헤란로 1', 120000, 1, 'DRAFT')",
                ownerUserId,
                workplaceId,
                startsAt,
                startsAt.plusHours(8L));
        Long workCaseId = jdbcTemplate.queryForObject(
                "SELECT id FROM work_cases WHERE employer_id = ?", Long.class, ownerUserId);

        return new Fixture(ownerUserId, workerUserId, workplaceId, workCaseId, startsAt);
    }

    private Long insertUser(JdbcTemplate jdbcTemplate, String loginId, String role, String name) {
        jdbcTemplate.update(
                "INSERT INTO users (login_id, email, password_hash, name, role) "
                        + "VALUES (?, ?, ?, ?, ?)",
                loginId,
                loginId + "@example.com",
                "$2a$10$abcdefghijklmnopqrstuvwxyz0123456789ABCDEFGHIJKLMNOPQR",
                name,
                role);
        return jdbcTemplate.queryForObject(
                "SELECT id FROM users WHERE login_id = ?", Long.class, loginId);
    }

    private void cleanUp(JdbcTemplate jdbcTemplate, Fixture fixture) {
        jdbcTemplate.update(
                "DELETE FROM work_invitations WHERE work_case_id = ?", fixture.workCaseId);
        jdbcTemplate.update("DELETE FROM work_cases WHERE id = ?", fixture.workCaseId);
        jdbcTemplate.update("DELETE FROM workplaces WHERE id = ?", fixture.workplaceId);
        jdbcTemplate.update(
                "DELETE FROM users WHERE id IN (?, ?)",
                fixture.ownerUserId,
                fixture.workerUserId);
    }

    /** 검증에 필요한 Fixture 식별자 묶음입니다. */
    private static final class Fixture {

        private final Long ownerUserId;
        private final Long workerUserId;
        private final Long workplaceId;
        private final Long workCaseId;
        private final LocalDateTime startsAt;

        private Fixture(
                Long ownerUserId,
                Long workerUserId,
                Long workplaceId,
                Long workCaseId,
                LocalDateTime startsAt) {
            this.ownerUserId = ownerUserId;
            this.workerUserId = workerUserId;
            this.workplaceId = workplaceId;
            this.workCaseId = workCaseId;
            this.startsAt = startsAt;
        }

        private AuthPrincipal owner() {
            return new AuthPrincipal(ownerUserId, UserRole.OWNER, "초대발급사장");
        }
    }
}
