SET NAMES utf8mb4 COLLATE utf8mb4_0900_ai_ci;

CREATE TABLE users (
                       id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
                       email VARCHAR(255) NOT NULL,
                       password_hash VARCHAR(255) NOT NULL,
                       role VARCHAR(20) NOT NULL,
                       status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
                       created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                       updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),
                       PRIMARY KEY (id),
                       UNIQUE KEY uk_users_email (email),
                       CONSTRAINT ck_users_role
                           CHECK (role IN ('EMPLOYER', 'WORKER', 'ADMIN')),
                       CONSTRAINT ck_users_status
                           CHECK (status IN ('ACTIVE', 'INACTIVE', 'LOCKED'))
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_0900_ai_ci;

CREATE TABLE employer_profiles (
                                   id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
                                   user_id BIGINT UNSIGNED NOT NULL,
                                   business_name VARCHAR(120) NOT NULL,
                                   contact_phone VARCHAR(30) NULL,
                                   default_workplace_address VARCHAR(255) NULL,
                                   created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                                   updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),
                                   PRIMARY KEY (id),
                                   UNIQUE KEY uk_employer_profiles_user_id (user_id),
                                   CONSTRAINT fk_employer_profiles_user
                                       FOREIGN KEY (user_id) REFERENCES users (id)
                                           ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_0900_ai_ci;

CREATE TABLE wallets (
                         id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
                         user_id BIGINT UNSIGNED NOT NULL,
                         currency CHAR(3) NOT NULL DEFAULT 'KRW',
                         available_balance BIGINT UNSIGNED NOT NULL DEFAULT 0,
                         locked_balance BIGINT UNSIGNED NOT NULL DEFAULT 0,
                         created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                         updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),
                         PRIMARY KEY (id),
                         UNIQUE KEY uk_wallets_user_currency (user_id, currency),
                         CONSTRAINT fk_wallets_user
                             FOREIGN KEY (user_id) REFERENCES users (id)
                                 ON DELETE RESTRICT ON UPDATE RESTRICT,
                         CONSTRAINT ck_wallets_currency CHECK (currency = 'KRW')
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_0900_ai_ci;

CREATE TABLE mock_bank_accounts (
                                    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
                                    user_id BIGINT UNSIGNED NOT NULL,
                                    mock_fintech_use_num VARCHAR(64) CHARACTER SET ascii
                                        COLLATE ascii_bin NOT NULL,
                                    currency CHAR(3) NOT NULL DEFAULT 'KRW',
                                    balance BIGINT UNSIGNED NOT NULL DEFAULT 0,
                                    available_amount BIGINT UNSIGNED NOT NULL DEFAULT 0,
                                    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
                                    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                                    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),
                                    PRIMARY KEY (id),
                                    UNIQUE KEY uk_mock_bank_accounts_fintech_use_num
                                        (mock_fintech_use_num),
                                    KEY idx_mock_bank_accounts_user_status (user_id, status),
                                    CONSTRAINT fk_mock_bank_accounts_user
                                        FOREIGN KEY (user_id) REFERENCES users (id)
                                            ON DELETE RESTRICT ON UPDATE RESTRICT,
                                    CONSTRAINT ck_mock_bank_accounts_currency CHECK (currency = 'KRW'),
                                    CONSTRAINT ck_mock_bank_accounts_amounts
                                        CHECK (available_amount <= balance),
                                    CONSTRAINT ck_mock_bank_accounts_status
                                        CHECK (status IN ('ACTIVE', 'BLOCKED', 'CLOSED'))
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_0900_ai_ci;

CREATE TABLE work_cases (
                            id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
                            employer_id BIGINT UNSIGNED NOT NULL,
                            worker_id BIGINT UNSIGNED NULL,
                            title VARCHAR(150) NOT NULL,
                            starts_at DATETIME(6) NOT NULL,
                            ends_at DATETIME(6) NOT NULL,
                            break_minutes SMALLINT UNSIGNED NOT NULL DEFAULT 0,
                            workplace_name VARCHAR(150) NOT NULL,
                            workplace_address VARCHAR(255) NOT NULL,
                            workplace_latitude DECIMAL(10, 7) NULL,
                            workplace_longitude DECIMAL(10, 7) NULL,
                            allowed_radius_meters DECIMAL(8, 2) NOT NULL DEFAULT 100.00,
                            agreed_wage BIGINT UNSIGNED NOT NULL,
                            terms_version INT UNSIGNED NOT NULL DEFAULT 1,
                            status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
                            canceled_at DATETIME(6) NULL,
                            created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                            updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),
                            PRIMARY KEY (id),
                            KEY idx_work_cases_employer_status_start
                                (employer_id, status, starts_at),
                            KEY idx_work_cases_worker_status_start
                                (worker_id, status, starts_at),
                            KEY idx_work_cases_status_start (status, starts_at),
                            CONSTRAINT fk_work_cases_employer
                                FOREIGN KEY (employer_id) REFERENCES users (id)
                                    ON DELETE RESTRICT ON UPDATE RESTRICT,
                            CONSTRAINT fk_work_cases_worker
                                FOREIGN KEY (worker_id) REFERENCES users (id)
                                    ON DELETE RESTRICT ON UPDATE RESTRICT,
                            CONSTRAINT ck_work_cases_time CHECK (ends_at > starts_at),
                            CONSTRAINT ck_work_cases_wage CHECK (agreed_wage > 0),
                            CONSTRAINT ck_work_cases_terms_version CHECK (terms_version >= 1),
                            CONSTRAINT ck_work_cases_coordinates CHECK (
                                (workplace_latitude IS NULL AND workplace_longitude IS NULL)
                                    OR (
                                    workplace_latitude BETWEEN -90 AND 90
                                        AND workplace_longitude BETWEEN -180 AND 180
                                    )
                                ),
                            CONSTRAINT ck_work_cases_radius CHECK (allowed_radius_meters > 0),
                            CONSTRAINT ck_work_cases_status CHECK (
                                status IN (
                                           'DRAFT', 'INVITED', 'ACCEPTED', 'READY',
                                           'IN_PROGRESS', 'COMPLETED', 'NO_SHOW', 'CANCELED'
                                    )
                                )
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_0900_ai_ci;

CREATE TABLE work_invitations (
                                  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
                                  work_case_id BIGINT UNSIGNED NOT NULL,
                                  token_hash BINARY(32) NOT NULL,
                                  status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
                                  expected_terms_version INT UNSIGNED NOT NULL,
                                  expires_at DATETIME(6) NOT NULL,
                                  accepted_by_user_id BIGINT UNSIGNED NULL,
                                  accepted_terms_version INT UNSIGNED NULL,
                                  accepted_at DATETIME(6) NULL,
                                  rejected_at DATETIME(6) NULL,
                                  revoked_at DATETIME(6) NULL,
                                  active_slot TINYINT GENERATED ALWAYS AS (
                                      CASE WHEN status = 'PENDING' THEN 1 ELSE NULL END
                                      ) STORED,
                                  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                                  PRIMARY KEY (id),
                                  UNIQUE KEY uk_work_invitations_token_hash (token_hash),
                                  UNIQUE KEY uk_work_invitations_active
                                      (work_case_id, active_slot),
                                  KEY idx_work_invitations_case_status_expiry
                                      (work_case_id, status, expires_at),
                                  CONSTRAINT fk_work_invitations_work_case
                                      FOREIGN KEY (work_case_id) REFERENCES work_cases (id)
                                          ON DELETE RESTRICT ON UPDATE RESTRICT,
                                  CONSTRAINT fk_work_invitations_accepted_user
                                      FOREIGN KEY (accepted_by_user_id) REFERENCES users (id)
                                          ON DELETE RESTRICT ON UPDATE RESTRICT,
                                  CONSTRAINT ck_work_invitations_status CHECK (
                                      status IN ('PENDING', 'ACCEPTED', 'REJECTED', 'REVOKED', 'EXPIRED')
                                      )
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_0900_ai_ci;

CREATE TABLE work_contracts (
                                id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
                                work_case_id BIGINT UNSIGNED NOT NULL,
                                employer_id BIGINT UNSIGNED NOT NULL,
                                worker_id BIGINT UNSIGNED NOT NULL,
                                title VARCHAR(150) NOT NULL,
                                starts_at DATETIME(6) NOT NULL,
                                ends_at DATETIME(6) NOT NULL,
                                break_minutes SMALLINT UNSIGNED NOT NULL,
                                workplace_name VARCHAR(150) NOT NULL,
                                workplace_address VARCHAR(255) NOT NULL,
                                workplace_latitude DECIMAL(10, 7) NULL,
                                workplace_longitude DECIMAL(10, 7) NULL,
                                allowed_radius_meters DECIMAL(8, 2) NOT NULL,
                                agreed_wage BIGINT UNSIGNED NOT NULL,
                                source_terms_version INT UNSIGNED NOT NULL,
                                terms_snapshot JSON NOT NULL,
                                accepted_at DATETIME(6) NOT NULL,
                                created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                                PRIMARY KEY (id),
                                UNIQUE KEY uk_work_contracts_work_case_id (work_case_id),
                                CONSTRAINT fk_work_contracts_work_case
                                    FOREIGN KEY (work_case_id) REFERENCES work_cases (id)
                                        ON DELETE RESTRICT ON UPDATE RESTRICT,
                                CONSTRAINT fk_work_contracts_employer
                                    FOREIGN KEY (employer_id) REFERENCES users (id)
                                        ON DELETE RESTRICT ON UPDATE RESTRICT,
                                CONSTRAINT fk_work_contracts_worker
                                    FOREIGN KEY (worker_id) REFERENCES users (id)
                                        ON DELETE RESTRICT ON UPDATE RESTRICT,
                                CONSTRAINT ck_work_contracts_time CHECK (ends_at > starts_at),
                                CONSTRAINT ck_work_contracts_wage CHECK (agreed_wage > 0),
                                CONSTRAINT ck_work_contracts_terms_version
                                    CHECK (source_terms_version >= 1)
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_0900_ai_ci;

CREATE TABLE mock_bank_transactions (
                                        id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
                                        account_id BIGINT UNSIGNED NOT NULL,
                                        bank_tran_id VARCHAR(64) CHARACTER SET ascii
                                            COLLATE ascii_bin NOT NULL,
                                        transfer_type VARCHAR(20) NOT NULL,
                                        amount BIGINT UNSIGNED NOT NULL,
                                        balance_before BIGINT UNSIGNED NOT NULL,
                                        balance_after BIGINT UNSIGNED NOT NULL,
                                        reference_type VARCHAR(30) NOT NULL,
                                        reference_id BIGINT UNSIGNED NOT NULL,
                                        status VARCHAR(20) NOT NULL,
                                        failure_code VARCHAR(50) NULL,
                                        created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                                        PRIMARY KEY (id),
                                        UNIQUE KEY uk_mock_bank_transactions_bank_tran_id (bank_tran_id),
                                        UNIQUE KEY uk_mock_bank_transactions_reference (
                                            reference_type, reference_id, transfer_type
                                            ),
                                        KEY idx_mock_bank_transactions_account_created
                                            (account_id, created_at),
                                        CONSTRAINT fk_mock_bank_transactions_account
                                            FOREIGN KEY (account_id) REFERENCES mock_bank_accounts (id)
                                                ON DELETE RESTRICT ON UPDATE RESTRICT,
                                        CONSTRAINT ck_mock_bank_transactions_amount CHECK (amount > 0),
                                        CONSTRAINT ck_mock_bank_transactions_transfer_type
                                            CHECK (transfer_type IN ('WITHDRAW', 'DEPOSIT')),
                                        CONSTRAINT ck_mock_bank_transactions_status
                                            CHECK (status IN ('READY', 'PROCESSING', 'SUCCESS', 'FAILED'))
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_0900_ai_ci;

CREATE TABLE funding_orders (
                                id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
                                employer_id BIGINT UNSIGNED NOT NULL,
                                linked_account_id BIGINT UNSIGNED NOT NULL,
                                expected_amount BIGINT UNSIGNED NOT NULL,
                                transferred_amount BIGINT UNSIGNED NULL,
                                mock_bank_transaction_id BIGINT UNSIGNED NULL,
                                idempotency_key VARCHAR(100) CHARACTER SET ascii
                                    COLLATE ascii_bin NOT NULL,
                                status VARCHAR(30) NOT NULL DEFAULT 'READY',
                                failure_code VARCHAR(50) NULL,
                                completed_at DATETIME(6) NULL,
                                created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                                updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),
                                PRIMARY KEY (id),
                                UNIQUE KEY uk_funding_orders_idempotency_key (idempotency_key),
                                UNIQUE KEY uk_funding_orders_bank_transaction
                                    (mock_bank_transaction_id),
                                KEY idx_funding_orders_employer_status_created
                                    (employer_id, status, created_at),
                                CONSTRAINT fk_funding_orders_employer
                                    FOREIGN KEY (employer_id) REFERENCES users (id)
                                        ON DELETE RESTRICT ON UPDATE RESTRICT,
                                CONSTRAINT fk_funding_orders_linked_account
                                    FOREIGN KEY (linked_account_id) REFERENCES mock_bank_accounts (id)
                                        ON DELETE RESTRICT ON UPDATE RESTRICT,
                                CONSTRAINT fk_funding_orders_bank_transaction
                                    FOREIGN KEY (mock_bank_transaction_id)
                                        REFERENCES mock_bank_transactions (id)
                                        ON DELETE RESTRICT ON UPDATE RESTRICT,
                                CONSTRAINT ck_funding_orders_expected_amount
                                    CHECK (expected_amount > 0),
                                CONSTRAINT ck_funding_orders_transferred_amount
                                    CHECK (transferred_amount IS NULL OR transferred_amount > 0),
                                CONSTRAINT ck_funding_orders_status CHECK (
                                    status IN (
                                               'READY', 'COMPLETED', 'FAILED', 'RECONCILIATION_REQUIRED'
                                        )
                                    )
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_0900_ai_ci;

CREATE TABLE withdrawal_requests (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    worker_id BIGINT UNSIGNED NOT NULL,
    wallet_id BIGINT UNSIGNED NOT NULL,
    linked_account_id BIGINT UNSIGNED NOT NULL,
    amount BIGINT UNSIGNED NOT NULL,
    mock_bank_transaction_id BIGINT UNSIGNED NULL,
    idempotency_key VARCHAR(100) CHARACTER SET ascii
        COLLATE ascii_bin NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'READY',
    failure_code VARCHAR(50) NULL,
    completed_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_withdrawal_requests_idempotency_key (idempotency_key),
    UNIQUE KEY uk_withdrawal_requests_bank_transaction
        (mock_bank_transaction_id),
    KEY idx_withdrawal_requests_worker_status_created
        (worker_id, status, created_at),
    CONSTRAINT fk_withdrawal_requests_worker
        FOREIGN KEY (worker_id) REFERENCES users (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_withdrawal_requests_wallet
        FOREIGN KEY (wallet_id) REFERENCES wallets (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_withdrawal_requests_linked_account
        FOREIGN KEY (linked_account_id) REFERENCES mock_bank_accounts (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_withdrawal_requests_bank_transaction
        FOREIGN KEY (mock_bank_transaction_id)
        REFERENCES mock_bank_transactions (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT ck_withdrawal_requests_amount CHECK (amount > 0),
    CONSTRAINT ck_withdrawal_requests_status CHECK (
        status IN (
            'READY', 'PROCESSING', 'COMPLETED', 'FAILED',
            'RECONCILIATION_REQUIRED'
        )
    )
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_0900_ai_ci;

CREATE TABLE wallet_transactions (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    wallet_id BIGINT UNSIGNED NOT NULL,
    work_case_id BIGINT UNSIGNED NULL,
    transaction_type VARCHAR(30) NOT NULL,
    amount BIGINT UNSIGNED NOT NULL,
    available_before BIGINT UNSIGNED NOT NULL,
    available_after BIGINT UNSIGNED NOT NULL,
    locked_before BIGINT UNSIGNED NOT NULL,
    locked_after BIGINT UNSIGNED NOT NULL,
    reference_type VARCHAR(30) NOT NULL,
    reference_id BIGINT UNSIGNED NOT NULL,
    idempotency_key VARCHAR(100) CHARACTER SET ascii
        COLLATE ascii_bin NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_wallet_transactions_idempotency_key (idempotency_key),
    KEY idx_wallet_transactions_wallet_created (wallet_id, created_at),
    KEY idx_wallet_transactions_work_case_created
        (work_case_id, created_at),
    CONSTRAINT fk_wallet_transactions_wallet
        FOREIGN KEY (wallet_id) REFERENCES wallets (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_wallet_transactions_work_case
        FOREIGN KEY (work_case_id) REFERENCES work_cases (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT ck_wallet_transactions_amount CHECK (amount > 0),
    CONSTRAINT ck_wallet_transactions_type CHECK (
        transaction_type IN (
            'FUNDING', 'ESCROW_HOLD', 'ESCROW_RELEASE',
            'ESCROW_REFUND', 'WITHDRAWAL', 'WITHDRAWAL_REFUND',
            'ADJUSTMENT'
        )
    )
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_0900_ai_ci;

CREATE TABLE escrows (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    work_case_id BIGINT UNSIGNED NOT NULL,
    amount BIGINT UNSIGNED NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'UNFUNDED',
    held_at DATETIME(6) NULL,
    released_at DATETIME(6) NULL,
    refunded_at DATETIME(6) NULL,
    on_hold_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_escrows_work_case_id (work_case_id),
    CONSTRAINT fk_escrows_work_case
        FOREIGN KEY (work_case_id) REFERENCES work_cases (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT ck_escrows_amount CHECK (amount > 0),
    CONSTRAINT ck_escrows_status CHECK (
        status IN ('UNFUNDED', 'HELD', 'RELEASED', 'REFUNDED', 'ON_HOLD')
    )
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_0900_ai_ci;

CREATE TABLE qr_tokens (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    work_case_id BIGINT UNSIGNED NOT NULL,
    issued_by_user_id BIGINT UNSIGNED NOT NULL,
    token_hash BINARY(32) NOT NULL,
    action VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    expires_at DATETIME(6) NOT NULL,
    used_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_qr_tokens_token_hash (token_hash),
    KEY idx_qr_tokens_case_action_status_expiry
        (work_case_id, action, status, expires_at),
    CONSTRAINT fk_qr_tokens_work_case
        FOREIGN KEY (work_case_id) REFERENCES work_cases (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_qr_tokens_issuer
        FOREIGN KEY (issued_by_user_id) REFERENCES users (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT ck_qr_tokens_action
        CHECK (action IN ('CHECK_IN', 'CHECK_OUT')),
    CONSTRAINT ck_qr_tokens_status
        CHECK (status IN ('ACTIVE', 'USED', 'EXPIRED', 'REVOKED'))
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_0900_ai_ci;

CREATE TABLE attendance_records (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    work_case_id BIGINT UNSIGNED NOT NULL,
    worker_id BIGINT UNSIGNED NOT NULL,
    qr_token_id BIGINT UNSIGNED NULL,
    attendance_type VARCHAR(20) NOT NULL,
    captured_at DATETIME(6) NOT NULL,
    attempted_at DATETIME(6) NOT NULL,
    distance_meters DECIMAL(10, 2) NULL,
    accuracy_meters DECIMAL(10, 2) NULL,
    result VARCHAR(20) NOT NULL,
    failure_reason VARCHAR(100) NULL,
    success_slot TINYINT GENERATED ALWAYS AS (
        CASE WHEN result = 'SUCCESS' THEN 1 ELSE NULL END
    ) STORED,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_attendance_records_success (
        work_case_id, attendance_type, success_slot
    ),
    KEY idx_attendance_records_case_type_created
        (work_case_id, attendance_type, created_at),
    KEY idx_attendance_records_worker_created (worker_id, created_at),
    CONSTRAINT fk_attendance_records_work_case
        FOREIGN KEY (work_case_id) REFERENCES work_cases (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_attendance_records_worker
        FOREIGN KEY (worker_id) REFERENCES users (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_attendance_records_qr_token
        FOREIGN KEY (qr_token_id) REFERENCES qr_tokens (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT ck_attendance_records_type
        CHECK (attendance_type IN ('CHECK_IN', 'CHECK_OUT')),
    CONSTRAINT ck_attendance_records_result
        CHECK (result IN ('SUCCESS', 'REJECTED')),
    CONSTRAINT ck_attendance_records_distance
        CHECK (distance_meters IS NULL OR distance_meters >= 0),
    CONSTRAINT ck_attendance_records_accuracy
        CHECK (accuracy_meters IS NULL OR accuracy_meters >= 0)
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_0900_ai_ci;

CREATE TABLE settlements (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    work_case_id BIGINT UNSIGNED NOT NULL,
    amount BIGINT UNSIGNED NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'WAITING',
    approved_by_user_id BIGINT UNSIGNED NULL,
    due_at DATETIME(6) NULL,
    processing_at DATETIME(6) NULL,
    completed_at DATETIME(6) NULL,
    failure_code VARCHAR(50) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_settlements_work_case_id (work_case_id),
    KEY idx_settlements_status_due_at (status, due_at),
    CONSTRAINT fk_settlements_work_case
        FOREIGN KEY (work_case_id) REFERENCES work_cases (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_settlements_approver
        FOREIGN KEY (approved_by_user_id) REFERENCES users (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT ck_settlements_amount CHECK (amount > 0),
    CONSTRAINT ck_settlements_status CHECK (
        status IN (
            'WAITING', 'SCHEDULED', 'PROCESSING',
            'COMPLETED', 'FAILED', 'ON_HOLD'
        )
    )
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_0900_ai_ci;

CREATE TABLE disputes (
                          id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
                          work_case_id BIGINT UNSIGNED NOT NULL,
                          requester_id BIGINT UNSIGNED NOT NULL,
                          dispute_type VARCHAR(30) NOT NULL,
                          content TEXT NOT NULL,
                          status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
                          resolution TEXT NULL,
                          resolved_by_user_id BIGINT UNSIGNED NULL,
                          resolved_at DATETIME(6) NULL,
                          open_slot TINYINT GENERATED ALWAYS AS (
                              CASE WHEN status IN ('OPEN', 'UNDER_REVIEW') THEN 1 ELSE NULL END
                              ) STORED,
                          created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                          updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),
                          PRIMARY KEY (id),
                          UNIQUE KEY uk_disputes_open (work_case_id, open_slot),
                          KEY idx_disputes_status_created (status, created_at),
                          CONSTRAINT fk_disputes_work_case
                              FOREIGN KEY (work_case_id) REFERENCES work_cases (id)
                                  ON DELETE RESTRICT ON UPDATE RESTRICT,
                          CONSTRAINT fk_disputes_requester
                              FOREIGN KEY (requester_id) REFERENCES users (id)
                                  ON DELETE RESTRICT ON UPDATE RESTRICT,
                          CONSTRAINT fk_disputes_resolver
                              FOREIGN KEY (resolved_by_user_id) REFERENCES users (id)
                                  ON DELETE RESTRICT ON UPDATE RESTRICT,
                          CONSTRAINT ck_disputes_status CHECK (
                              status IN (
                                         'OPEN', 'UNDER_REVIEW', 'RESOLVED',
                                         'REJECTED', 'CANCELED'
                                  )
                              )
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_0900_ai_ci;

CREATE TABLE documents (
                           id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
                           created_by_user_id BIGINT UNSIGNED NOT NULL,
                           owner_user_id BIGINT UNSIGNED NOT NULL,
                           work_case_id BIGINT UNSIGNED NULL,
                           document_type VARCHAR(30) NOT NULL,
                           status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
                           issued_on DATE NULL,
                           expires_on DATE NULL,
                           created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                           updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),
                           PRIMARY KEY (id),
                           UNIQUE KEY uk_documents_work_case_type (work_case_id, document_type),
                           KEY idx_documents_owner_status_created
                               (owner_user_id, status, created_at),
                           CONSTRAINT fk_documents_creator
                               FOREIGN KEY (created_by_user_id) REFERENCES users (id)
                                   ON DELETE RESTRICT ON UPDATE RESTRICT,
                           CONSTRAINT fk_documents_owner
                               FOREIGN KEY (owner_user_id) REFERENCES users (id)
                                   ON DELETE RESTRICT ON UPDATE RESTRICT,
                           CONSTRAINT fk_documents_work_case
                               FOREIGN KEY (work_case_id) REFERENCES work_cases (id)
                                   ON DELETE RESTRICT ON UPDATE RESTRICT,
                           CONSTRAINT ck_documents_type CHECK (
                               document_type IN ('EMPLOYMENT_CONTRACT', 'HEALTH_CERTIFICATE')
                               ),
                           CONSTRAINT ck_documents_status CHECK (
                               status IN (
                                          'DRAFT', 'AWAITING_SIGNATURE', 'SIGNED',
                                          'ACTIVE', 'CANCELED', 'DELETED'
                                   )
                               ),
                           CONSTRAINT ck_documents_expiry
                               CHECK (expires_on IS NULL OR issued_on IS NULL OR expires_on >= issued_on)
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_0900_ai_ci;

CREATE TABLE document_versions (
                                   id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
                                   document_id BIGINT UNSIGNED NOT NULL,
                                   version_no INT UNSIGNED NOT NULL,
                                   version_type VARCHAR(20) NOT NULL,
                                   storage_key VARCHAR(255) CHARACTER SET ascii
                                       COLLATE ascii_bin NOT NULL,
                                   mime_type VARCHAR(100) NOT NULL,
                                   size_bytes BIGINT UNSIGNED NOT NULL,
                                   checksum BINARY(32) NOT NULL,
                                   created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                                   PRIMARY KEY (id),
                                   UNIQUE KEY uk_document_versions_storage_key (storage_key),
                                   UNIQUE KEY uk_document_versions_document_version
                                       (document_id, version_no),
                                   UNIQUE KEY uk_document_versions_document_id_id (document_id, id),
                                   KEY idx_document_versions_type_created
                                       (document_id, version_type, created_at),
                                   CONSTRAINT fk_document_versions_document
                                       FOREIGN KEY (document_id) REFERENCES documents (id)
                                           ON DELETE RESTRICT ON UPDATE RESTRICT,
                                   CONSTRAINT ck_document_versions_number CHECK (version_no >= 1),
                                   CONSTRAINT ck_document_versions_type
                                       CHECK (version_type IN ('ORIGINAL', 'SIGNED')),
                                   CONSTRAINT ck_document_versions_size CHECK (size_bytes > 0)
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_0900_ai_ci;

CREATE TABLE document_signatures (
                                     id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
                                     document_id BIGINT UNSIGNED NOT NULL,
                                     source_version_id BIGINT UNSIGNED NOT NULL,
                                     signed_version_id BIGINT UNSIGNED NOT NULL,
                                     signer_user_id BIGINT UNSIGNED NOT NULL,
                                     source_checksum BINARY(32) NOT NULL,
                                     signed_checksum BINARY(32) NOT NULL,
                                     typed_name VARCHAR(100) NOT NULL,
                                     signature_method VARCHAR(20) NOT NULL,
                                     consented_at DATETIME(6) NOT NULL,
                                     signed_at DATETIME(6) NOT NULL,
                                     created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                                     PRIMARY KEY (id),
                                     UNIQUE KEY uk_document_signatures_signed_version (signed_version_id),
                                     UNIQUE KEY uk_document_signatures_once (
                                         document_id, source_version_id, signer_user_id
                                         ),
                                     CONSTRAINT fk_document_signatures_source_version
                                         FOREIGN KEY (document_id, source_version_id)
                                             REFERENCES document_versions (document_id, id)
                                             ON DELETE RESTRICT ON UPDATE RESTRICT,
                                     CONSTRAINT fk_document_signatures_signed_version
                                         FOREIGN KEY (document_id, signed_version_id)
                                             REFERENCES document_versions (document_id, id)
                                             ON DELETE RESTRICT ON UPDATE RESTRICT,
                                     CONSTRAINT fk_document_signatures_signer
                                         FOREIGN KEY (signer_user_id) REFERENCES users (id)
                                             ON DELETE RESTRICT ON UPDATE RESTRICT,
                                     CONSTRAINT ck_document_signatures_versions
                                         CHECK (source_version_id <> signed_version_id),
                                     CONSTRAINT ck_document_signatures_method
                                         CHECK (signature_method IN ('TYPED_NAME', 'DRAWN')),
                                     CONSTRAINT ck_document_signatures_time
                                         CHECK (signed_at >= consented_at)
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_0900_ai_ci;

CREATE TABLE document_shares (
                                 id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
                                 document_id BIGINT UNSIGNED NOT NULL,
                                 work_case_id BIGINT UNSIGNED NOT NULL,
                                 shared_with_user_id BIGINT UNSIGNED NOT NULL,
                                 purpose VARCHAR(30) NOT NULL,
                                 status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
                                 expires_at DATETIME(6) NULL,
                                 revoked_at DATETIME(6) NULL,
                                 active_slot TINYINT GENERATED ALWAYS AS (
                                     CASE WHEN status = 'ACTIVE' THEN 1 ELSE NULL END
                                     ) STORED,
                                 created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                                 PRIMARY KEY (id),
                                 UNIQUE KEY uk_document_shares_active (
                                     document_id, work_case_id, shared_with_user_id,
                                     purpose, active_slot
                                     ),
                                 KEY idx_document_shares_recipient_status_expiry
                                     (shared_with_user_id, status, expires_at),
                                 CONSTRAINT fk_document_shares_document
                                     FOREIGN KEY (document_id) REFERENCES documents (id)
                                         ON DELETE RESTRICT ON UPDATE RESTRICT,
                                 CONSTRAINT fk_document_shares_work_case
                                     FOREIGN KEY (work_case_id) REFERENCES work_cases (id)
                                         ON DELETE RESTRICT ON UPDATE RESTRICT,
                                 CONSTRAINT fk_document_shares_recipient
                                     FOREIGN KEY (shared_with_user_id) REFERENCES users (id)
                                         ON DELETE RESTRICT ON UPDATE RESTRICT,
                                 CONSTRAINT ck_document_shares_purpose CHECK (
                                     purpose IN ('CONTRACT_PARTY', 'HEALTH_CERTIFICATE')
                                     ),
                                 CONSTRAINT ck_document_shares_status
                                     CHECK (status IN ('ACTIVE', 'EXPIRED', 'REVOKED'))
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_0900_ai_ci;

CREATE TABLE document_access_logs (
                                      id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
                                      document_id BIGINT UNSIGNED NOT NULL,
                                      actor_user_id BIGINT UNSIGNED NULL,
                                      action VARCHAR(30) NOT NULL,
                                      result VARCHAR(20) NOT NULL,
                                      created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                                      PRIMARY KEY (id),
                                      KEY idx_document_access_logs_document_created
                                          (document_id, created_at),
                                      KEY idx_document_access_logs_actor_created
                                          (actor_user_id, created_at),
                                      CONSTRAINT fk_document_access_logs_document
                                          FOREIGN KEY (document_id) REFERENCES documents (id)
                                              ON DELETE RESTRICT ON UPDATE RESTRICT,
                                      CONSTRAINT fk_document_access_logs_actor
                                          FOREIGN KEY (actor_user_id) REFERENCES users (id)
                                              ON DELETE RESTRICT ON UPDATE RESTRICT,
                                      CONSTRAINT ck_document_access_logs_result
                                          CHECK (result IN ('ALLOWED', 'DENIED'))
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_0900_ai_ci;

CREATE TABLE user_badges (
                             id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
                             user_id BIGINT UNSIGNED NOT NULL,
                             badge_type VARCHAR(40) NOT NULL,
                             evidence JSON NOT NULL,
                             awarded_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                             PRIMARY KEY (id),
                             UNIQUE KEY uk_user_badges_user_type (user_id, badge_type),
                             CONSTRAINT fk_user_badges_user
                                 FOREIGN KEY (user_id) REFERENCES users (id)
                                     ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_0900_ai_ci;
