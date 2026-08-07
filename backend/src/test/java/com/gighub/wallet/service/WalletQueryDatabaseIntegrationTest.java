package com.gighub.wallet.service;

import com.gighub.config.RootConfig;
import com.gighub.wallet.dto.WalletBalanceResponse;
import com.gighub.wallet.dto.WalletTransactionItem;
import com.gighub.wallet.service.command.WalletTransactionCriteria;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** 실제 MySQL과 MyBatis로 지갑 거래 필터, 정렬, UTC와 사용자 격리를 검증합니다. */
@Tag("database")
class WalletQueryDatabaseIntegrationTest {

    @Test
    void filtersSortsPagesConvertsUtcAndIsolatesAuthenticatedUserOnMySql() {
        try (AnnotationConfigApplicationContext context =
                     new AnnotationConfigApplicationContext(RootConfig.class)) {
            JdbcTemplate jdbcTemplate = new JdbcTemplate(context.getBean(DataSource.class));
            WalletQueryService service = context.getBean(WalletQueryService.class);
            WalletQueryFixture fixture = createFixture(jdbcTemplate);

            try {
                WalletBalanceResponse balance = service.getWallet(fixture.userId());
                assertEquals(150L, balance.getAvailableBalance());
                assertEquals(100L, balance.getLockedBalance());

                List<WalletTransactionItem> day = service.getTransactions(
                        fixture.userId(),
                        criteriaBuilder()
                                .from(LocalDate.of(2026, 7, 21))
                                .to(LocalDate.of(2026, 7, 21))
                                .build()
                ).getContent();
                assertEquals(2, day.size());
                assertEquals(300L, day.get(0).getAmount());
                assertEquals("2026-07-21T14:59:59.999999Z",
                        day.get(0).getCreatedAt().toString());
                assertEquals("CREDIT", day.get(0).getDirection());
                assertNull(day.get(0).getWorkCaseId());
                assertEquals(100L, day.get(1).getAmount());
                assertEquals("2026-07-20T15:00:00Z",
                        day.get(1).getCreatedAt().toString());
                assertEquals("DEBIT", day.get(1).getDirection());

                List<WalletTransactionItem> workplaceName = service.getTransactions(
                        fixture.userId(),
                        criteriaBuilder().keyword("브라보").build()
                ).getContent();
                assertEquals(1, workplaceName.size());
                assertEquals("브라보 매장", workplaceName.get(0).getWorkplaceName());

                List<WalletTransactionItem> workTitle = service.getTransactions(
                        fixture.userId(),
                        criteriaBuilder().keyword("야간 정산").build()
                ).getContent();
                assertEquals(1, workTitle.size());
                assertEquals("야간 정산 보조", workTitle.get(0).getWorkTitle());

                List<WalletTransactionItem> workplace = service.getTransactions(
                        fixture.userId(),
                        criteriaBuilder().workplaceId(fixture.workplaceId()).build()
                ).getContent();
                assertEquals(1, workplace.size());
                assertEquals(fixture.workCaseId(), workplace.get(0).getWorkCaseId());

                List<WalletTransactionItem> amountOrder = service.getTransactions(
                        fixture.userId(),
                        criteriaBuilder()
                                .minAmount(50L)
                                .maxAmount(300L)
                                .sort("AMOUNT_ASC")
                                .build()
                ).getContent();
                assertEquals(List.of(50L, 100L, 300L),
                        amountOrder.stream().map(WalletTransactionItem::getAmount).toList());

                var secondPage = service.getTransactions(
                        fixture.userId(),
                        criteriaBuilder()
                                .from(LocalDate.of(2026, 7, 21))
                                .to(LocalDate.of(2026, 7, 21))
                                .page(1)
                                .size(1)
                                .build()
                );
                assertEquals(1, secondPage.getContent().size());
                assertEquals(2, secondPage.getPage().getTotalElements());
                assertEquals(2, secondPage.getPage().getTotalPages());
            } finally {
                deleteFixture(jdbcTemplate, fixture);
            }
        }
    }

    private WalletTransactionCriteria.WalletTransactionCriteriaBuilder criteriaBuilder() {
        return WalletTransactionCriteria.builder()
                .sort("LATEST")
                .page(0)
                .size(20);
    }

    private WalletQueryFixture createFixture(JdbcTemplate jdbcTemplate) {
        String token = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        Long userId = insertUser(jdbcTemplate, "it_wallet_owner_" + token, "OWNER");
        Long otherUserId = insertUser(jdbcTemplate, "it_wallet_other_" + token, "WORKER");
        Long walletId = insertWallet(jdbcTemplate, userId, 150L, 100L);
        Long otherWalletId = insertWallet(jdbcTemplate, otherUserId, 999L, 0L);

        String registrationNumber = String.format(
                "%010d",
                Integer.toUnsignedLong(token.hashCode())
        );
        jdbcTemplate.update(
                "INSERT INTO workplaces"
                        + " (owner_user_id, business_registration_number, name,"
                        + " representative_name, road_address, phone, status)"
                        + " VALUES (?, ?, '브라보 매장', '통합 대표', '서울 테스트로 1',"
                        + " '0212345678', 'ACTIVE')",
                userId,
                registrationNumber
        );
        Long workplaceId = jdbcTemplate.queryForObject(
                "SELECT id FROM workplaces WHERE business_registration_number = ?",
                Long.class,
                registrationNumber
        );
        jdbcTemplate.update(
                "INSERT INTO work_cases"
                        + " (employer_id, workplace_id, title, starts_at, ends_at,"
                        + " break_minutes, break_paid, workplace_name, workplace_address,"
                        + " allowed_radius_meters, agreed_wage, terms_version, status)"
                        + " VALUES (?, ?, '야간 정산 보조', '2026-07-21 18:00:00',"
                        + " '2026-07-21 22:00:00', 0, 0, '브라보 매장', '서울 테스트로 1',"
                        + " 100, 100, 1, 'DRAFT')",
                userId,
                workplaceId
        );
        Long workCaseId = jdbcTemplate.queryForObject(
                "SELECT id FROM work_cases WHERE employer_id = ? AND title = '야간 정산 보조'",
                Long.class,
                userId
        );

        insertTransaction(jdbcTemplate, walletId, null, "FUNDING", 300,
                0, 300, 0, 0, token + "-fund", "2026-07-21 23:59:59.999999");
        insertTransaction(jdbcTemplate, walletId, workCaseId, "ESCROW_HOLD", 100,
                300, 200, 0, 100, token + "-hold", "2026-07-21 00:00:00");
        insertTransaction(jdbcTemplate, walletId, null, "WITHDRAWAL", 50,
                200, 150, 100, 100, token + "-withdraw", "2026-07-22 00:00:00");
        insertTransaction(jdbcTemplate, otherWalletId, null, "FUNDING", 999,
                0, 999, 0, 0, token + "-other", "2026-07-21 12:00:00");

        return new WalletQueryFixture(
                userId,
                otherUserId,
                walletId,
                otherWalletId,
                workplaceId,
                workCaseId
        );
    }

    private Long insertUser(JdbcTemplate jdbcTemplate, String loginId, String role) {
        jdbcTemplate.update(
                "INSERT INTO users"
                        + " (login_id, email, password_hash, name, role, status)"
                        + " VALUES (?, ?, 'integration-test', '통합 테스트', ?, 'ACTIVE')",
                loginId,
                loginId + "@example.invalid",
                role
        );
        return jdbcTemplate.queryForObject(
                "SELECT id FROM users WHERE login_id = ?",
                Long.class,
                loginId
        );
    }

    private Long insertWallet(
            JdbcTemplate jdbcTemplate,
            Long userId,
            long available,
            long locked) {
        jdbcTemplate.update(
                "INSERT INTO wallets"
                        + " (user_id, currency, available_balance, locked_balance)"
                        + " VALUES (?, 'KRW', ?, ?)",
                userId,
                available,
                locked
        );
        return jdbcTemplate.queryForObject(
                "SELECT id FROM wallets WHERE user_id = ?",
                Long.class,
                userId
        );
    }

    private void insertTransaction(
            JdbcTemplate jdbcTemplate,
            Long walletId,
            Long workCaseId,
            String type,
            long amount,
            long availableBefore,
            long availableAfter,
            long lockedBefore,
            long lockedAfter,
            String key,
            String createdAt) {
        jdbcTemplate.update(
                "INSERT INTO wallet_transactions"
                        + " (wallet_id, work_case_id, transaction_type, amount,"
                        + " available_before, available_after, locked_before, locked_after,"
                        + " reference_type, reference_id, idempotency_key, created_at)"
                        + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'INTEGRATION_TEST', 1, ?, ?)",
                walletId,
                workCaseId,
                type,
                amount,
                availableBefore,
                availableAfter,
                lockedBefore,
                lockedAfter,
                key,
                createdAt
        );
    }

    private void deleteFixture(JdbcTemplate jdbcTemplate, WalletQueryFixture fixture) {
        jdbcTemplate.update(
                "DELETE FROM wallet_transactions WHERE wallet_id IN (?, ?)",
                fixture.walletId(),
                fixture.otherWalletId()
        );
        jdbcTemplate.update("DELETE FROM work_cases WHERE id = ?", fixture.workCaseId());
        jdbcTemplate.update("DELETE FROM workplaces WHERE id = ?", fixture.workplaceId());
        jdbcTemplate.update(
                "DELETE FROM wallets WHERE id IN (?, ?)",
                fixture.walletId(),
                fixture.otherWalletId()
        );
        jdbcTemplate.update(
                "DELETE FROM users WHERE id IN (?, ?)",
                fixture.userId(),
                fixture.otherUserId()
        );
    }

    private record WalletQueryFixture(
            Long userId,
            Long otherUserId,
            Long walletId,
            Long otherWalletId,
            Long workplaceId,
            Long workCaseId) {
    }
}
