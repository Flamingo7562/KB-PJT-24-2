package com.gighub.idempotency;

import com.gighub.config.RootConfig;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.util.Arrays;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Tag("database")
class IdempotencyRequestSchemaDatabaseIntegrationTest {

    @Test
    void enforcesClaimScopeAndMinimalLifecycle() {
        try (AnnotationConfigApplicationContext context =
                     new AnnotationConfigApplicationContext(RootConfig.class)) {
            JdbcTemplate jdbcTemplate =
                    new JdbcTemplate(context.getBean(DataSource.class));
            String token = UUID.randomUUID().toString().replace("-", "");
            String firstLoginId = "it_idem_a_" + token;
            String secondLoginId = "it_idem_b_" + token;
            String key = "IT-IDEM-" + token;

            try {
                long firstUserId = insertUser(jdbcTemplate, firstLoginId, token + "a");
                long secondUserId = insertUser(jdbcTemplate, secondLoginId, token + "b");
                byte[] fingerprint = fingerprint((byte) 1);

                insertProcessingClaim(
                        jdbcTemplate,
                        firstUserId,
                        "WALLET_FUNDING",
                        key,
                        fingerprint
                );

                assertThrows(
                        DuplicateKeyException.class,
                        () -> insertProcessingClaim(
                                jdbcTemplate,
                                firstUserId,
                                "WALLET_FUNDING",
                                key,
                                fingerprint((byte) 2)
                        )
                );

                insertProcessingClaim(
                        jdbcTemplate,
                        firstUserId,
                        "WALLET_WITHDRAWAL",
                        key,
                        fingerprint
                );
                insertProcessingClaim(
                        jdbcTemplate,
                        secondUserId,
                        "WALLET_FUNDING",
                        key,
                        fingerprint
                );

                assertEquals(
                        3,
                        jdbcTemplate.queryForObject(
                                "SELECT COUNT(*) FROM idempotency_requests"
                                        + " WHERE idempotency_key = ?",
                                Integer.class,
                                key
                        )
                );

                assertThrows(
                        DataAccessException.class,
                        () -> insertIncompleteSuccess(
                                jdbcTemplate,
                                firstUserId,
                                key + "-INVALID",
                                fingerprint
                        )
                );

                insertCompletedClaim(
                        jdbcTemplate,
                        firstUserId,
                        key + "-COMPLETED",
                        fingerprint
                );
            } finally {
                jdbcTemplate.update(
                        "DELETE FROM idempotency_requests"
                                + " WHERE idempotency_key LIKE ?",
                        key + "%"
                );
                jdbcTemplate.update(
                        "DELETE FROM users WHERE login_id IN (?, ?)",
                        firstLoginId,
                        secondLoginId
                );
            }
        }
    }

    private long insertUser(
            JdbcTemplate jdbcTemplate,
            String loginId,
            String emailToken) {
        jdbcTemplate.update(
                "INSERT INTO users"
                        + " (login_id, email, password_hash, name, role, status)"
                        + " VALUES (?, ?, 'schema-test-hash', '멱등 스키마 테스트',"
                        + " 'OWNER', 'ACTIVE')",
                loginId,
                emailToken + "@example.test"
        );
        return jdbcTemplate.queryForObject(
                "SELECT id FROM users WHERE login_id = ?",
                Long.class,
                loginId
        );
    }

    private void insertProcessingClaim(
            JdbcTemplate jdbcTemplate,
            long userId,
            String operationCode,
            String key,
            byte[] fingerprint) {
        jdbcTemplate.update(
                "INSERT INTO idempotency_requests"
                        + " (user_id, operation_code, idempotency_key,"
                        + " request_fingerprint, expires_at)"
                        + " VALUES (?, ?, ?, ?,"
                        + " DATE_ADD(CURRENT_TIMESTAMP(6), INTERVAL 24 HOUR))",
                userId,
                operationCode,
                key,
                fingerprint
        );
    }

    private void insertIncompleteSuccess(
            JdbcTemplate jdbcTemplate,
            long userId,
            String key,
            byte[] fingerprint) {
        jdbcTemplate.update(
                "INSERT INTO idempotency_requests"
                        + " (user_id, operation_code, idempotency_key,"
                        + " request_fingerprint, status, completed_at, expires_at)"
                        + " VALUES (?, 'WALLET_FUNDING', ?, ?, 'COMPLETED',"
                        + " CURRENT_TIMESTAMP(6),"
                        + " DATE_ADD(CURRENT_TIMESTAMP(6), INTERVAL 24 HOUR))",
                userId,
                key,
                fingerprint
        );
    }

    private void insertCompletedClaim(
            JdbcTemplate jdbcTemplate,
            long userId,
            String key,
            byte[] fingerprint) {
        jdbcTemplate.update(
                "INSERT INTO idempotency_requests"
                        + " (user_id, operation_code, idempotency_key,"
                        + " request_fingerprint, status, response_http_status,"
                        + " response_body, completed_at, expires_at)"
                        + " VALUES (?, 'WALLET_FUNDING', ?, ?, 'COMPLETED', 201,"
                        + " JSON_OBJECT('fundingOrderId', 1), CURRENT_TIMESTAMP(6),"
                        + " DATE_ADD(CURRENT_TIMESTAMP(6), INTERVAL 24 HOUR))",
                userId,
                key,
                fingerprint
        );
    }

    private byte[] fingerprint(byte value) {
        byte[] fingerprint = new byte[32];
        Arrays.fill(fingerprint, value);
        return fingerprint;
    }
}
