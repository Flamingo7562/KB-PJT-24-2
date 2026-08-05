package com.gighub.bank;

import com.gighub.config.RootConfig;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Tag("database")
class MockBankAccountPinSchemaDatabaseIntegrationTest {

    @Test
    void enforcesNonOwnedPinSchemaAndPreservesAccountReferences() {
        try (AnnotationConfigApplicationContext context =
                     new AnnotationConfigApplicationContext(RootConfig.class)) {
            JdbcTemplate jdbcTemplate =
                    new JdbcTemplate(context.getBean(DataSource.class));
            String token = UUID.randomUUID().toString().replace("-", "");
            long userId = insertUser(jdbcTemplate, token);
            long walletId = insertWallet(jdbcTemplate, userId);
            long accountId = insertAccount(jdbcTemplate, token, "0000", 10_000L, 10_000L);

            try {
                assertPinColumn(jdbcTemplate);
                assertRemovedOwnershipArtifacts(jdbcTemplate);
                assertAccountReference(
                        jdbcTemplate,
                        "funding_orders",
                        "fk_funding_orders_linked_account",
                        "linked_account_id"
                );
                assertAccountReference(
                        jdbcTemplate,
                        "withdrawal_requests",
                        "fk_withdrawal_requests_linked_account",
                        "linked_account_id"
                );
                assertAccountReference(
                        jdbcTemplate,
                        "mock_bank_transactions",
                        "fk_mock_bank_transactions_account",
                        "account_id"
                );
                assertThrows(
                        DataAccessException.class,
                        () -> insertAccount(
                                jdbcTemplate, token + "a", "12a4", 10_000L, 10_000L)
                );
                assertThrows(
                        DataAccessException.class,
                        () -> insertAccount(
                                jdbcTemplate, token + "b", "123", 10_000L, 10_000L)
                );
                assertThrows(
                        DataAccessException.class,
                        () -> insertAccount(
                                jdbcTemplate, token + "c", "12345", 10_000L, 10_000L)
                );
                assertThrows(
                        DataAccessException.class,
                        () -> insertAccount(
                                jdbcTemplate, token + "d", "0000", 10_000L, 10_001L)
                );
                long defaultPinAccountId = insertAccountWithDefaultPin(
                        jdbcTemplate, token + "e", 10_000L, 10_000L);
                try {
                    assertEquals("0000", jdbcTemplate.queryForObject(
                            "SELECT pin FROM mock_bank_accounts WHERE id = ?",
                            String.class,
                            defaultPinAccountId
                    ));
                } finally {
                    jdbcTemplate.update(
                            "DELETE FROM mock_bank_accounts WHERE id = ?", defaultPinAccountId);
                }

                insertAccountReferences(jdbcTemplate, token, userId, walletId, accountId);
                assertEquals(1, countByAccount(
                        jdbcTemplate, "funding_orders", "linked_account_id", accountId));
                assertEquals(1, countByAccount(
                        jdbcTemplate, "withdrawal_requests", "linked_account_id", accountId));
                assertEquals(1, countByAccount(
                        jdbcTemplate, "mock_bank_transactions", "account_id", accountId));

                // 세 업무 참조가 남아 있는 동안 계좌 PK 삭제를 막는 RESTRICT 관계를 확인한다.
                assertThrows(
                        DataAccessException.class,
                        () -> jdbcTemplate.update(
                                "DELETE FROM mock_bank_accounts WHERE id = ?", accountId)
                );
            } finally {
                deleteFixture(jdbcTemplate, token, userId, walletId, accountId);
            }
        }
    }

    private void assertPinColumn(JdbcTemplate jdbcTemplate) {
        Map<String, Object> column = jdbcTemplate.queryForMap(
                "SELECT COLUMN_TYPE, IS_NULLABLE, COLUMN_DEFAULT,"
                        + " CHARACTER_SET_NAME, COLLATION_NAME"
                        + " FROM information_schema.columns"
                        + " WHERE table_schema = DATABASE()"
                        + " AND table_name = 'mock_bank_accounts'"
                        + " AND column_name = 'pin'"
        );
        assertEquals("char(4)", column.get("COLUMN_TYPE"));
        assertEquals("NO", column.get("IS_NULLABLE"));
        assertEquals("0000", column.get("COLUMN_DEFAULT"));
        assertEquals("ascii", column.get("CHARACTER_SET_NAME"));
        assertEquals("ascii_bin", column.get("COLLATION_NAME"));
    }

    private void assertRemovedOwnershipArtifacts(JdbcTemplate jdbcTemplate) {
        assertEquals(0, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns"
                        + " WHERE table_schema = DATABASE()"
                        + " AND table_name = 'mock_bank_accounts'"
                        + " AND column_name = 'user_id'",
                Integer.class
        ));
        assertEquals(0, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.statistics"
                        + " WHERE table_schema = DATABASE()"
                        + " AND table_name = 'mock_bank_accounts'"
                        + " AND index_name = 'idx_mock_bank_accounts_user_status'",
                Integer.class
        ));
        assertEquals(3, jdbcTemplate.queryForObject(
                "SELECT COUNT(DISTINCT index_name)"
                        + " FROM information_schema.statistics"
                        + " WHERE table_schema = DATABASE()"
                        + " AND table_name = 'mock_bank_accounts'",
                Integer.class
        ));
        assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT COUNT(DISTINCT index_name)"
                        + " FROM information_schema.statistics"
                        + " WHERE table_schema = DATABASE()"
                        + " AND table_name = 'mock_bank_accounts'"
                        + " AND index_name = 'uk_mock_bank_accounts_bank_account'",
                Integer.class
        ));
        assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT COUNT(DISTINCT index_name)"
                        + " FROM information_schema.statistics"
                        + " WHERE table_schema = DATABASE()"
                        + " AND table_name = 'mock_bank_accounts'"
                        + " AND index_name = 'uk_mock_bank_accounts_fintech_use_num'",
                Integer.class
        ));
        assertEquals(0, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.referential_constraints"
                        + " WHERE constraint_schema = DATABASE()"
                        + " AND table_name = 'mock_bank_accounts'"
                        + " AND constraint_name = 'fk_mock_bank_accounts_user'",
                Integer.class
        ));
        assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.table_constraints"
                        + " WHERE constraint_schema = DATABASE()"
                        + " AND table_name = 'mock_bank_accounts'"
                        + " AND constraint_type = 'CHECK'"
                        + " AND constraint_name = 'ck_mock_bank_accounts_pin'",
                Integer.class
        ));
    }

    private long insertUser(JdbcTemplate jdbcTemplate, String token) {
        String loginId = "it_bank_pin_" + token.substring(0, 12);
        jdbcTemplate.update(
                "INSERT INTO users"
                        + " (login_id, email, password_hash, name, role, status)"
                        + " VALUES (?, ?, 'schema-test-hash', '계좌 PIN 스키마 테스트',"
                        + " 'OWNER', 'ACTIVE')",
                loginId,
                loginId + "@example.invalid"
        );
        return jdbcTemplate.queryForObject(
                "SELECT id FROM users WHERE login_id = ?", Long.class, loginId);
    }

    private void assertAccountReference(
            JdbcTemplate jdbcTemplate,
            String tableName,
            String constraintName,
            String columnName) {
        assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.key_column_usage"
                        + " WHERE constraint_schema = DATABASE()"
                        + " AND table_name = ?"
                        + " AND constraint_name = ?"
                        + " AND column_name = ?"
                        + " AND referenced_table_name = 'mock_bank_accounts'"
                        + " AND referenced_column_name = 'id'",
                Integer.class,
                tableName,
                constraintName,
                columnName
        ));
    }

    private long insertWallet(JdbcTemplate jdbcTemplate, long userId) {
        jdbcTemplate.update(
                "INSERT INTO wallets"
                        + " (user_id, currency, available_balance, locked_balance)"
                        + " VALUES (?, 'KRW', 10000, 0)",
                userId
        );
        return jdbcTemplate.queryForObject(
                "SELECT id FROM wallets WHERE user_id = ?", Long.class, userId);
    }

    private long insertAccount(
            JdbcTemplate jdbcTemplate,
            String token,
            String pin,
            long balance,
            long availableAmount) {
        String accountNumber = "9" + token;
        String fintechUseNumber = "IT-PIN-" + token;
        jdbcTemplate.update(
                "INSERT INTO mock_bank_accounts"
                        + " (bank_code, mock_account_number, pin,"
                        + " mock_fintech_use_num, currency, balance,"
                        + " available_amount, status)"
                        + " VALUES ('999', ?, ?, ?, 'KRW', ?, ?, 'ACTIVE')",
                accountNumber,
                pin,
                fintechUseNumber,
                balance,
                availableAmount
        );
        return jdbcTemplate.queryForObject(
                "SELECT id FROM mock_bank_accounts WHERE mock_fintech_use_num = ?",
                Long.class,
                fintechUseNumber
        );
    }

    private void insertAccountReferences(
            JdbcTemplate jdbcTemplate,
            String token,
            long userId,
            long walletId,
            long accountId) {
        jdbcTemplate.update(
                "INSERT INTO funding_orders"
                        + " (employer_id, linked_account_id, expected_amount, idempotency_key)"
                        + " VALUES (?, ?, 1000, ?)",
                userId,
                accountId,
                "IT-PIN-FUND-" + token
        );
        jdbcTemplate.update(
                "INSERT INTO withdrawal_requests"
                        + " (user_id, wallet_id, linked_account_id, amount, idempotency_key)"
                        + " VALUES (?, ?, ?, 1000, ?)",
                userId,
                walletId,
                accountId,
                "IT-PIN-WD-" + token
        );
        jdbcTemplate.update(
                "INSERT INTO mock_bank_transactions"
                        + " (account_id, bank_tran_id, transfer_type, amount,"
                        + " balance_before, balance_after, reference_type, reference_id, status)"
                        + " VALUES (?, ?, 'WITHDRAW', 1000, 10000, 9000,"
                        + " 'PIN_SCHEMA_TEST', ?, 'SUCCESS')",
                accountId,
                "IT-PIN-TRAN-" + token,
                accountId
        );
    }

    private long insertAccountWithDefaultPin(
            JdbcTemplate jdbcTemplate,
            String token,
            long balance,
            long availableAmount) {
        String fintechUseNumber = "IT-PIN-" + token;
        jdbcTemplate.update(
                "INSERT INTO mock_bank_accounts"
                        + " (bank_code, mock_account_number, mock_fintech_use_num,"
                        + " currency, balance, available_amount, status)"
                        + " VALUES ('999', ?, ?, 'KRW', ?, ?, 'ACTIVE')",
                "9" + token,
                fintechUseNumber,
                balance,
                availableAmount
        );
        return jdbcTemplate.queryForObject(
                "SELECT id FROM mock_bank_accounts WHERE mock_fintech_use_num = ?",
                Long.class,
                fintechUseNumber
        );
    }

    private int countByAccount(
            JdbcTemplate jdbcTemplate,
            String table,
            String column,
            long accountId) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM " + table + " WHERE " + column + " = ?",
                Integer.class,
                accountId
        );
    }

    private void deleteFixture(
            JdbcTemplate jdbcTemplate,
            String token,
            long userId,
            long walletId,
            long accountId) {
        jdbcTemplate.update(
                "DELETE FROM funding_orders WHERE idempotency_key = ?",
                "IT-PIN-FUND-" + token
        );
        jdbcTemplate.update(
                "DELETE FROM withdrawal_requests WHERE idempotency_key = ?",
                "IT-PIN-WD-" + token
        );
        jdbcTemplate.update(
                "DELETE FROM mock_bank_transactions WHERE bank_tran_id = ?",
                "IT-PIN-TRAN-" + token
        );
        jdbcTemplate.update("DELETE FROM mock_bank_accounts WHERE id = ?", accountId);
        jdbcTemplate.update("DELETE FROM wallets WHERE id = ?", walletId);
        jdbcTemplate.update("DELETE FROM users WHERE id = ?", userId);
    }
}
