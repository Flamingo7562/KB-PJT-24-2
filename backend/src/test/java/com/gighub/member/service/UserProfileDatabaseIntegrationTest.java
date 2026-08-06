package com.gighub.member.service;

import java.util.UUID;

import javax.sql.DataSource;

import com.gighub.common.exception.ResourceNotFoundException;
import com.gighub.config.RootConfig;
import com.gighub.member.domain.UserRole;
import com.gighub.member.domain.UserStatus;
import com.gighub.member.dto.UserProfileResponse;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Tag("database")
class UserProfileDatabaseIntegrationTest {

    @Test
    @Timeout(25)
    void readsAndUpdatesProfileOnCurrentMysqlSchema() {
        try (AnnotationConfigApplicationContext context =
                     new AnnotationConfigApplicationContext(RootConfig.class)) {
            JdbcTemplate jdbcTemplate = new JdbcTemplate(context.getBean(DataSource.class));
            UserService userService = context.getBean(UserService.class);

            String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
            String loginId = "qa144" + suffix;

            try {
                Long userId = insertOwner(jdbcTemplate, loginId);

                verifyApprovedProfileColumns(userService, userId, loginId);
                verifyPhoneUpdatePersistsNormalizedDigits(jdbcTemplate, userService, userId);
                verifyPhoneCanBeCleared(jdbcTemplate, userService, userId);
                verifyMissingUserIsRejected(userService);
            } finally {
                jdbcTemplate.update("DELETE FROM users WHERE login_id = ?", loginId);
            }
        }
    }

    private Long insertOwner(JdbcTemplate jdbcTemplate, String loginId) {
        jdbcTemplate.update(
                "INSERT INTO users"
                        + " (login_id, email, password_hash, name, phone, role, status)"
                        + " VALUES (?, ?, '$2a$10$notarealhashnotarealhashnotarealhashnotarealha',"
                        + " '김사장', '01012345678', 'OWNER', 'ACTIVE')",
                loginId,
                loginId + "@example.com"
        );
        return jdbcTemplate.queryForObject(
                "SELECT id FROM users WHERE login_id = ?", Long.class, loginId);
    }

    private void verifyApprovedProfileColumns(
            UserService userService, Long userId, String loginId) {
        UserProfileResponse profile = userService.getProfile(userId);

        assertEquals(loginId, profile.getLoginId());
        assertEquals(loginId + "@example.com", profile.getEmail());
        assertEquals("김사장", profile.getName());
        assertEquals("01012345678", profile.getPhone());
        assertEquals(UserRole.OWNER, profile.getRole());
        assertEquals(UserStatus.ACTIVE, profile.getStatus());
    }

    private void verifyPhoneUpdatePersistsNormalizedDigits(
            JdbcTemplate jdbcTemplate, UserService userService, Long userId) {
        UserProfileResponse updated = userService.updatePhone(userId, "01099998888");

        assertEquals("01099998888", updated.getPhone());
        assertEquals("01099998888", jdbcTemplate.queryForObject(
                "SELECT phone FROM users WHERE id = ?", String.class, userId));
    }

    private void verifyPhoneCanBeCleared(
            JdbcTemplate jdbcTemplate, UserService userService, Long userId) {
        UserProfileResponse cleared = userService.updatePhone(userId, null);

        assertNull(cleared.getPhone());
        assertNull(jdbcTemplate.queryForObject(
                "SELECT phone FROM users WHERE id = ?", String.class, userId));
    }

    private void verifyMissingUserIsRejected(UserService userService) {
        assertThrows(
                ResourceNotFoundException.class,
                () -> userService.getProfile(-1L));
    }
}