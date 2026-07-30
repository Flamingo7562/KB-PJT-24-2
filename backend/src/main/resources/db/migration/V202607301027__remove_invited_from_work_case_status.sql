SET NAMES utf8mb4 COLLATE utf8mb4_0900_ai_ci;

-- Invitation delivery belongs to work_invitations, so unaccepted work cases remain DRAFT.
UPDATE work_cases
SET status = 'DRAFT'
WHERE status = 'INVITED';

ALTER TABLE work_cases
    DROP CHECK ck_work_cases_status,
    ADD CONSTRAINT ck_work_cases_status
        CHECK (
            status IN (
                'DRAFT', 'ACCEPTED', 'READY',
                'IN_PROGRESS', 'COMPLETED', 'NO_SHOW', 'CANCELED'
            )
        );
