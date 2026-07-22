SET NAMES utf8mb4 COLLATE utf8mb4_0900_ai_ci;

ALTER TABLE users
    ADD COLUMN login_id VARCHAR(50) NULL AFTER id,
    ADD COLUMN name VARCHAR(100) NULL AFTER password_hash,
    ADD COLUMN phone VARCHAR(30) NULL AFTER name,
    ADD COLUMN deleted_at DATETIME(6) NULL AFTER status;

UPDATE users
SET login_id = CONCAT('__legacy_user_', id)
WHERE login_id IS NULL;

UPDATE users
SET name = '미등록 사용자'
WHERE name IS NULL;

ALTER TABLE users
    MODIFY COLUMN login_id VARCHAR(50) NOT NULL,
    MODIFY COLUMN name VARCHAR(100) NOT NULL,
    ADD UNIQUE KEY uk_users_login_id (login_id);

ALTER TABLE users
    DROP CHECK ck_users_role;

UPDATE users
SET role = 'OWNER'
WHERE role = 'EMPLOYER';

ALTER TABLE users
    ADD CONSTRAINT ck_users_role
        CHECK (role IN ('OWNER', 'WORKER', 'ADMIN'));

ALTER TABLE users
    DROP CHECK ck_users_status;

ALTER TABLE users
    ADD CONSTRAINT ck_users_status
        CHECK (status IN ('ACTIVE', 'INACTIVE', 'LOCKED', 'WITHDRAWN')),
    ADD CONSTRAINT ck_users_withdrawn_at
        CHECK (
            (status = 'WITHDRAWN' AND deleted_at IS NOT NULL)
            OR (status <> 'WITHDRAWN' AND deleted_at IS NULL)
        );

ALTER TABLE mock_bank_accounts
    ADD COLUMN bank_code CHAR(3) CHARACTER SET ascii
        COLLATE ascii_bin NULL AFTER user_id,
    ADD COLUMN mock_account_number VARCHAR(64) CHARACTER SET ascii
        COLLATE ascii_bin NULL AFTER bank_code;

UPDATE mock_bank_accounts
SET bank_code = '000',
    mock_account_number = mock_fintech_use_num
WHERE bank_code IS NULL
   OR mock_account_number IS NULL;

ALTER TABLE mock_bank_accounts
    MODIFY COLUMN bank_code CHAR(3) CHARACTER SET ascii
        COLLATE ascii_bin NOT NULL,
    MODIFY COLUMN mock_account_number VARCHAR(64) CHARACTER SET ascii
        COLLATE ascii_bin NOT NULL,
    ADD UNIQUE KEY uk_mock_bank_accounts_bank_account
        (bank_code, mock_account_number),
    ADD CONSTRAINT ck_mock_bank_accounts_bank_code
        CHECK (bank_code REGEXP '^[0-9]{3}$'),
    ADD CONSTRAINT ck_mock_bank_accounts_account_number
        CHECK (CHAR_LENGTH(TRIM(mock_account_number)) > 0);

CREATE TABLE workplaces (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    owner_user_id BIGINT UNSIGNED NOT NULL,
    business_registration_number CHAR(10) CHARACTER SET ascii
        COLLATE ascii_bin NOT NULL,
    name VARCHAR(120) NOT NULL,
    representative_name VARCHAR(100) NOT NULL,
    address VARCHAR(255) NOT NULL,
    phone VARCHAR(30) NOT NULL,
    latitude DECIMAL(10, 7) NULL,
    longitude DECIMAL(10, 7) NULL,
    radius_meters DECIMAL(8, 2) NOT NULL DEFAULT 100.00,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    deleted_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_workplaces_business_registration_number
        (business_registration_number),
    UNIQUE KEY uk_workplaces_owner_id (owner_user_id, id),
    KEY idx_workplaces_owner_status (owner_user_id, status),
    CONSTRAINT fk_workplaces_owner
        FOREIGN KEY (owner_user_id) REFERENCES users (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT ck_workplaces_business_registration_number
        CHECK (business_registration_number REGEXP '^[0-9]{10}$'),
    CONSTRAINT ck_workplaces_required_text
        CHECK (
            CHAR_LENGTH(TRIM(name)) > 0
            AND CHAR_LENGTH(TRIM(representative_name)) > 0
            AND CHAR_LENGTH(TRIM(address)) > 0
            AND CHAR_LENGTH(TRIM(phone)) > 0
        ),
    CONSTRAINT ck_workplaces_coordinates
        CHECK (
            (latitude IS NULL AND longitude IS NULL)
            OR (
                latitude BETWEEN -90 AND 90
                AND longitude BETWEEN -180 AND 180
            )
        ),
    CONSTRAINT ck_workplaces_radius CHECK (radius_meters > 0),
    CONSTRAINT ck_workplaces_status
        CHECK (status IN ('ACTIVE', 'INACTIVE', 'DELETED')),
    CONSTRAINT ck_workplaces_deleted_at
        CHECK (
            (status = 'DELETED' AND deleted_at IS NOT NULL)
            OR (status <> 'DELETED' AND deleted_at IS NULL)
        )
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_0900_ai_ci;

ALTER TABLE work_cases
    ADD COLUMN workplace_id BIGINT UNSIGNED NOT NULL AFTER worker_id,
    ADD KEY idx_work_cases_workplace_status_start
        (workplace_id, status, starts_at),
    ADD KEY idx_work_cases_employer_workplace
        (employer_id, workplace_id),
    ADD CONSTRAINT fk_work_cases_employer_workplace
        FOREIGN KEY (employer_id, workplace_id)
        REFERENCES workplaces (owner_user_id, id)
        ON DELETE RESTRICT ON UPDATE RESTRICT;
