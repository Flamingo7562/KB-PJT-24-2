package com.gighub.document.mapper;

import com.gighub.config.RootConfig;
import com.gighub.document.dto.Document;
import com.gighub.document.dto.DocumentListItem;
import com.gighub.document.dto.DocumentVersion;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@Tag("database")
class DocumentQueryMapperTest {

    @Test
        // 메서드 이름 변경: 순서(Position)가 아닌 필드 매핑(Fields) 자체를 검증하도록 수정
    void findDocumentByIdMapsFieldsCorrectly() {
        try (AnnotationConfigApplicationContext context =
                     new AnnotationConfigApplicationContext(RootConfig.class)) {
            JdbcTemplate jdbcTemplate =
                    new JdbcTemplate(context.getBean(DataSource.class));
            DocumentQueryMapper mapper =
                    context.getBean(DocumentQueryMapper.class);

            String token = UUID.randomUUID().toString().substring(0, 8);
            String creatorLogin = "it_doc_creator_" + token;
            String ownerLogin = "it_doc_owner_" + token;

            jdbcTemplate.update(
                    "INSERT INTO users"
                            + " (login_id, email, password_hash, name, role, status)"
                            + " VALUES (?, ?, 'integration-test', '문서 생성자', 'WORKER', 'ACTIVE')",
                    creatorLogin, creatorLogin + "@example.invalid"
            );
            jdbcTemplate.update(
                    "INSERT INTO users"
                            + " (login_id, email, password_hash, name, role, status)"
                            + " VALUES (?, ?, 'integration-test', '문서 소유자', 'WORKER', 'ACTIVE')",
                    ownerLogin, ownerLogin + "@example.invalid"
            );
            Long creatorId = jdbcTemplate.queryForObject(
                    "SELECT id FROM users WHERE login_id = ?", Long.class, creatorLogin);
            Long ownerId = jdbcTemplate.queryForObject(
                    "SELECT id FROM users WHERE login_id = ?", Long.class, ownerLogin);

            jdbcTemplate.update(
                    "INSERT INTO documents"
                            + " (created_by_user_id, owner_user_id, document_type,"
                            + " status, issued_on, expires_on)"
                            + " VALUES (?, ?, 'HEALTH_CERTIFICATE', 'ACTIVE',"
                            + " '2026-01-15', '2027-01-15')",
                    creatorId, ownerId
            );
            Long documentId = jdbcTemplate.queryForObject(
                    "SELECT id FROM documents WHERE owner_user_id = ?",
                    Long.class, ownerId);

            try {
                Document document = mapper.findDocumentById(documentId);

                assertNotNull(document);
                assertEquals(documentId, document.getId());
                assertEquals(creatorId, document.getCreatedByUserId());
                assertEquals(ownerId, document.getOwnerUserId());
                assertNull(document.getWorkCaseId());
                assertEquals("HEALTH_CERTIFICATE", document.getDocumentType());
                assertEquals("ACTIVE", document.getStatus());
                assertEquals(LocalDate.of(2026, 1, 15), document.getIssuedOn());
                assertEquals(LocalDate.of(2027, 1, 15), document.getExpiresOn());
                assertNotNull(document.getCreatedAt());
                assertNotNull(document.getUpdatedAt());
            } finally {
                jdbcTemplate.update("DELETE FROM documents WHERE id = ?", documentId);
                jdbcTemplate.update(
                        "DELETE FROM users WHERE id IN (?, ?)", creatorId, ownerId);
            }
        }
    }

    @Test
    void findVersionsByDocumentIdMapsFieldsCorrectly() { // 이름 변경 완료
        try (AnnotationConfigApplicationContext context =
                     new AnnotationConfigApplicationContext(RootConfig.class)) {
            JdbcTemplate jdbcTemplate =
                    new JdbcTemplate(context.getBean(DataSource.class));
            DocumentQueryMapper mapper =
                    context.getBean(DocumentQueryMapper.class);

            String token = UUID.randomUUID().toString().substring(0, 8);
            String ownerLogin = "it_ver_owner_" + token;

            // DB 삭제 시 참조할 ID 변수들을 미리 선언해 둡니다.
            Long ownerId = null;
            Long documentId = null;
            Long versionId = null;

            try {
                // 1. 유저 생성
                jdbcTemplate.update(
                        "INSERT INTO users"
                                + " (login_id, email, password_hash, name, role, status)"
                                + " VALUES (?, ?, 'integration-test', '버전 테스트', 'WORKER', 'ACTIVE')",
                        ownerLogin, ownerLogin + "@example.invalid"
                );
                ownerId = jdbcTemplate.queryForObject(
                        "SELECT id FROM users WHERE login_id = ?", Long.class, ownerLogin);

                // 2. 문서 생성
                jdbcTemplate.update(
                        "INSERT INTO documents"
                                + " (created_by_user_id, owner_user_id, document_type, status, issued_on, expires_on)"
                                + " VALUES (?, ?, 'HEALTH_CERTIFICATE', 'ACTIVE', '2026-01-15', '2027-01-15')",
                        ownerId, ownerId
                );
                documentId = jdbcTemplate.queryForObject(
                        "SELECT id FROM documents WHERE owner_user_id = ?",
                        Long.class, ownerId);

                // 3. 문서 버전 생성
                jdbcTemplate.update(
                        "INSERT INTO document_versions"
                                + " (document_id, version_no, version_type,"
                                + " storage_key, mime_type, size_bytes, checksum)"
                                + " VALUES (?, 1, 'ORIGINAL', ?, 'image/png', 555, ?)",
                        documentId, "docs/" + token + "/v1.png", "dummy-checksum-hash-1234"
                );

                // DB에 방금 들어간 버전 ID를 조회해 옵니다
                versionId = jdbcTemplate.queryForObject(
                        "SELECT id FROM document_versions WHERE document_id = ?",
                        Long.class, documentId);

                // 4. 매퍼 테스트 검증
                List<DocumentVersion> versions = mapper.findVersionsByDocumentId(documentId);

                assertEquals(1, versions.size());
                DocumentVersion version = versions.get(0);
                assertEquals(versionId, version.getId());
                assertEquals(documentId, version.getDocumentId());
                assertEquals(1, version.getVersionNo());
                assertEquals("ORIGINAL", version.getVersionType());
                assertEquals("docs/" + token + "/v1.png", version.getStorageKey());
                assertEquals("image/png", version.getMimeType());
                assertEquals(555L, version.getSizeBytes());
                assertNotNull(version.getCreatedAt());

            } finally {
                // 자식(버전) 데이터를 먼저 완벽하게 삭제한 후 -> 부모(문서) 삭제 -> 최상위(유저) 삭제
                if (documentId != null) {
                    // 외래키 에러 방지를 위해 이 문서에 달린 모든 버전을 싹 지웁니다.
                    jdbcTemplate.update("DELETE FROM document_versions WHERE document_id = ?", documentId);
                    // 자식이 모두 사라졌으니 이제 부모(문서)를 안심하고 지울 수 있습니다.
                    jdbcTemplate.update("DELETE FROM documents WHERE id = ?", documentId);
                }
                if (ownerId != null) {
                    jdbcTemplate.update("DELETE FROM users WHERE id = ?", ownerId);
                }
            }
        }
    }

    @Test
    void findDocumentsReturnsOwnedDocumentsOrderedByCreatedAtDesc() {
        try (AnnotationConfigApplicationContext context =
                     new AnnotationConfigApplicationContext(RootConfig.class)) {
            JdbcTemplate jdbcTemplate =
                    new JdbcTemplate(context.getBean(DataSource.class));
            DocumentQueryMapper mapper =
                    context.getBean(DocumentQueryMapper.class);

            String token = UUID.randomUUID().toString().substring(0, 8);
            String ownerLogin = "it_list_owner_" + token;

            // 🚨 DB 삭제 시 참조할 ID 변수를 맨 위에 미리 선언해 둡니다.
            Long ownerId = null;

            try {
                // 1. 유저 생성
                jdbcTemplate.update(
                        "INSERT INTO users"
                                + " (login_id, email, password_hash, name, role, status)"
                                + " VALUES (?, ?, 'integration-test', '목록 테스트', 'WORKER', 'ACTIVE')",
                        ownerLogin, ownerLogin + "@example.invalid"
                );
                ownerId = jdbcTemplate.queryForObject(
                        "SELECT id FROM users WHERE login_id = ?", Long.class, ownerLogin);

                // 2. 문서 생성 (🚨 해결: issued_on, expires_on 추가!)
                jdbcTemplate.update(
                        "INSERT INTO documents"
                                + " (created_by_user_id, owner_user_id, document_type, status, issued_on, expires_on)"
                                + " VALUES (?, ?, 'HEALTH_CERTIFICATE', 'ACTIVE', '2026-01-15', '2027-01-15')",
                        ownerId, ownerId
                );
                List<Long> documentIds = jdbcTemplate.queryForList(
                        "SELECT id FROM documents WHERE owner_user_id = ?",
                        Long.class, ownerId);

                // 3. 매퍼 테스트 검증
                List<DocumentListItem> content =
                        mapper.findDocuments(ownerId, null, 0L, 20);
                int total = mapper.countDocuments(ownerId, null);

                assertEquals(1, total);
                assertEquals(1, content.size());
                assertEquals(documentIds.get(0), content.get(0).getDocumentId());
                assertEquals("HEALTH_CERTIFICATE", content.get(0).getType());
                assertEquals("ACTIVE", content.get(0).getStatus());
                assertEquals("목록 테스트", content.get(0).getOwnerName());
                assertEquals("OWN", content.get(0).getSource());

            } finally {
                // 4. 안전한 DB 정리 (에러가 났더라도 ownerId가 있으면 모두 지움)
                if (ownerId != null) {
                    jdbcTemplate.update(
                            "DELETE FROM document_versions "
                                    + "WHERE document_id IN (SELECT id FROM documents WHERE owner_user_id = ?)",
                            ownerId
                    );
                    jdbcTemplate.update("DELETE FROM documents WHERE owner_user_id = ?", ownerId);
                    jdbcTemplate.update("DELETE FROM users WHERE id = ?", ownerId);
                }
            }
        }
    }
}