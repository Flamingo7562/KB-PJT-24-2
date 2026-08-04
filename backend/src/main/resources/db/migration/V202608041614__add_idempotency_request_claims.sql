SET NAMES utf8mb4 COLLATE utf8mb4_0900_ai_ci;

-- 사용자와 Operation별 멱등 요청 Claim과 최초 성공 응답을 저장한다.
CREATE TABLE idempotency_requests (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id BIGINT UNSIGNED NOT NULL COMMENT '요청 인증 사용자',
    operation_code VARCHAR(64) CHARACTER SET ascii
        COLLATE ascii_bin NOT NULL COMMENT '멱등성 적용 Operation',
    idempotency_key VARCHAR(100) CHARACTER SET ascii
        COLLATE ascii_bin NOT NULL COMMENT '클라이언트 원본 Key',
    request_fingerprint BINARY(32) NOT NULL
        COMMENT '정규화 요청의 32바이트 Fingerprint',
    status VARCHAR(20) CHARACTER SET ascii
        COLLATE ascii_bin NOT NULL DEFAULT 'PROCESSING'
        COMMENT 'PROCESSING 또는 COMPLETED',
    response_http_status SMALLINT UNSIGNED NULL
        COMMENT '최초 성공 응답 HTTP 상태',
    response_body JSON NULL COMMENT '최초 성공 응답 JSON Snapshot',
    completed_at DATETIME(6) NULL COMMENT '성공 처리 완료 시각',
    expires_at DATETIME(6) NOT NULL COMMENT 'Claim 보존 만료 시각',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_idempotency_requests_scope (
        user_id,
        operation_code,
        idempotency_key
    ),
    CONSTRAINT fk_idempotency_requests_user
        FOREIGN KEY (user_id) REFERENCES users (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT ck_idempotency_requests_operation
        CHECK (CHAR_LENGTH(operation_code) > 0),
    CONSTRAINT ck_idempotency_requests_key
        CHECK (CHAR_LENGTH(idempotency_key) > 0),
    CONSTRAINT ck_idempotency_requests_status
        CHECK (status IN ('PROCESSING', 'COMPLETED')),
    CONSTRAINT ck_idempotency_requests_http_status
        CHECK (
            response_http_status IS NULL
            OR response_http_status BETWEEN 200 AND 299
        ),
    CONSTRAINT ck_idempotency_requests_lifecycle
        CHECK (
            (
                status = 'PROCESSING'
                AND response_http_status IS NULL
                AND response_body IS NULL
                AND completed_at IS NULL
            )
            OR
            (
                status = 'COMPLETED'
                AND response_http_status IS NOT NULL
                AND response_body IS NOT NULL
                AND completed_at IS NOT NULL
            )
        ),
    CONSTRAINT ck_idempotency_requests_expiry
        CHECK (
            expires_at > created_at
            AND (
                completed_at IS NULL
                OR (
                    completed_at >= created_at
                    AND completed_at <= expires_at
                )
            )
        )
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_0900_ai_ci
  COMMENT='멱등 요청 Claim과 성공 응답 저장';
