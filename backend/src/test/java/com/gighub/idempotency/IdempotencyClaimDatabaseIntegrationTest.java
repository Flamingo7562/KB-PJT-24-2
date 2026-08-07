package com.gighub.idempotency;

import com.gighub.common.exception.ConflictException;
import com.gighub.config.RootConfig;
import com.gighub.idempotency.exception.IdempotencyClaimKeyReusedException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 실제 MySQL에서 멱등 Claim의 범위 격리와 Transaction 경계를 검증합니다.
 *
 * <p>선점 경쟁과 Transaction 전파는 실제 행 잠금과 Commit 경계 없이는 확인할 수 없어
 * {@code database} Tag의 Opt-in Test로 둡니다.</p>
 */
@Tag("database")
class IdempotencyClaimDatabaseIntegrationTest {

    private static final String OPERATION = "INVITATION_ACCEPT";
    private static final String OTHER_OPERATION = "WALLET_FUNDING";
    private static final String RESPONSE_BODY = "{\"data\":{\"workCaseId\":123,\"escrowStatus\":\"HELD\"}}";

    @Test
    @Timeout(90)
    void claimScopeAndTransactionBoundariesHoldOnCurrentMysqlSchema() throws Exception {
        try (AnnotationConfigApplicationContext context =
                     new AnnotationConfigApplicationContext(RootConfig.class)) {
            JdbcTemplate jdbcTemplate = new JdbcTemplate(context.getBean(DataSource.class));
            IdempotencyClaimService claimService = context.getBean(IdempotencyClaimService.class);
            TransactionTemplate transactionTemplate = new TransactionTemplate(
                    context.getBean(PlatformTransactionManager.class));

            String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
            long firstUserId = insertUser(jdbcTemplate, "qa156a" + suffix);
            long secondUserId = insertUser(jdbcTemplate, "qa156b" + suffix);

            try {
                verifyScopeIsolation(claimService, firstUserId, secondUserId, suffix);
                verifyCompletedReplayReturnsStoredResponse(
                        jdbcTemplate, claimService, transactionTemplate, firstUserId, suffix);
                verifyReusedKeyWithDifferentFingerprint(claimService, firstUserId, suffix);
                verifyAbandonSurvivesFailedWork(
                        jdbcTemplate, claimService, transactionTemplate, firstUserId, suffix);
                verifyCompleteRequiresCallerTransaction(claimService, firstUserId, suffix);
                verifyConcurrentClaimsLeaveOneWinner(claimService, firstUserId, suffix);
            } finally {
                cleanUp(jdbcTemplate, firstUserId, secondUserId);
            }
        }
    }

    /** 같은 Key라도 사용자나 Operation이 다르면 서로 막지 않아야 합니다. */
    private void verifyScopeIsolation(
            IdempotencyClaimService claimService,
            long firstUserId,
            long secondUserId,
            String suffix) {
        String key = "scope-" + suffix;

        assertFalse(claimService.claim(firstUserId, OPERATION, key, fingerprint(1)).isReplay());
        // 다른 Operation, 다른 사용자는 같은 Key 문자열을 그대로 쓸 수 있습니다.
        assertFalse(
                claimService.claim(firstUserId, OTHER_OPERATION, key, fingerprint(1)).isReplay());
        assertFalse(claimService.claim(secondUserId, OPERATION, key, fingerprint(1)).isReplay());

        // 같은 범위의 재요청만 처리 중 충돌입니다.
        assertThrows(
                ConflictException.class,
                () -> claimService.claim(firstUserId, OPERATION, key, fingerprint(1))
        );
    }

    /** 완료 Claim은 저장한 응답과 상태를 그대로 재현해야 합니다. */
    private void verifyCompletedReplayReturnsStoredResponse(
            JdbcTemplate jdbcTemplate,
            IdempotencyClaimService claimService,
            TransactionTemplate transactionTemplate,
            long userId,
            String suffix) {
        String key = "replay-" + suffix;
        long claimId = claimService.claim(userId, OPERATION, key, fingerprint(2)).getClaimId();

        // 완료는 본 처리 Transaction 안에서만 가능합니다.
        transactionTemplate.executeWithoutResult(
                status -> claimService.complete(claimId, 200, RESPONSE_BODY));

        Map<String, Object> stored = jdbcTemplate.queryForMap(
                "SELECT status, response_http_status, response_body, completed_at"
                        + " FROM idempotency_requests WHERE id = ?", claimId);
        assertEquals("COMPLETED", stored.get("status"));
        assertEquals(200, ((Number) stored.get("response_http_status")).intValue());
        assertNotNull(stored.get("completed_at"));

        IdempotencyClaimResult replay = claimService.claim(userId, OPERATION, key, fingerprint(2));
        assertTrue(replay.isReplay());
        assertEquals(200, replay.getResponseHttpStatus());
        // JSON Column을 왕복해도 저장한 Body가 그대로 나와야 합니다.
        assertEquals(
                RESPONSE_BODY.replace(" ", ""),
                replay.getResponseBody().replace(" ", "")
        );
    }

    /** 같은 Key를 내용이 다른 요청에 쓰면 완료 여부와 무관하게 거절해야 합니다. */
    private void verifyReusedKeyWithDifferentFingerprint(
            IdempotencyClaimService claimService,
            long userId,
            String suffix) {
        assertThrows(
                IdempotencyClaimKeyReusedException.class,
                () -> claimService.claim(userId, OPERATION, "replay-" + suffix, fingerprint(9))
        );
        assertThrows(
                IdempotencyClaimKeyReusedException.class,
                () -> claimService.claim(userId, OPERATION, "scope-" + suffix, fingerprint(9))
        );
    }

    /**
     * 본 처리가 Rollback돼도 Claim 삭제는 남아야 같은 Key로 다시 시도할 수 있습니다.
     *
     * <p>{@code abandon}이 호출자의 Transaction에 참여하면 삭제까지 함께 되돌아가 Claim이
     * 영원히 남습니다. 별도 Transaction으로 분리한 이유를 여기서 확인합니다.</p>
     */
    private void verifyAbandonSurvivesFailedWork(
            JdbcTemplate jdbcTemplate,
            IdempotencyClaimService claimService,
            TransactionTemplate transactionTemplate,
            long userId,
            String suffix) {
        String key = "abandon-" + suffix;
        long claimId = claimService.claim(userId, OPERATION, key, fingerprint(3)).getClaimId();

        assertThrows(IllegalStateException.class, () -> transactionTemplate.executeWithoutResult(
                status -> {
                    throw new IllegalStateException("본 처리 실패");
                }));
        // 실패한 본 처리가 끝난 뒤에 포기합니다. 잠금을 쥔 채 부르면 스스로 교착합니다.
        claimService.abandon(claimId);

        assertEquals(
                0,
                jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM idempotency_requests WHERE id = ?",
                        Integer.class,
                        claimId)
        );

        // 지워졌으므로 같은 Key로 다시 선점할 수 있습니다.
        assertFalse(claimService.claim(userId, OPERATION, key, fingerprint(3)).isReplay());
    }

    /** 완료를 본 처리 밖에서 부르면 응답만 따로 Commit되므로 막아야 합니다. */
    private void verifyCompleteRequiresCallerTransaction(
            IdempotencyClaimService claimService,
            long userId,
            String suffix) {
        long claimId = claimService
                .claim(userId, OPERATION, "mandatory-" + suffix, fingerprint(4))
                .getClaimId();

        assertThrows(
                IllegalTransactionStateException.class,
                () -> claimService.complete(claimId, 200, RESPONSE_BODY)
        );
    }

    /**
     * 사전 조회로는 막을 수 없는 경로입니다. 두 요청이 동시에 같은 Key를 선점하려 해도
     * 한 요청만 본 처리에 진입해야 합니다.
     */
    private void verifyConcurrentClaimsLeaveOneWinner(
            IdempotencyClaimService claimService,
            long userId,
            String suffix) throws Exception {
        String key = "race-" + suffix;
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            List<Future<Boolean>> results = List.of(
                    executor.submit(() -> attemptClaim(claimService, userId, key, start)),
                    executor.submit(() -> attemptClaim(claimService, userId, key, start))
            );

            start.countDown();

            int winners = 0;
            for (Future<Boolean> result : results) {
                if (Boolean.TRUE.equals(result.get(30L, TimeUnit.SECONDS))) {
                    winners++;
                }
            }

            assertEquals(1, winners, "동시 선점에서 한 요청만 본 처리에 진입해야 합니다.");
        } finally {
            executor.shutdownNow();
        }
    }

    private boolean attemptClaim(
            IdempotencyClaimService claimService,
            long userId,
            String key,
            CountDownLatch start) throws InterruptedException {
        start.await();
        try {
            return !claimService.claim(userId, OPERATION, key, fingerprint(5)).isReplay();
        } catch (ConflictException expected) {
            return false;
        }
    }

    private static byte[] fingerprint(int seed) {
        byte[] value = new byte[32];
        Arrays.fill(value, (byte) seed);
        return value;
    }

    private long insertUser(JdbcTemplate jdbcTemplate, String loginId) {
        jdbcTemplate.update(
                "INSERT INTO users (login_id, email, password_hash, name, role) "
                        + "VALUES (?, ?, ?, ?, 'WORKER')",
                loginId,
                loginId + "@example.com",
                "$2a$10$abcdefghijklmnopqrstuvwxyz0123456789ABCDEFGHIJKLMNOPQR",
                "멱등검증자");
        return jdbcTemplate.queryForObject(
                "SELECT id FROM users WHERE login_id = ?", Long.class, loginId);
    }

    private void cleanUp(JdbcTemplate jdbcTemplate, long firstUserId, long secondUserId) {
        jdbcTemplate.update(
                "DELETE FROM idempotency_requests WHERE user_id IN (?, ?)",
                firstUserId,
                secondUserId);
        jdbcTemplate.update("DELETE FROM users WHERE id IN (?, ?)", firstUserId, secondUserId);
    }
}
