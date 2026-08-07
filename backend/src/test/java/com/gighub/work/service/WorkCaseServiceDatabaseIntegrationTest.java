package com.gighub.work.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import javax.sql.DataSource;

import com.gighub.auth.security.AuthPrincipal;
import com.gighub.common.exception.ResourceNotFoundException;
import com.gighub.common.exception.WorkCaseLockedException;
import com.gighub.config.RootConfig;
import com.gighub.member.domain.UserRole;
import com.gighub.work.service.command.WorkCaseCreateCommand;
import com.gighub.work.service.command.WorkCaseUpdateCommand;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 실제 MySQL Head Schema와 Spring Transaction Proxy 위에서 {@link WorkCaseService} 계약을
 * 검증합니다.
 *
 * <p>{@link WorkCaseServiceImplTest 단위테스트}는 Mapper를 Mockito로 대체해 순수 로직만
 * 검증하고, {@code WorkCaseMapperDatabaseIntegrationTest}는 개별 SQL만 검증합니다. 이 Test는
 * 둘 다 못 보는 지점 — {@code @Service} Bean 조립, {@code @Transactional} Proxy 적용,
 * {@link com.gighub.work.domain.WorkCaseTimes}·{@link com.gighub.work.domain.WorkCaseAddress}를
 * 거친 실제 값이 DB Round Trip에서 그대로 맞는지 — 를 확인합니다.
 *
 * <p>{@code database} Tag의 Opt-in Test이며 {@code ./gradlew databaseTest}로만 실행합니다.</p>
 */
@Tag("database")
class WorkCaseServiceDatabaseIntegrationTest {

    private static final BigDecimal LATITUDE = new BigDecimal("37.1234567");
    private static final BigDecimal LONGITUDE = new BigDecimal("127.1234567");

    @Test
    @Timeout(60)
    void appliesWorkCaseWriteContractOnCurrentMysqlSchema() {
        try (AnnotationConfigApplicationContext context =
                     new AnnotationConfigApplicationContext(RootConfig.class)) {
            JdbcTemplate jdbc = new JdbcTemplate(context.getBean(DataSource.class));
            WorkCaseService service = context.getBean(WorkCaseService.class);

            String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
            String businessNumber = String.format(
                    "%010d", ThreadLocalRandom.current().nextLong(0, 1_000_000_0000L));

            Long ownerId = insertOwner(jdbc, "qa154w" + suffix);
            Long otherOwnerId = insertOwner(jdbc, "qa154x" + suffix);
            Long workplaceId = insertWorkplace(jdbc, ownerId, businessNumber);

            try {
                Long workCaseId = verifyCreateStoresRealAddressAndTimes(
                        service, jdbc, ownerId, workplaceId);
                verifyUpdateBumpsVersionAndRevokesPendingInvitation(
                        service, jdbc, ownerId, workCaseId);
                verifyOwnershipIsolation(service, otherOwnerId, workCaseId);

                Long deletableId = createDraft(service, ownerId, workplaceId);
                verifyDeleteHardDeletesWithoutInvitationHistory(service, jdbc, ownerId, deletableId);

                Long cancelableId = createDraft(service, ownerId, workplaceId);
                verifyDeleteCancelsWithInvitationHistory(service, jdbc, ownerId, cancelableId);
            } finally {
                cleanUp(jdbc, ownerId, otherOwnerId);
            }
        }
    }

    /** Service가 만든 combine 결과가 Mock이 아니라 실제 DB 값으로 저장·조회되는지 확인합니다. */
    private Long verifyCreateStoresRealAddressAndTimes(
            WorkCaseService service,
            JdbcTemplate jdbc,
            Long ownerId,
            Long workplaceId) {
        Long workCaseId = createDraft(service, ownerId, workplaceId);

        Map<String, Object> row = jdbc.queryForMap(
                "SELECT workplace_address, starts_at, ends_at, terms_version, status"
                        + " FROM work_cases WHERE id = ?",
                workCaseId);

        assertEquals("서울 강남구 테헤란로 1 2층", row.get("workplace_address"));
        // Connector/J가 DATETIME을 시간대 없는 LocalDateTime으로 그대로 돌려줍니다.
        assertEquals(LocalDateTime.of(2026, 8, 10, 9, 0), row.get("starts_at"));
        assertEquals(LocalDateTime.of(2026, 8, 10, 18, 0), row.get("ends_at"));
        assertEquals(1, ((Number) row.get("terms_version")).intValue());
        assertEquals("DRAFT", row.get("status"));
        return workCaseId;
    }

    /** update()가 잠금·조건 교체·PENDING 초대 철회를 한 Transaction으로 실제 반영하는지 확인합니다. */
    private void verifyUpdateBumpsVersionAndRevokesPendingInvitation(
            WorkCaseService service,
            JdbcTemplate jdbc,
            Long ownerId,
            Long workCaseId) {
        insertInvitation(jdbc, workCaseId, "PENDING");

        service.update(owner(ownerId), WorkCaseUpdateCommand.builder()
                .workCaseId(workCaseId)
                .title("주말 홀 서빙(수정)")
                .workDate(LocalDate.of(2026, 8, 10))
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(19, 0))
                .breakMinutes(30)
                .breakPaid(true)
                .dailyWage(130_000L)
                .build());

        Map<String, Object> row = jdbc.queryForMap(
                "SELECT title, terms_version FROM work_cases WHERE id = ?", workCaseId);
        assertEquals("주말 홀 서빙(수정)", row.get("title"));
        assertEquals(2, ((Number) row.get("terms_version")).intValue());

        assertEquals(
                0,
                countInvitations(jdbc, workCaseId, "PENDING"),
                "조건 변경 뒤에는 PENDING 초대가 남아 있으면 안 됩니다.");
        assertEquals(1, countInvitations(jdbc, workCaseId, "REVOKED"));

        // DRAFT가 아니면 같은 요청이 WORK_CASE_LOCKED로 거절되는지 같은 흐름에서 확인합니다.
        jdbc.update(
                "UPDATE work_cases SET status = 'CANCELED', canceled_at = CURRENT_TIMESTAMP(6)"
                        + " WHERE id = ?",
                workCaseId);
        assertThrows(
                WorkCaseLockedException.class,
                () -> service.update(owner(ownerId), WorkCaseUpdateCommand.builder()
                        .workCaseId(workCaseId)
                        .title("재수정 시도")
                        .workDate(LocalDate.of(2026, 8, 10))
                        .startTime(LocalTime.of(10, 0))
                        .endTime(LocalTime.of(19, 0))
                        .breakMinutes(30)
                        .breakPaid(true)
                        .dailyWage(130_000L)
                        .build()));
    }

    /** 다른 OWNER는 존재 여부를 알 수 없도록 404로만 응답받습니다. */
    private void verifyOwnershipIsolation(WorkCaseService service, Long otherOwnerId, Long workCaseId) {
        assertThrows(
                ResourceNotFoundException.class,
                () -> service.delete(owner(otherOwnerId), workCaseId));
    }

    private void verifyDeleteHardDeletesWithoutInvitationHistory(
            WorkCaseService service,
            JdbcTemplate jdbc,
            Long ownerId,
            Long workCaseId) {
        service.delete(owner(ownerId), workCaseId);

        Integer remaining = jdbc.queryForObject(
                "SELECT COUNT(*) FROM work_cases WHERE id = ?", Integer.class, workCaseId);
        assertEquals(0, remaining);
    }

    private void verifyDeleteCancelsWithInvitationHistory(
            WorkCaseService service,
            JdbcTemplate jdbc,
            Long ownerId,
            Long workCaseId) {
        insertInvitation(jdbc, workCaseId, "PENDING");

        service.delete(owner(ownerId), workCaseId);

        Map<String, Object> row = jdbc.queryForMap(
                "SELECT status, canceled_at FROM work_cases WHERE id = ?", workCaseId);
        assertEquals("CANCELED", row.get("status"));
        assertNotNull(row.get("canceled_at"));
        assertEquals(0, countInvitations(jdbc, workCaseId, "PENDING"));
    }

    private Long createDraft(WorkCaseService service, Long ownerId, Long workplaceId) {
        return service.create(owner(ownerId), WorkCaseCreateCommand.builder()
                .workplaceId(workplaceId)
                .title("주말 홀 서빙")
                .workDate(LocalDate.of(2026, 8, 10))
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(18, 0))
                .breakMinutes(60)
                .breakPaid(false)
                .dailyWage(120_000L)
                .build());
    }

    private AuthPrincipal owner(Long ownerId) {
        return new AuthPrincipal(ownerId, UserRole.OWNER, "김사장");
    }

    private int countInvitations(JdbcTemplate jdbc, Long workCaseId, String status) {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM work_invitations WHERE work_case_id = ? AND status = ?",
                Integer.class, workCaseId, status);
    }

    private void insertInvitation(JdbcTemplate jdbc, Long workCaseId, String status) {
        jdbc.update(
                "INSERT INTO work_invitations"
                        + " (work_case_id, token_hash, status, expected_terms_version, expires_at)"
                        + " VALUES (?, UNHEX(SHA2(CONCAT(?, RAND()), 256)), ?, 1,"
                        + " DATE_ADD(CURRENT_TIMESTAMP(6), INTERVAL 1 DAY))",
                workCaseId, workCaseId, status);
    }

    private Long insertOwner(JdbcTemplate jdbc, String loginId) {
        jdbc.update(
                "INSERT INTO users (login_id, email, password_hash, name, role)"
                        + " VALUES (?, ?, ?, ?, 'OWNER')",
                loginId,
                loginId + "@example.com",
                "$2a$10$0000000000000000000000000000000000000000000000000000",
                "김사장");
        return jdbc.queryForObject("SELECT id FROM users WHERE login_id = ?", Long.class, loginId);
    }

    private Long insertWorkplace(JdbcTemplate jdbc, Long ownerId, String businessNumber) {
        jdbc.update(
                "INSERT INTO workplaces (owner_user_id, business_registration_number, name,"
                        + " representative_name, road_address, detail_address, phone,"
                        + " latitude, longitude, radius_meters, status)"
                        + " VALUES (?, ?, '강남점', '김사장', '  서울 강남구 테헤란로 1  ', '  2층  ',"
                        + " '0212345678', ?, ?, 100.00, 'ACTIVE')",
                ownerId, businessNumber, LATITUDE, LONGITUDE);
        return jdbc.queryForObject(
                "SELECT id FROM workplaces WHERE business_registration_number = ?",
                Long.class, businessNumber);
    }

    private void cleanUp(JdbcTemplate jdbc, Long... ownerUserIds) {
        for (Long ownerUserId : ownerUserIds) {
            jdbc.update(
                    "DELETE FROM work_invitations WHERE work_case_id IN"
                            + " (SELECT id FROM work_cases WHERE employer_id = ?)",
                    ownerUserId);
            jdbc.update("DELETE FROM work_cases WHERE employer_id = ?", ownerUserId);
            jdbc.update("DELETE FROM workplaces WHERE owner_user_id = ?", ownerUserId);
            jdbc.update("DELETE FROM users WHERE id = ?", ownerUserId);
        }
    }
}
