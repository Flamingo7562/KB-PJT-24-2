SET NAMES utf8mb4 COLLATE utf8mb4_0900_ai_ci;

-- Keep every existing address intact as the road address. Legacy combined
-- strings cannot be split reliably, so detail_address starts as NULL.
-- Re-adding the coordinate check validates existing rows. Partial legacy
-- coordinates intentionally block migration instead of being guessed or cleared.
ALTER TABLE workplaces
    DROP CHECK ck_workplaces_required_text,
    DROP CHECK ck_workplaces_coordinates,
    RENAME COLUMN address TO road_address,
    ADD COLUMN detail_address VARCHAR(100) NULL AFTER road_address,
    ADD CONSTRAINT ck_workplaces_required_text
        CHECK (
            CHAR_LENGTH(TRIM(name)) > 0
            AND CHAR_LENGTH(TRIM(representative_name)) > 0
            AND CHAR_LENGTH(TRIM(road_address)) > 0
            AND CHAR_LENGTH(TRIM(phone)) > 0
        ),
    ADD CONSTRAINT ck_workplaces_detail_address
        CHECK (
            detail_address IS NULL
            OR CHAR_LENGTH(TRIM(detail_address)) > 0
        ),
    ADD CONSTRAINT ck_workplaces_coordinates
        CHECK (
            (latitude IS NULL AND longitude IS NULL)
            OR (
                latitude IS NOT NULL
                AND longitude IS NOT NULL
                AND latitude BETWEEN -90 AND 90
                AND longitude BETWEEN -180 AND 180
            )
        );
