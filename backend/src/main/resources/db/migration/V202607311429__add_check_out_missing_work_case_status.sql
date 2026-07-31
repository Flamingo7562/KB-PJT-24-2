SET NAMES utf8mb4 COLLATE utf8mb4_0900_ai_ci;

-- 출근 성공 후 퇴근 기록이 없는 근무를 노쇼와 구분하되,
-- 판정 시점·해소·정산 Workflow와 기존 데이터 이관은 후속 정책으로 남긴다.
ALTER TABLE work_cases
    DROP CHECK ck_work_cases_status,
    DROP CHECK ck_work_cases_matched_worker,
    ADD CONSTRAINT ck_work_cases_status
        CHECK (
            status IN (
                'DRAFT', 'ACCEPTED', 'READY', 'IN_PROGRESS',
                'CHECK_OUT_MISSING', 'COMPLETED', 'NO_SHOW', 'CANCELED'
            )
        ),
    ADD CONSTRAINT ck_work_cases_matched_worker
        CHECK (
            status NOT IN (
                'ACCEPTED', 'READY', 'IN_PROGRESS',
                'CHECK_OUT_MISSING', 'COMPLETED', 'NO_SHOW'
            )
            OR worker_id IS NOT NULL
        );
