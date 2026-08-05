-- 기존 계좌 ID와 참조는 그대로 두고, 사용자 귀속 컬럼만 Demo PIN으로 치환한다.
-- NOT NULL DEFAULT 추가가 기존 행도 0000으로 채우므로 updated_at을 바꾸는 별도 UPDATE는 하지 않는다.
ALTER TABLE mock_bank_accounts
    DROP FOREIGN KEY fk_mock_bank_accounts_user,
    DROP INDEX idx_mock_bank_accounts_user_status,
    DROP COLUMN user_id,
    ADD COLUMN pin CHAR(4) CHARACTER SET ascii
        COLLATE ascii_bin NOT NULL DEFAULT '0000'
        AFTER mock_account_number,
    ADD CONSTRAINT ck_mock_bank_accounts_pin
        CHECK (pin REGEXP '^[0-9]{4}$');
