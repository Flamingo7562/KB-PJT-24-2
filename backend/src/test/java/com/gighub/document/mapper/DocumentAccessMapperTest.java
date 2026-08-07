package com.gighub.document.mapper;

import com.gighub.config.RootConfig;
import com.gighub.document.dto.DocumentFileVersion;
import com.gighub.document.mapper.param.DocumentAccessLogParam;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.util.Arrays;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** {@link DocumentAccessMapper}의 SIGNED 전용 선택과 계약 당사자 경계를 MySQL로 검증합니다. */
@Tag("database")
class DocumentAccessMapperTest {

    @Test
    void returnsOnlySignedVersionTwoWithItsChecksum() {
        try (AnnotationConfigApplicationContext context =
                     new AnnotationConfigApplicationContext(RootConfig.class)) {
            JdbcTemplate jdbcTemplate = new JdbcTemplate(context.getBean(DataSource.class));
            DocumentAccessMapper mapper = context.getBean(DocumentAccessMapper.class);
            String token = UUID.randomUUID().toString().replace("-", "").substring(0, 10);

            Long ownerId = insertUser(jdbcTemplate, "it_da_owner_" + token);
            Long documentId = insertDocument(jdbcTemplate, ownerId);
            insertVersion(jdbcTemplate, documentId, 1, "ORIGINAL", token + "-original");
            Long signedVersionId =
                    insertVersion(jdbcTemplate, documentId, 2, "SIGNED", token + "-signed");

            try {
                DocumentFileVersion latest = mapper.findSignedVersionForAccess(documentId);

                assertEquals(signedVersionId, latest.getId());
                assertEquals("SIGNED", latest.getVersionType());
                assertEquals(2, latest.getVersionNo());
                byte[] expectedChecksum = new byte[32];
                Arrays.fill(expectedChecksum, (byte) 7);
                assertArrayEquals(expectedChecksum, latest.getChecksum());

                jdbcTemplate.update(
                        "DELETE FROM document_versions WHERE id = ?", signedVersionId);
                assertNull(mapper.findSignedVersionForAccess(documentId));
            } finally {
                deleteFixture(jdbcTemplate, documentId, ownerId);
            }
        }
    }

    @Test
    void allowsOnlyTheOwnerAndWorkerStoredInTheContractSnapshot() {
        try (AnnotationConfigApplicationContext context =
                     new AnnotationConfigApplicationContext(RootConfig.class)) {
            JdbcTemplate jdbcTemplate = new JdbcTemplate(context.getBean(DataSource.class));
            DocumentAccessMapper mapper = context.getBean(DocumentAccessMapper.class);
            String token = UUID.randomUUID().toString().replace("-", "").substring(0, 10);

            Long ownerId = insertUser(jdbcTemplate, "it_da_shr_owner_" + token);
            Long workerId = insertUser(jdbcTemplate, "it_da_party_worker_" + token);
            Long strangerId = insertUser(jdbcTemplate, "it_da_shr_stranger_" + token);
            Long workplaceId = insertWorkplace(jdbcTemplate, ownerId, token);
            Long workCaseId = insertWorkCase(jdbcTemplate, ownerId, workerId, workplaceId);
            insertWorkContract(jdbcTemplate, workCaseId, ownerId, workerId);
            Long documentId = insertDocument(jdbcTemplate, ownerId, workCaseId);

            try {
                assertTrue(mapper.isContractParty(documentId, ownerId));
                assertTrue(mapper.isContractParty(documentId, workerId));
                assertFalse(mapper.isContractParty(documentId, strangerId));
            } finally {
                jdbcTemplate.update("DELETE FROM documents WHERE id = ?", documentId);
                jdbcTemplate.update(
                        "DELETE FROM work_contracts WHERE work_case_id = ?", workCaseId);
                jdbcTemplate.update("DELETE FROM work_cases WHERE id = ?", workCaseId);
                jdbcTemplate.update("DELETE FROM workplaces WHERE id = ?", workplaceId);
                jdbcTemplate.update(
                        "DELETE FROM users WHERE id IN (?, ?, ?)",
                        ownerId, workerId, strangerId);
            }
        }
    }

    @Test
    void insertsAllowedAndDeniedAuditRows() {
        try (AnnotationConfigApplicationContext context =
                     new AnnotationConfigApplicationContext(RootConfig.class)) {
            JdbcTemplate jdbcTemplate = new JdbcTemplate(context.getBean(DataSource.class));
            DocumentAccessMapper mapper = context.getBean(DocumentAccessMapper.class);
            String token = UUID.randomUUID().toString().replace("-", "").substring(0, 10);

            Long ownerId = insertUser(jdbcTemplate, "it_da_audit_" + token);
            Long documentId = insertDocument(jdbcTemplate, ownerId);
            Long versionId =
                    insertVersion(jdbcTemplate, documentId, 1, "ORIGINAL", token + "-audit");

            try {
                mapper.insertAccessLog(DocumentAccessLogParam.builder()
                        .documentId(documentId)
                        .documentVersionId(versionId)
                        .actorUserId(ownerId)
                        .action("CONTRACT_FILE_VIEW")
                        .result("ALLOWED")
                        .denialReason(null)
                        .build());
                mapper.insertAccessLog(DocumentAccessLogParam.builder()
                        .documentId(documentId)
                        .documentVersionId(versionId)
                        .actorUserId(ownerId)
                        .action("CONTRACT_FILE_DOWNLOAD")
                        .result("DENIED")
                        .denialReason("PARTY_ACCESS_DENIED")
                        .build());

                Integer count = jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM document_access_logs WHERE document_id = ?",
                        Integer.class, documentId);
                assertEquals(2, count);
            } finally {
                jdbcTemplate.update(
                        "DELETE FROM document_access_logs WHERE document_id = ?", documentId);
                deleteFixture(jdbcTemplate, documentId, ownerId);
            }
        }
    }

    private Long insertUser(JdbcTemplate jdbcTemplate, String loginId) {
        jdbcTemplate.update(
                "INSERT INTO users (login_id, email, password_hash, name, role, status)"
                        + " VALUES (?, ?, 'test-hash', '문서 접근 테스트', 'OWNER', 'ACTIVE')",
                loginId, loginId + "@example.test"
        );
        return jdbcTemplate.queryForObject(
                "SELECT id FROM users WHERE login_id = ?", Long.class, loginId);
    }

    private Long insertDocument(JdbcTemplate jdbcTemplate, Long ownerId) {
        return insertDocument(jdbcTemplate, ownerId, null);
    }

    private Long insertDocument(
            JdbcTemplate jdbcTemplate, Long ownerId, Long workCaseId) {
        jdbcTemplate.update(
                "INSERT INTO documents"
                        + " (created_by_user_id, owner_user_id, work_case_id, document_type, status)"
                        + " VALUES (?, ?, ?, 'EMPLOYMENT_CONTRACT', 'ACTIVE')",
                ownerId, ownerId, workCaseId
        );
        return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    private Long insertVersion(
            JdbcTemplate jdbcTemplate, Long documentId, int versionNo,
            String versionType, String storageToken) {
        byte[] checksum = new byte[32];
        Arrays.fill(checksum, (byte) 7);
        jdbcTemplate.update(
                "INSERT INTO document_versions"
                        + " (document_id, version_no, version_type, storage_key,"
                        + " mime_type, size_bytes, checksum)"
                        + " VALUES (?, ?, ?, ?, 'application/pdf', 1, ?)",
                documentId, versionNo, versionType,
                "tests/document-access/" + storageToken + ".pdf", checksum
        );
        return jdbcTemplate.queryForObject(
                "SELECT id FROM document_versions WHERE document_id = ? AND version_no = ?",
                Long.class, documentId, versionNo);
    }

    private Long insertWorkplace(JdbcTemplate jdbcTemplate, Long ownerUserId, String token) {
        String registrationNumber = String.format(
                "%010d", Integer.toUnsignedLong(token.hashCode()));
        jdbcTemplate.update(
                "INSERT INTO workplaces"
                        + " (owner_user_id, business_registration_number, name,"
                        + " representative_name, road_address, phone, status)"
                        + " VALUES (?, ?, '문서 접근 테스트 사업장', '통합 대표',"
                        + " '서울 테스트로 1', '0212345678', 'ACTIVE')",
                ownerUserId, registrationNumber
        );
        return jdbcTemplate.queryForObject(
                "SELECT id FROM workplaces WHERE business_registration_number = ?",
                Long.class, registrationNumber);
    }

    private Long insertWorkCase(
            JdbcTemplate jdbcTemplate, Long employerId, Long workerId, Long workplaceId) {
        jdbcTemplate.update(
                "INSERT INTO work_cases"
                        + " (employer_id, worker_id, workplace_id, title, starts_at, ends_at,"
                        + " break_minutes, break_paid, workplace_name, workplace_address,"
                        + " allowed_radius_meters, agreed_wage, terms_version, status)"
                        + " VALUES (?, ?, ?, '문서 접근 테스트 근무', '2026-07-22 10:00:00',"
                        + " '2026-07-22 18:00:00', 0, 0, '테스트 사업장', '서울 테스트로 1',"
                        + " 100, 90000, 1, 'ACCEPTED')",
                employerId, workerId, workplaceId
        );
        return jdbcTemplate.queryForObject(
                "SELECT id FROM work_cases WHERE employer_id = ? AND worker_id = ?"
                        + " AND title = '문서 접근 테스트 근무'",
                Long.class, employerId, workerId);
    }

    private void insertWorkContract(
            JdbcTemplate jdbcTemplate, Long workCaseId, Long employerId, Long workerId) {
        jdbcTemplate.update(
                "INSERT INTO work_contracts"
                        + " (work_case_id, employer_id, worker_id, title, starts_at, ends_at,"
                        + " break_minutes, break_paid, workplace_name, workplace_address,"
                        + " allowed_radius_meters, agreed_wage, source_terms_version,"
                        + " terms_snapshot, accepted_at)"
                        + " VALUES (?, ?, ?, '문서 접근 테스트 근무',"
                        + " '2026-07-22 10:00:00', '2026-07-22 18:00:00', 0, 0,"
                        + " '테스트 사업장', '서울 테스트로 1', 100, 90000, 1, '{}',"
                        + " '2026-07-21 10:00:00')",
                workCaseId, employerId, workerId
        );
    }

    private void deleteFixture(JdbcTemplate jdbcTemplate, Long documentId, Long ownerId) {
        jdbcTemplate.update(
                "DELETE FROM document_versions WHERE document_id = ?", documentId);
        jdbcTemplate.update("DELETE FROM documents WHERE id = ?", documentId);
        jdbcTemplate.update("DELETE FROM users WHERE id = ?", ownerId);
    }
}
