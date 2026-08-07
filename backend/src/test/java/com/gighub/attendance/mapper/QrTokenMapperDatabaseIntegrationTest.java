package com.gighub.attendance.mapper;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import javax.sql.DataSource;

import com.gighub.attendance.mapper.param.QrTokenInsertParam;
import com.gighub.attendance.mapper.result.QrTokenRow;
import com.gighub.config.RootConfig;
import com.gighub.workplace.mapper.WorkplaceMapper;
import com.gighub.workplace.mapper.param.WorkplaceInsertParam;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 실제 MySQL Head Schema에서 고정 QR 저장 계약을 검증합니다.
 *
 * <p>사업장당 활성 QR 하나라는 규칙은 애플리케이션 코드가 아니라
 * {@code uk_qr_tokens_workplace_active} 부분 유니크가 최종 보장합니다. 그 보장과
 * {@code ck_qr_tokens_revoked_at}, 발급자 복합 FK는 Java 단위 검증으로 대신할 수 없어
 * {@code database} Tag의 Opt-in Test로 둡니다.</p>
 */
@Tag("database")
class QrTokenMapperDatabaseIntegrationTest {

    @Test
    @Timeout(60)
    void enforcesSingleActiveQrPerWorkplaceOnCurrentMysqlSchema() {
        try (AnnotationConfigApplicationContext context =
                     new AnnotationConfigApplicationContext(RootConfig.class)) {
            JdbcTemplate jdbcTemplate = new JdbcTemplate(context.getBean(DataSource.class));
            QrTokenMapper qrTokenMapper = context.getBean(QrTokenMapper.class);
            WorkplaceMapper workplaceMapper = context.getBean(WorkplaceMapper.class);

            String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
            Long ownerUserId = insertOwnerFixture(jdbcTemplate, suffix);
            Long strangerUserId = insertOwnerFixture(jdbcTemplate, "s" + suffix.substring(1));
            Long workplaceId = insertWorkplaceFixture(workplaceMapper, ownerUserId);

            try {
                verifyInsertStoresNonceAndActiveStatus(
                        jdbcTemplate, qrTokenMapper, workplaceId, ownerUserId);
                verifySecondActiveInsertIsRejected(qrTokenMapper, workplaceId, ownerUserId);
                verifyRevokeSetsRevokedAtAndFreesTheActiveSlot(
                        jdbcTemplate, qrTokenMapper, workplaceId, ownerUserId);
                verifyIssuerMustOwnTheWorkplace(qrTokenMapper, workplaceId, strangerUserId);
            } finally {
                jdbcTemplate.update("DELETE FROM qr_tokens WHERE workplace_id = ?", workplaceId);
                jdbcTemplate.update("DELETE FROM workplaces WHERE id = ?", workplaceId);
                jdbcTemplate.update("DELETE FROM users WHERE id IN (?, ?)",
                        ownerUserId, strangerUserId);
            }
        }
    }

    /** nonce는 그대로 저장되고 상태와 시각은 서버가 정합니다. */
    private void verifyInsertStoresNonceAndActiveStatus(
            JdbcTemplate jdbcTemplate,
            QrTokenMapper qrTokenMapper,
            Long workplaceId,
            Long ownerUserId) {
        byte[] nonce = nonce();
        QrTokenInsertParam param = param(workplaceId, ownerUserId, nonce);

        assertEquals(1, qrTokenMapper.insertActive(param));
        assertNotNull(param.getId());

        Map<String, Object> stored = jdbcTemplate.queryForMap(
                "SELECT status, revoked_at, active_slot FROM qr_tokens WHERE id = ?",
                param.getId());
        assertEquals("ACTIVE", stored.get("status"));
        assertNull(stored.get("revoked_at"));
        assertEquals(1, ((Number) stored.get("active_slot")).intValue());

        QrTokenRow row = qrTokenMapper.findActiveByWorkplaceId(workplaceId);
        assertNotNull(row);
        assertArrayEquals(nonce, row.getTokenNonce());
        assertEquals(workplaceId, row.getWorkplaceId());
        assertNotNull(row.getCreatedAt());
    }

    /** 두 번째 활성 QR은 부분 유니크가 막습니다. */
    private void verifySecondActiveInsertIsRejected(
            QrTokenMapper qrTokenMapper,
            Long workplaceId,
            Long ownerUserId) {
        assertThrows(DuplicateKeyException.class,
                () -> qrTokenMapper.insertActive(param(workplaceId, ownerUserId, nonce())));
    }

    /** 폐기는 revoked_at을 함께 채워야 하고, 그래야 다음 활성 QR을 넣을 수 있습니다. */
    private void verifyRevokeSetsRevokedAtAndFreesTheActiveSlot(
            JdbcTemplate jdbcTemplate,
            QrTokenMapper qrTokenMapper,
            Long workplaceId,
            Long ownerUserId) {
        assertEquals(1, qrTokenMapper.revokeActiveByWorkplaceId(workplaceId));

        Map<String, Object> revoked = jdbcTemplate.queryForMap(
                "SELECT status, revoked_at FROM qr_tokens WHERE workplace_id = ?", workplaceId);
        assertEquals("REVOKED", revoked.get("status"));
        assertNotNull(revoked.get("revoked_at"));

        assertNull(qrTokenMapper.findActiveByWorkplaceId(workplaceId));
        assertEquals(1, qrTokenMapper.insertActive(param(workplaceId, ownerUserId, nonce())));

        // 활성 QR이 없는 사업장의 폐기는 조용히 0건입니다. 재발급의 최초 발급 경로입니다.
        assertEquals(0, qrTokenMapper.revokeActiveByWorkplaceId(-1L));
    }

    /** 발급자는 반드시 그 사업장의 소유자여야 합니다. 복합 FK가 강제합니다. */
    private void verifyIssuerMustOwnTheWorkplace(
            QrTokenMapper qrTokenMapper,
            Long workplaceId,
            Long strangerUserId) {
        qrTokenMapper.revokeActiveByWorkplaceId(workplaceId);

        assertThrows(DataAccessException.class,
                () -> qrTokenMapper.insertActive(param(workplaceId, strangerUserId, nonce())));
    }

    private static QrTokenInsertParam param(Long workplaceId, Long issuerId, byte[] nonce) {
        QrTokenInsertParam param = new QrTokenInsertParam();
        param.setWorkplaceId(workplaceId);
        param.setIssuedByUserId(issuerId);
        param.setTokenNonce(nonce);
        return param;
    }

    /** nonce는 전역 Unique이므로 실행마다 달라야 이전에 중단된 실행과 충돌하지 않습니다. */
    private static byte[] nonce() {
        byte[] value = new byte[16];
        ThreadLocalRandom.current().nextBytes(value);
        return value;
    }

    private Long insertOwnerFixture(JdbcTemplate jdbcTemplate, String suffix) {
        jdbcTemplate.update(
                "INSERT INTO users (login_id, email, password_hash, name, role) "
                        + "VALUES (?, ?, ?, ?, 'OWNER')",
                "qr162" + suffix,
                "qr162" + suffix + "@example.com",
                "$2a$10$0000000000000000000000000000000000000000000000000000",
                "김사장");

        return jdbcTemplate.queryForObject(
                "SELECT id FROM users WHERE login_id = ?", Long.class, "qr162" + suffix);
    }

    private Long insertWorkplaceFixture(WorkplaceMapper workplaceMapper, Long ownerUserId) {
        WorkplaceInsertParam param = WorkplaceInsertParam.builder()
                .ownerUserId(ownerUserId)
                .businessRegistrationNumber(
                        String.format("%010d", ThreadLocalRandom.current().nextLong(1_000_000_000L)))
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
