-- 계약 파일 접근 감사가 실제 문서 Version과 구조화된 거부 사유를 가리키도록 보강한다.
-- 기존 감사 행은 당시 Version과 거부 사유를 안전하게 복원할 수 없으므로 NULL을 허용한다.
ALTER TABLE document_access_logs
    ADD COLUMN document_version_id BIGINT UNSIGNED NULL
        COMMENT '접근 대상으로 확정된 문서 Version; 기존 행 또는 Version 확정 전 거부는 NULL'
        AFTER document_id,
    ADD COLUMN denial_reason VARCHAR(50) CHARACTER SET ascii
        COLLATE ascii_bin NULL
        COMMENT 'DENIED 결과의 구조화된 내부 사유 Code; 기존 행은 NULL'
        AFTER result,
    ADD KEY idx_document_access_logs_document_version_created
        (document_id, document_version_id, created_at),
    ADD CONSTRAINT fk_document_access_logs_document_version
        FOREIGN KEY (document_id, document_version_id)
            REFERENCES document_versions (document_id, id)
            ON DELETE RESTRICT ON UPDATE RESTRICT,
    ADD CONSTRAINT ck_document_access_logs_denial_reason
        CHECK (
            denial_reason IS NULL
            OR (
                result = 'DENIED'
                AND CHAR_LENGTH(denial_reason) > 0
            )
        );
