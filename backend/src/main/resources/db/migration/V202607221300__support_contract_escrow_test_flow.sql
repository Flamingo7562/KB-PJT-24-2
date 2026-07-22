SET NAMES utf8mb4 COLLATE utf8mb4_0900_ai_ci;

ALTER TABLE work_cases
    ADD COLUMN break_paid TINYINT UNSIGNED NOT NULL DEFAULT 0
        AFTER break_minutes,
    ADD UNIQUE KEY uk_work_cases_id_parties_wage
        (id, employer_id, worker_id, agreed_wage),
    ADD UNIQUE KEY uk_work_cases_id_wage (id, agreed_wage),
    ADD CONSTRAINT ck_work_cases_break_paid
        CHECK (break_paid IN (0, 1)),
    ADD CONSTRAINT ck_work_cases_matched_worker
        CHECK (
            status NOT IN ('ACCEPTED', 'READY', 'IN_PROGRESS', 'COMPLETED', 'NO_SHOW')
            OR worker_id IS NOT NULL
        );

ALTER TABLE work_contracts
    ADD COLUMN break_paid TINYINT UNSIGNED NOT NULL DEFAULT 0
        AFTER break_minutes,
    ADD CONSTRAINT ck_work_contracts_break_paid
        CHECK (break_paid IN (0, 1)),
    ADD CONSTRAINT fk_work_contracts_case_parties_wage
        FOREIGN KEY (work_case_id, employer_id, worker_id, agreed_wage)
        REFERENCES work_cases (id, employer_id, worker_id, agreed_wage)
        ON DELETE RESTRICT ON UPDATE RESTRICT;

ALTER TABLE escrows
    ADD CONSTRAINT fk_escrows_case_wage
        FOREIGN KEY (work_case_id, amount)
        REFERENCES work_cases (id, agreed_wage)
        ON DELETE RESTRICT ON UPDATE RESTRICT;

ALTER TABLE settlements
    ADD CONSTRAINT fk_settlements_case_wage
        FOREIGN KEY (work_case_id, amount)
        REFERENCES work_cases (id, agreed_wage)
        ON DELETE RESTRICT ON UPDATE RESTRICT;

ALTER TABLE withdrawal_requests
    DROP FOREIGN KEY fk_withdrawal_requests_worker,
    DROP INDEX idx_withdrawal_requests_worker_status_created,
    CHANGE COLUMN worker_id user_id BIGINT UNSIGNED NOT NULL,
    ADD KEY idx_withdrawal_requests_user_status_created
        (user_id, status, created_at),
    ADD CONSTRAINT fk_withdrawal_requests_user
        FOREIGN KEY (user_id) REFERENCES users (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT;
