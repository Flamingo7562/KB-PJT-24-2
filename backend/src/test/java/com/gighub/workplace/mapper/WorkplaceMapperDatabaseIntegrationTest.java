package com.gighub.workplace.mapper;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

import com.gighub.config.RootConfig;
import com.gighub.workplace.mapper.param.WorkplaceInsertParam;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 실제 MySQL Head Schema에서 사업장 INSERT 계약을 검증합니다.
 *
 * <p>Unique·CHECK 제약과 계약값 저장은 Java 단위 검증으로 대신할 수 없어 {@code database}
 * Tag의 Opt-in Test로 둡니다.</p>
 */
@Tag("database")
class WorkplaceMapperDatabaseIntegrationTest {

    private String businessNumberPrefix;

    @Test
    @Timeout(30)
    void storesContractValuesAndBlocksDuplicateBusinessNumberOnCurrentMysqlSchema() throws Exception {
        try (AnnotationConfigApplicationContext context =
                     new AnnotationConfigApplicationContext(RootConfig.class)) {
            JdbcTemplate jdbcTemplate = new JdbcTemplate(context.getBean(DataSource.class));
            WorkplaceMapper workplaceMapper = context.getBean(WorkplaceMapper.class);

            String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
            // 사업자등록번호는 Unique 숫자 10자리이므로 앞 6자리를 실행마다 다르게 만듭니다.
            // 고정값을 쓰면 이전 실행이 정리 전에 중단됐을 때 다음 실행이 엉뚱한 중복으로 실패합니다.
            businessNumberPrefix = String.format(
                    "%06d", ThreadLocalRandom.current().nextInt(1_000_000));
            Long ownerUserId = insertOwnerFixture(jdbcTemplate, suffix);

            try {
                verifyServerControlledColumns(jdbcTemplate, workplaceMapper, ownerUserId);
                verifyOptionalColumnsStayNull(jdbcTemplate, workplaceMapper, ownerUserId);
                verifyPartialCoordinateIsRejected(workplaceMapper, ownerUserId);
                verifyDuplicateBusinessNumberIsRejected(workplaceMapper, ownerUserId);
                verifyConcurrentDuplicateLeavesSingleRow(jdbcTemplate, workplaceMapper, ownerUserId);
            } finally {
                jdbcTemplate.update("DELETE FROM workplaces WHERE owner_user_id = ?", ownerUserId);
                jdbcTemplate.update("DELETE FROM users WHERE id = ?", ownerUserId);
            }
        }
    }

    /** 반경과 최초 상태는 호출자가 정할 수 없고 항상 계약값으로 저장돼야 합니다. */
    private void verifyServerControlledColumns(
            JdbcTemplate jdbcTemplate,
            WorkplaceMapper workplaceMapper,
            Long ownerUserId) {
        WorkplaceInsertParam param = paramBuilder(ownerUserId, businessNumber(1))
                .latitude(new BigDecimal("37.1234567"))
                .longitude(new BigDecimal("127.1234567"))
                .build();

        assertEquals(1, workplaceMapper.insert(param));
        assertNotNull(param.getId(), "생성 Key가 파라미터로 되돌아와야 합니다.");

        Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT * FROM workplaces WHERE id = ?", param.getId());

        assertEquals(ownerUserId, ((Number) row.get("owner_user_id")).longValue());
        assertEquals(businessNumber(1), row.get("business_registration_number"));
        assertEquals("강남점", row.get("name"));
        assertEquals("김사장", row.get("representative_name"));
        assertEquals("서울 강남구 테헤란로 1", row.get("road_address"));
        assertEquals("2층", row.get("detail_address"));
        assertEquals("0212345678", row.get("phone"));
        assertEquals(0, new BigDecimal("100.00").compareTo((BigDecimal) row.get("radius_meters")));
        assertEquals("ACTIVE", row.get("status"));
        assertNull(row.get("deleted_at"));
        assertEquals(0, new BigDecimal("37.1234567").compareTo((BigDecimal) row.get("latitude")));
        assertEquals(0, new BigDecimal("127.1234567").compareTo((BigDecimal) row.get("longitude")));
    }

    private void verifyOptionalColumnsStayNull(
            JdbcTemplate jdbcTemplate,
            WorkplaceMapper workplaceMapper,
            Long ownerUserId) {
        WorkplaceInsertParam param = paramBuilder(ownerUserId, businessNumber(2))
                .detailAddress(null)
                .build();

        assertEquals(1, workplaceMapper.insert(param));

        Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT detail_address, latitude, longitude FROM workplaces WHERE id = ?",
                param.getId());

        assertNull(row.get("detail_address"));
        assertNull(row.get("latitude"));
        assertNull(row.get("longitude"));
    }

    /**
     * 애플리케이션 검증을 우회해도 좌표 한쪽만 저장되지 않아야 합니다.
     *
     * <p>MySQL의 CHECK 위반(3819)은 Spring 기본 Error Code Mapping에 없어
     * {@code UncategorizedSQLException}으로 올라옵니다. 즉 이 제약에 도달하면 승인된 400이
     * 아니라 500이 되므로 좌표 쌍의 실제 방어선은 요청 DTO 검증입니다.</p>
     */
    private void verifyPartialCoordinateIsRejected(
            WorkplaceMapper workplaceMapper,
            Long ownerUserId) {
        WorkplaceInsertParam param = paramBuilder(ownerUserId, businessNumber(3))
                .latitude(new BigDecimal("37.1234567"))
                .build();

        assertThrows(DataAccessException.class, () -> workplaceMapper.insert(param));
    }

    private void verifyDuplicateBusinessNumberIsRejected(
            WorkplaceMapper workplaceMapper,
            Long ownerUserId) {
        assertEquals(1, workplaceMapper.insert(paramBuilder(ownerUserId, businessNumber(4)).build()));

        WorkplaceInsertParam duplicate = paramBuilder(ownerUserId, businessNumber(4)).build();
        assertThrows(DuplicateKeyException.class, () -> workplaceMapper.insert(duplicate));
    }

    /**
     * 사전 조회로는 막을 수 없는 경로입니다. 두 요청이 동시에 "없음"을 확인해도 Unique
     * 제약이 한 건만 남기는지 확인합니다.
     */
    private void verifyConcurrentDuplicateLeavesSingleRow(
            JdbcTemplate jdbcTemplate,
            WorkplaceMapper workplaceMapper,
            Long ownerUserId) throws Exception {
        String businessNumber = businessNumber(5);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            List<Future<Boolean>> results = List.of(
                    executor.submit(() -> attemptInsert(workplaceMapper, ownerUserId, businessNumber, start)),
                    executor.submit(() -> attemptInsert(workplaceMapper, ownerUserId, businessNumber, start))
            );

            start.countDown();

            int inserted = 0;
            for (Future<Boolean> result : results) {
                if (Boolean.TRUE.equals(result.get(20, TimeUnit.SECONDS))) {
                    inserted++;
                }
            }

            assertEquals(1, inserted, "동시 등록에서 한 건만 성공해야 합니다.");
            assertEquals(
                    1,
                    jdbcTemplate.queryForObject(
                            "SELECT COUNT(*) FROM workplaces WHERE business_registration_number = ?",
                            Integer.class,
                            businessNumber));
        } finally {
            executor.shutdownNow();
        }
    }

    private boolean attemptInsert(
            WorkplaceMapper workplaceMapper,
            Long ownerUserId,
            String businessNumber,
            CountDownLatch start) throws InterruptedException {
        start.await();
        try {
            workplaceMapper.insert(paramBuilder(ownerUserId, businessNumber).build());
            return true;
        } catch (DuplicateKeyException expected) {
            return false;
        }
    }

    private String businessNumber(int index) {
        return businessNumberPrefix + String.format("%04d", index);
    }

    private WorkplaceInsertParam.WorkplaceInsertParamBuilder paramBuilder(
            Long ownerUserId,
            String businessRegistrationNumber) {
        return WorkplaceInsertParam.builder()
                .ownerUserId(ownerUserId)
                .businessRegistrationNumber(businessRegistrationNumber)
                .name("강남점")
                .representativeName("김사장")
                .roadAddress("서울 강남구 테헤란로 1")
                .detailAddress("2층")
                .phone("0212345678");
    }

    private Long insertOwnerFixture(JdbcTemplate jdbcTemplate, String suffix) {
        jdbcTemplate.update(
                "INSERT INTO users (login_id, email, password_hash, name, role) "
                        + "VALUES (?, ?, ?, ?, 'OWNER')",
                "qa145" + suffix,
                "qa145" + suffix + "@example.com",
                "$2a$10$0000000000000000000000000000000000000000000000000000",
                "김사장");

        return jdbcTemplate.queryForObject(
                "SELECT id FROM users WHERE login_id = ?", Long.class, "qa145" + suffix);
    }
}
