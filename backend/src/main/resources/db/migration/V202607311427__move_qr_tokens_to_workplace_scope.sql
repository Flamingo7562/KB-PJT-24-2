SET NAMES utf8mb4 COLLATE utf8mb4_0900_ai_ci;

-- 발급자와 사업장 소유자가 다른 과거 행은 이력을 임의로 고치지 않고 변경 전에 중단한다.
CREATE TEMPORARY TABLE gighub_qr_owner_migration_guard (
    invalid_row_count BIGINT UNSIGNED NOT NULL,
    CONSTRAINT ck_gighub_qr_owner_migration_guard
        CHECK (invalid_row_count = 0)
);

INSERT INTO gighub_qr_owner_migration_guard (invalid_row_count)
SELECT COUNT(*)
FROM qr_tokens qr
INNER JOIN work_cases work_case
    ON work_case.id = qr.work_case_id
INNER JOIN workplaces workplace
    ON workplace.id = work_case.workplace_id
WHERE qr.issued_by_user_id <> workplace.owner_user_id;

DROP TEMPORARY TABLE gighub_qr_owner_migration_guard;

-- 기존 근무별 QR의 사업장은 work_cases를 통해 손실 없이 결정할 수 있다.
ALTER TABLE qr_tokens
    ADD COLUMN workplace_id BIGINT UNSIGNED NULL AFTER id,
    ADD COLUMN token_nonce BINARY(16) NULL AFTER token_hash,
    ADD COLUMN revoked_at DATETIME(6) NULL AFTER used_at;

UPDATE qr_tokens qr
INNER JOIN work_cases work_case
    ON work_case.id = qr.work_case_id
SET qr.workplace_id = work_case.workplace_id;

SET @GIGHUB_QR_SCOPE_MIGRATED_AT = CURRENT_TIMESTAMP(6);

-- 근무·동작별 기존 Token은 새 고정 QR로 재사용하지 않고 모두 비활성화한다.
UPDATE qr_tokens
SET status = 'REVOKED',
    revoked_at = @GIGHUB_QR_SCOPE_MIGRATED_AT
WHERE status = 'ACTIVE';

UPDATE qr_tokens
SET revoked_at = @GIGHUB_QR_SCOPE_MIGRATED_AT
WHERE status = 'REVOKED'
  AND revoked_at IS NULL;

ALTER TABLE qr_tokens
    DROP FOREIGN KEY fk_qr_tokens_work_case,
    DROP FOREIGN KEY fk_qr_tokens_issuer,
    DROP CHECK ck_qr_tokens_action,
    DROP CHECK ck_qr_tokens_status,
    DROP INDEX idx_qr_tokens_case_action_status_expiry,
    CHANGE COLUMN work_case_id legacy_work_case_id
        BIGINT UNSIGNED NULL,
    CHANGE COLUMN token_hash legacy_token_hash
        BINARY(32) NULL,
    CHANGE COLUMN action legacy_action
        VARCHAR(20) NULL,
    CHANGE COLUMN expires_at legacy_expires_at
        DATETIME(6) NULL,
    CHANGE COLUMN used_at legacy_used_at
        DATETIME(6) NULL,
    MODIFY COLUMN workplace_id BIGINT UNSIGNED NOT NULL,
    ADD COLUMN active_slot TINYINT GENERATED ALWAYS AS (
        CASE
            WHEN token_nonce IS NOT NULL AND status = 'ACTIVE' THEN 1
            ELSE NULL
        END
    ) STORED AFTER revoked_at,
    RENAME INDEX uk_qr_tokens_token_hash
        TO uk_qr_tokens_legacy_token_hash,
    ADD UNIQUE KEY uk_qr_tokens_token_nonce (token_nonce),
    ADD UNIQUE KEY uk_qr_tokens_workplace_active
        (workplace_id, active_slot),
    ADD KEY idx_qr_tokens_workplace_status_created
        (workplace_id, status, created_at),
    ADD KEY idx_qr_tokens_legacy_case_action_created
        (legacy_work_case_id, legacy_action, created_at),
    ADD CONSTRAINT fk_qr_tokens_legacy_work_case
        FOREIGN KEY (legacy_work_case_id) REFERENCES work_cases (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    ADD CONSTRAINT fk_qr_tokens_workplace_issuer
        FOREIGN KEY (issued_by_user_id, workplace_id)
        REFERENCES workplaces (owner_user_id, id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    ADD CONSTRAINT ck_qr_tokens_legacy_action
        CHECK (
            legacy_action IS NULL
            OR legacy_action IN ('CHECK_IN', 'CHECK_OUT')
        ),
    ADD CONSTRAINT ck_qr_tokens_status
        CHECK (status IN ('ACTIVE', 'USED', 'EXPIRED', 'REVOKED')),
    ADD CONSTRAINT ck_qr_tokens_revoked_at
        CHECK (
            (status = 'REVOKED' AND revoked_at IS NOT NULL)
            OR (status <> 'REVOKED' AND revoked_at IS NULL)
        ),
    ADD CONSTRAINT ck_qr_tokens_shape
        CHECK (
            (
                token_nonce IS NOT NULL
                AND legacy_token_hash IS NULL
                AND legacy_work_case_id IS NULL
                AND legacy_action IS NULL
                AND legacy_expires_at IS NULL
                AND legacy_used_at IS NULL
                AND status IN ('ACTIVE', 'REVOKED')
            )
            OR
            (
                token_nonce IS NULL
                AND legacy_token_hash IS NOT NULL
                AND legacy_work_case_id IS NOT NULL
                AND legacy_action IS NOT NULL
                AND legacy_expires_at IS NOT NULL
                AND status IN ('USED', 'EXPIRED', 'REVOKED')
            )
        );

-- token_nonce는 공개 식별자이며 QR 서명 비밀이 아니다.
-- 애플리케이션은 외부 설정 HMAC Key로 nonce와 workplace_id를 서명해
-- 새로고침 후에도 같은 고정 QR Token을 재생성해야 한다.
INSERT INTO qr_tokens (
    workplace_id,
    issued_by_user_id,
    token_nonce,
    status,
    created_at
)
SELECT
    id,
    owner_user_id,
    RANDOM_BYTES(16),
    'ACTIVE',
    @GIGHUB_QR_SCOPE_MIGRATED_AT
FROM workplaces
WHERE status = 'ACTIVE';

ALTER TABLE attendance_records
    ADD COLUMN early_checkout_confirmed_at DATETIME(6) NULL
        AFTER failure_reason,
    ADD CONSTRAINT ck_attendance_records_early_checkout_confirmation
        CHECK (
            early_checkout_confirmed_at IS NULL
            OR (
                attendance_type = 'CHECK_OUT'
                AND result = 'SUCCESS'
            )
        );

-- QR 스캔 시 로그인 근로자의 해당 사업장 처리 대상 근무를 찾는 조회 경로다.
ALTER TABLE work_cases
    ADD KEY idx_work_cases_worker_workplace_status_start_end (
        worker_id,
        workplace_id,
        status,
        starts_at,
        ends_at
    );
