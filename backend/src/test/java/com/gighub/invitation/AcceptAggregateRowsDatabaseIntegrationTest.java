package com.gighub.invitation;

import com.gighub.config.RootConfig;
import com.gighub.settlement.mapper.SettlementMapper;
import com.gighub.wallet.mapper.WalletMapper;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 수락이 만드는 에스크로·정산 행의 초기값과 제약을 실제 MySQL에서 검증합니다.
 *
 * <p>두 행 모두 근무당 하나이고 금액이 근무의 일급과 같아야 합니다. 이 불변식은 복합 FK와
 * UNIQUE가 지키므로 Java 검증만으로는 확인할 수 없습니다.</p>
 */
@Tag("database")
class AcceptAggregateRowsDatabaseIntegrationTest {

    private static final long WAGE = 120_000L;

    @Test
    @Timeout(60)
    void escrowAndSettlementRowsUseApprovedInitialValuesAndConstraints() {
        try (AnnotationConfigApplicationContext context =
                     new AnnotationConfigApplicationContext(RootConfig.class)) {
            JdbcTemplate jdbcTemplate = new JdbcTemplate(context.getBean(DataSource.class));
            WalletMapper walletMapper = context.getBean(WalletMapper.class);
            SettlementMapper settlementMapper = context.getBean(SettlementMapper.class);

            String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
            Fixture fixture = insertAcceptedWorkCase(jdbcTemplate, suffix);

            try {
                verifyEscrowUsesSharedAcceptedAt(jdbcTemplate, walletMapper, fixture);
                verifySettlementStartsWaitingWithEmptyProgress(
                        jdbcTemplate, settlementMapper, fixture);
                verifyOneRowPerWorkCase(walletMapper, settlementMapper, fixture);
                verifyAmountMustMatchTheAgreedWage(walletMapper, settlementMapper, fixture);
            } finally {
                cleanUp(jdbcTemplate, fixture);
            }
        }
    }

    /** 에스크로는 Aggregate가 공유하는 시각을 그대로 남겨야 합니다. */
    private void verifyEscrowUsesSharedAcceptedAt(
            JdbcTemplate jdbcTemplate,
            WalletMapper walletMapper,
            Fixture fixture) {
        assertEquals(
                1,
                walletMapper.insertHeldEscrowAt(fixture.workCaseId, WAGE, fixture.acceptedAt));

        Map<String, Object> escrow = jdbcTemplate.queryForMap(
                "SELECT amount, status, held_at, released_at, refunded_at"
                        + " FROM escrows WHERE work_case_id = ?", fixture.workCaseId);

        assertEquals(WAGE, ((Number) escrow.get("amount")).longValue());
        assertEquals("HELD", escrow.get("status"));
        // NOW(6)가 아니라 인자로 받은 시각이어야 계약·원장과 같은 순간을 가리킵니다.
        assertEquals(fixture.acceptedAt, toLocalDateTime(escrow.get("held_at")));
        assertNull(escrow.get("released_at"));
        assertNull(escrow.get("refunded_at"));
    }

    /** 정산은 예약만 하고 진행 관련 필드는 비어 있어야 합니다. */
    private void verifySettlementStartsWaitingWithEmptyProgress(
            JdbcTemplate jdbcTemplate,
            SettlementMapper settlementMapper,
            Fixture fixture) {
        assertEquals(1, settlementMapper.insertWaiting(fixture.workCaseId, WAGE));

        Map<String, Object> settlement = jdbcTemplate.queryForMap(
                "SELECT amount, status, due_at, approved_by_user_id, processing_at,"
                        + " completed_at, failure_code"
                        + " FROM settlements WHERE work_case_id = ?", fixture.workCaseId);

        assertEquals(WAGE, ((Number) settlement.get("amount")).longValue());
        assertEquals("WAITING", settlement.get("status"));
        assertNull(settlement.get("due_at"));
        assertNull(settlement.get("approved_by_user_id"));
        assertNull(settlement.get("processing_at"));
        assertNull(settlement.get("completed_at"));
        assertNull(settlement.get("failure_code"));
        assertNotNull(settlement.get("amount"));
    }

    /** 중복 수락이 검증을 모두 통과하더라도 UNIQUE가 마지막 방어선입니다. */
    private void verifyOneRowPerWorkCase(
            WalletMapper walletMapper,
            SettlementMapper settlementMapper,
            Fixture fixture) {
        assertThrows(
                DuplicateKeyException.class,
                () -> walletMapper.insertHeldEscrowAt(fixture.workCaseId, WAGE, fixture.acceptedAt)
        );
        assertThrows(
                DuplicateKeyException.class,
                () -> settlementMapper.insertWaiting(fixture.workCaseId, WAGE)
        );
    }

    /**
     * 금액이 근무의 일급과 다르면 복합 FK가 막아야 합니다.
     *
     * <p>애플리케이션이 다른 금액을 계산하더라도 예치액과 정산액이 계약 금액에서 벗어날 수
     * 없다는 뜻입니다.</p>
     */
    private void verifyAmountMustMatchTheAgreedWage(
            WalletMapper walletMapper,
            SettlementMapper settlementMapper,
            Fixture fixture) {
        long otherWorkCaseId = fixture.secondWorkCaseId;

        assertThrows(
                DataIntegrityViolationException.class,
                () -> walletMapper.insertHeldEscrowAt(
                        otherWorkCaseId, WAGE + 1L, fixture.acceptedAt)
        );
        assertThrows(
                DataIntegrityViolationException.class,
                () -> settlementMapper.insertWaiting(otherWorkCaseId, WAGE + 1L)
        );
    }

    private static LocalDateTime toLocalDateTime(Object value) {
        return value instanceof LocalDateTime
                ? (LocalDateTime) value
                : ((java.sql.Timestamp) value).toLocalDateTime();
    }

    private Fixture insertAcceptedWorkCase(JdbcTemplate jdbcTemplate, String suffix) {
        long ownerUserId = insertUser(jdbcTemplate, "qa156o" + suffix, "OWNER", "예치검증사장");
        long workerUserId = insertUser(jdbcTemplate, "qa156w" + suffix, "WORKER", "예치검증알바");

        String businessNumber = String.format(
                "%010d", ThreadLocalRandom.current().nextLong(1_000_000_0000L));
        jdbcTemplate.update(
                "INSERT INTO workplaces (owner_user_id, business_registration_number, name, "
                        + "representative_name, road_address, phone) "
                        + "VALUES (?, ?, '강남점', '김사장', '서울 강남구 테헤란로 1', '0212345678')",
                ownerUserId,
                businessNumber);
        long workplaceId = jdbcTemplate.queryForObject(
                "SELECT id FROM workplaces WHERE business_registration_number = ?",
                Long.class,
                businessNumber);

        LocalDateTime startsAt = LocalDateTime.now().plusDays(30L).truncatedTo(ChronoUnit.MICROS);
        long first = insertWorkCase(jdbcTemplate, ownerUserId, workerUserId, workplaceId, startsAt);
        long second = insertWorkCase(
                jdbcTemplate, ownerUserId, workerUserId, workplaceId, startsAt.plusDays(1L));

        return new Fixture(
                ownerUserId,
                workerUserId,
                workplaceId,
                first,
                second,
                startsAt.minusDays(1L));
    }

    /** 에스크로·정산은 매칭된 근무에만 붙으므로 ACCEPTED 상태로 만듭니다. */
    private long insertWorkCase(
            JdbcTemplate jdbcTemplate,
            long ownerUserId,
            long workerUserId,
            long workplaceId,
            LocalDateTime startsAt) {
        jdbcTemplate.update(
                "INSERT INTO work_cases (employer_id, worker_id, workplace_id, title, starts_at,"
                        + " ends_at, break_minutes, break_paid, workplace_name, workplace_address,"
                        + " agreed_wage, terms_version, status)"
                        + " VALUES (?, ?, ?, '주말 홀 서빙', ?, ?, 60, 0, '강남점',"
                        + " '서울 강남구 테헤란로 1', ?, 1, 'ACCEPTED')",
                ownerUserId,
                workerUserId,
                workplaceId,
                startsAt,
                startsAt.plusHours(8L),
                WAGE);
        return jdbcTemplate.queryForObject(
                "SELECT id FROM work_cases WHERE employer_id = ? AND starts_at = ?",
                Long.class,
                ownerUserId,
                startsAt);
    }

    private long insertUser(
            JdbcTemplate jdbcTemplate, String loginId, String role, String name) {
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
                "DELETE FROM settlements WHERE work_case_id IN (?, ?)",
                fixture.workCaseId,
                fixture.secondWorkCaseId);
        jdbcTemplate.update(
                "DELETE FROM escrows WHERE work_case_id IN (?, ?)",
                fixture.workCaseId,
                fixture.secondWorkCaseId);
        jdbcTemplate.update(
                "DELETE FROM work_cases WHERE id IN (?, ?)",
                fixture.workCaseId,
                fixture.secondWorkCaseId);
        jdbcTemplate.update("DELETE FROM workplaces WHERE id = ?", fixture.workplaceId);
        jdbcTemplate.update(
                "DELETE FROM users WHERE id IN (?, ?)",
                fixture.ownerUserId,
                fixture.workerUserId);
    }

    /** 검증에 필요한 Fixture 식별자 묶음입니다. */
    private static final class Fixture {

        private final long ownerUserId;
        private final long workerUserId;
        private final long workplaceId;
        private final long workCaseId;
        private final long secondWorkCaseId;
        private final LocalDateTime acceptedAt;

        private Fixture(
                long ownerUserId,
                long workerUserId,
                long workplaceId,
                long workCaseId,
                long secondWorkCaseId,
                LocalDateTime acceptedAt) {
            this.ownerUserId = ownerUserId;
            this.workerUserId = workerUserId;
            this.workplaceId = workplaceId;
            this.workCaseId = workCaseId;
            this.secondWorkCaseId = secondWorkCaseId;
            this.acceptedAt = acceptedAt;
        }
    }
}
