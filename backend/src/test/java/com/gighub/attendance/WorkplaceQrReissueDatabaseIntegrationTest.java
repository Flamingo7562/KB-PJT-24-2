package com.gighub.attendance;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import javax.sql.DataSource;

import com.gighub.attendance.service.WorkplaceQrIssuer;
import com.gighub.attendance.service.WorkplaceQrService;
import com.gighub.auth.security.AuthPrincipal;
import com.gighub.config.RootConfig;
import com.gighub.member.domain.UserRole;
import com.gighub.workplace.mapper.WorkplaceMapper;
import com.gighub.workplace.mapper.param.WorkplaceInsertParam;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 실제 MySQL에서 동시 재발급이 활성 QR을 하나로 유지하는지 검증합니다.
 *
 * <p>행 잠금과 부분 유니크의 상호작용은 Mock으로 재현할 수 없습니다.</p>
 */
@Tag("database")
class WorkplaceQrReissueDatabaseIntegrationTest {

    private static final int THREADS = 8;

    @Test
    @Timeout(90)
    void concurrentReissuesLeaveExactlyOneActiveQr() throws Exception {
        try (AnnotationConfigApplicationContext context =
                     new AnnotationConfigApplicationContext(RootConfig.class)) {
            JdbcTemplate jdbcTemplate = new JdbcTemplate(context.getBean(DataSource.class));
            WorkplaceQrService service = context.getBean(WorkplaceQrService.class);
            WorkplaceMapper workplaceMapper = context.getBean(WorkplaceMapper.class);

            String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
            Long ownerUserId = insertOwnerFixture(jdbcTemplate, suffix);
            Long workplaceId = insertWorkplaceFixture(workplaceMapper, ownerUserId);

            // Mapper로 만든 사업장에는 QR이 없습니다. 활성 QR을 미리 넣어야 재발급이 폐기를
            // 거쳐야만 성공하는 실제 상황이 됩니다. 이게 없으면 폐기를 빼도 첫 요청이
            // 그냥 성공해 이 테스트가 아무것도 증명하지 못합니다.
            context.getBean(WorkplaceQrIssuer.class).issueActive(workplaceId, ownerUserId);

            try {
                runConcurrentReissues(service, ownerUserId, workplaceId);

                // 몇 건이 성공했든 활성 QR은 언제나 정확히 하나여야 합니다.
                assertEquals(1, jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM qr_tokens "
                                + "WHERE workplace_id = ? AND status = 'ACTIVE'",
                        Integer.class, workplaceId));

                // 폐기된 QR은 반드시 폐기 시각을 갖습니다.
                assertEquals(0, jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM qr_tokens WHERE workplace_id = ? "
                                + "AND status = 'REVOKED' AND revoked_at IS NULL",
                        Integer.class, workplaceId));
            } finally {
                jdbcTemplate.update("DELETE FROM qr_tokens WHERE workplace_id = ?", workplaceId);
                jdbcTemplate.update("DELETE FROM workplaces WHERE id = ?", workplaceId);
                jdbcTemplate.update("DELETE FROM users WHERE id = ?", ownerUserId);
            }
        }
    }

    private void runConcurrentReissues(
            WorkplaceQrService service,
            Long ownerUserId,
            Long workplaceId) throws Exception {
        AuthPrincipal owner = new AuthPrincipal(ownerUserId, UserRole.OWNER, "김사장");
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(THREADS);

        List<Callable<Boolean>> jobs = IntStream.range(0, THREADS)
                .<Callable<Boolean>>mapToObj(index -> () -> {
                    start.await();
                    try {
                        service.reissue(owner, workplaceId);
                        return true;
                    } catch (RuntimeException expected) {
                        // 경쟁에서 진 요청은 승인된 충돌이나 잠금 실패로 끝납니다.
                        return false;
                    }
                })
                .toList();

        List<Future<Boolean>> futures = jobs.stream().map(pool::submit).toList();
        start.countDown();
        pool.shutdown();
        assertTrue(pool.awaitTermination(60, TimeUnit.SECONDS));

        long succeeded = futures.stream().filter(WorkplaceQrReissueDatabaseIntegrationTest::result)
                .count();
        assertTrue(succeeded >= 1, "적어도 한 재발급은 성공해야 합니다.");
    }

    private static boolean result(Future<Boolean> future) {
        try {
            return future.get();
        } catch (Exception exception) {
            return false;
        }
    }

    private Long insertOwnerFixture(JdbcTemplate jdbcTemplate, String suffix) {
        jdbcTemplate.update(
                "INSERT INTO users (login_id, email, password_hash, name, role) "
                        + "VALUES (?, ?, ?, ?, 'OWNER')",
                "qr162r" + suffix,
                "qr162r" + suffix + "@example.com",
                "$2a$10$0000000000000000000000000000000000000000000000000000",
                "김사장");

        return jdbcTemplate.queryForObject(
                "SELECT id FROM users WHERE login_id = ?", Long.class, "qr162r" + suffix);
    }

    private Long insertWorkplaceFixture(WorkplaceMapper workplaceMapper, Long ownerUserId) {
        WorkplaceInsertParam param = WorkplaceInsertParam.builder()
                .ownerUserId(ownerUserId)
                .businessRegistrationNumber(String.format(
                        "%010d", ThreadLocalRandom.current().nextLong(1_000_000_000L)))
                .name("강남점")
                .representativeName("김사장")
                .roadAddress("서울 강남구 테헤란로 1")
                .detailAddress("2층")
                .phone("0212345678")
                .build();

        workplaceMapper.insert(param);
        return param.getId();
    }
}
