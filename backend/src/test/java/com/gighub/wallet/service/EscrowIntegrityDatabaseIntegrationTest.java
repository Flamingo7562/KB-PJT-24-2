package com.gighub.wallet.service;

import com.gighub.config.RootConfig;
import com.gighub.wallet.idempotency.WalletIdempotencyKeys;
import com.gighub.wallet.service.command.EscrowReleaseCommand;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag("database")
class EscrowIntegrityDatabaseIntegrationTest {

    private static final Long WAGE = 300_000L;

    @Test
    @Timeout(15)
    void releaseUsesHeldOwnerAndEscrowReferenceAndReplaysOnce() {
        try (AnnotationConfigApplicationContext context =
                     new AnnotationConfigApplicationContext(RootConfig.class)) {
            JdbcTemplate jdbcTemplate =
                    new JdbcTemplate(context.getBean(DataSource.class));
            EscrowService escrowService = context.getBean(EscrowService.class);
            EscrowFixture fixture = createEscrowFixture(jdbcTemplate);
            EscrowReleaseCommand command = EscrowReleaseCommand.builder()
                    .workCaseId(fixture.workCaseId())
                    .employerId(fixture.employerId())
                    .idempotencyKey(fixture.releaseKey())
                    .build();

            try {
                escrowService.release(command);

                assertEquals(0L, value(jdbcTemplate,
                        "SELECT locked_balance FROM wallets WHERE id = ?",
                        fixture.employerWalletId()));
                assertEquals(WAGE.longValue(), value(jdbcTemplate,
                        "SELECT available_balance FROM wallets WHERE id = ?",
                        fixture.workerWalletId()));
                assertEquals("RELEASED", text(jdbcTemplate,
                        "SELECT status FROM escrows WHERE id = ?",
                        fixture.escrowId()));
                assertEquals("COMPLETED", text(jdbcTemplate,
                        "SELECT status FROM work_cases WHERE id = ?",
                        fixture.workCaseId()));
                assertEquals(3, count(jdbcTemplate,
                        "SELECT COUNT(*) FROM wallet_transactions"
                                + " WHERE work_case_id = ?",
                        fixture.workCaseId()));

                escrowService.release(command);

                assertEquals(0L, value(jdbcTemplate,
                        "SELECT locked_balance FROM wallets WHERE id = ?",
                        fixture.employerWalletId()));
                assertEquals(WAGE.longValue(), value(jdbcTemplate,
                        "SELECT available_balance FROM wallets WHERE id = ?",
                        fixture.workerWalletId()));
                assertEquals(3, count(jdbcTemplate,
                        "SELECT COUNT(*) FROM wallet_transactions"
                                + " WHERE work_case_id = ?",
                        fixture.workCaseId()));
            } finally {
                deleteEscrowFixture(jdbcTemplate, fixture);
            }
        }
    }

    private EscrowFixture createEscrowFixture(JdbcTemplate jdbcTemplate) {
        String token = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        String digits = UUID.randomUUID().toString().replaceAll("[^0-9]", "")
                + "0000000000";
        String businessNumber = digits.substring(0, 10);
        String ownerLogin = "it_esc_owner_" + token;
        String workerLogin = "it_esc_worker_" + token;
        String releaseKey = "IT-ESC-RELEASE-" + token;

        jdbcTemplate.update(
                "INSERT INTO users"
                        + " (login_id, email, password_hash, name, role, status)"
                        + " VALUES (?, ?, 'integration-test', '합성 고용주', 'OWNER', 'ACTIVE')",
                ownerLogin,
                ownerLogin + "@example.invalid"
        );
        jdbcTemplate.update(
                "INSERT INTO users"
                        + " (login_id, email, password_hash, name, role, status)"
                        + " VALUES (?, ?, 'integration-test', '합성 근로자', 'WORKER', 'ACTIVE')",
                workerLogin,
                workerLogin + "@example.invalid"
        );
        Long employerId = idBy(jdbcTemplate, "users", "login_id", ownerLogin);
        Long workerId = idBy(jdbcTemplate, "users", "login_id", workerLogin);

        jdbcTemplate.update(
                "INSERT INTO wallets"
                        + " (user_id, currency, available_balance, locked_balance)"
                        + " VALUES (?, 'KRW', 0, ?)",
                employerId,
                WAGE
        );
        jdbcTemplate.update(
                "INSERT INTO wallets"
                        + " (user_id, currency, available_balance, locked_balance)"
                        + " VALUES (?, 'KRW', 0, 0)",
                workerId
        );
        Long employerWalletId = idBy(jdbcTemplate, "wallets", "user_id", employerId);
        Long workerWalletId = idBy(jdbcTemplate, "wallets", "user_id", workerId);

        jdbcTemplate.update(
                "INSERT INTO workplaces"
                        + " (owner_user_id, business_registration_number, name,"
                        + " representative_name, address, phone, status)"
                        + " VALUES (?, ?, '통합 테스트 사업장', '합성 대표',"
                        + " '서울특별시 테스트로 1', '02-0000-0000', 'ACTIVE')",
                employerId,
                businessNumber
        );
        Long workplaceId = idBy(
                jdbcTemplate,
                "workplaces",
                "business_registration_number",
                businessNumber
        );

        jdbcTemplate.update(
                "INSERT INTO work_cases"
                        + " (employer_id, worker_id, workplace_id, title,"
                        + " starts_at, ends_at, break_minutes, break_paid,"
                        + " workplace_name, workplace_address, allowed_radius_meters,"
                        + " agreed_wage, terms_version, status)"
                        + " VALUES (?, ?, ?, '통합 테스트 근무',"
                        + " '2030-01-01 09:00:00', '2030-01-01 18:00:00', 60, 0,"
                        + " '통합 테스트 사업장', '서울특별시 테스트로 1', 100,"
                        + " ?, 1, 'ACCEPTED')",
                employerId,
                workerId,
                workplaceId,
                WAGE
        );
        Long workCaseId = jdbcTemplate.queryForObject(
                "SELECT id FROM work_cases WHERE employer_id = ? AND title = '통합 테스트 근무'",
                Long.class,
                employerId
        );

        jdbcTemplate.update(
                "INSERT INTO escrows (work_case_id, amount, status, held_at)"
                        + " VALUES (?, ?, 'HELD', NOW(6))",
                workCaseId,
                WAGE
        );
        Long escrowId = idBy(jdbcTemplate, "escrows", "work_case_id", workCaseId);
        jdbcTemplate.update(
                "INSERT INTO wallet_transactions"
                        + " (wallet_id, work_case_id, transaction_type, amount,"
                        + " available_before, available_after, locked_before,"
                        + " locked_after, reference_type, reference_id,"
                        + " idempotency_key)"
                        + " VALUES (?, ?, 'ESCROW_HOLD', ?, ?, 0, 0, ?,"
                        + " 'ESCROW', ?, ?)",
                employerWalletId,
                workCaseId,
                WAGE,
                WAGE,
                WAGE,
                escrowId,
                WalletIdempotencyKeys.escrowHold("IT-ESC-HOLD-" + token)
        );

        return new EscrowFixture(
                employerId,
                workerId,
                employerWalletId,
                workerWalletId,
                workplaceId,
                workCaseId,
                escrowId,
                releaseKey
        );
    }

    private void deleteEscrowFixture(
            JdbcTemplate jdbcTemplate, EscrowFixture fixture) {
        jdbcTemplate.update(
                "DELETE FROM wallet_transactions WHERE work_case_id = ?",
                fixture.workCaseId()
        );
        jdbcTemplate.update(
                "DELETE FROM escrows WHERE id = ?",
                fixture.escrowId()
        );
        jdbcTemplate.update(
                "DELETE FROM work_cases WHERE id = ?",
                fixture.workCaseId()
        );
        jdbcTemplate.update(
                "DELETE FROM workplaces WHERE id = ?",
                fixture.workplaceId()
        );
        jdbcTemplate.update(
                "DELETE FROM wallets WHERE id IN (?, ?)",
                fixture.employerWalletId(),
                fixture.workerWalletId()
        );
        jdbcTemplate.update(
                "DELETE FROM users WHERE id IN (?, ?)",
                fixture.employerId(),
                fixture.workerId()
        );
    }

    private Long idBy(
            JdbcTemplate jdbcTemplate, String table, String column, Object value) {
        Long id = jdbcTemplate.queryForObject(
                "SELECT id FROM " + table + " WHERE " + column + " = ?",
                Long.class,
                value
        );
        if (id == null) {
            throw new IllegalStateException("통합 테스트 fixture ID를 찾을 수 없습니다.");
        }
        return id;
    }

    private int count(JdbcTemplate jdbcTemplate, String sql, Object argument) {
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, argument);
        return count == null ? 0 : count;
    }

    private long value(JdbcTemplate jdbcTemplate, String sql, Object argument) {
        Long value = jdbcTemplate.queryForObject(sql, Long.class, argument);
        if (value == null) {
            throw new IllegalStateException("통합 테스트 잔액을 찾을 수 없습니다.");
        }
        return value;
    }

    private String text(JdbcTemplate jdbcTemplate, String sql, Object argument) {
        String value = jdbcTemplate.queryForObject(sql, String.class, argument);
        if (value == null) {
            throw new IllegalStateException("통합 테스트 상태를 찾을 수 없습니다.");
        }
        return value;
    }

    private record EscrowFixture(
            Long employerId,
            Long workerId,
            Long employerWalletId,
            Long workerWalletId,
            Long workplaceId,
            Long workCaseId,
            Long escrowId,
            String releaseKey) {
    }
}
