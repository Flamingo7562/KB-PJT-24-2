SET NAMES utf8mb4 COLLATE utf8mb4_0900_ai_ci;

-- 비밀번호 재설정 Token 전달 방식은 애플리케이션에서 결정하며 DB에는 SHA-256 Hash만 보관한다.
-- 같은 사용자에게 활성 Token을 하나만 허용해 새 요청이 이전 요청을 반드시 폐기하게 한다.
CREATE TABLE password_reset_tokens (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id BIGINT UNSIGNED NOT NULL,
    token_hash BINARY(32) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    expires_at DATETIME(6) NOT NULL,
    used_at DATETIME(6) NULL,
    revoked_at DATETIME(6) NULL,
    active_slot TINYINT GENERATED ALWAYS AS (
        CASE WHEN status = 'ACTIVE' THEN 1 ELSE NULL END
    ) STORED,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_password_reset_tokens_token_hash (token_hash),
    UNIQUE KEY uk_password_reset_tokens_user_active (user_id, active_slot),
    KEY idx_password_reset_tokens_status_expiry (status, expires_at),
    CONSTRAINT fk_password_reset_tokens_user
        FOREIGN KEY (user_id) REFERENCES users (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT ck_password_reset_tokens_status
        CHECK (status IN ('ACTIVE', 'USED', 'EXPIRED', 'REVOKED')),
    CONSTRAINT ck_password_reset_tokens_lifecycle
        CHECK (
            (status = 'ACTIVE' AND used_at IS NULL AND revoked_at IS NULL)
            OR (status = 'USED' AND used_at IS NOT NULL AND revoked_at IS NULL)
            OR (status = 'EXPIRED' AND used_at IS NULL AND revoked_at IS NULL)
            OR (status = 'REVOKED' AND used_at IS NULL AND revoked_at IS NOT NULL)
        )
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_0900_ai_ci;
