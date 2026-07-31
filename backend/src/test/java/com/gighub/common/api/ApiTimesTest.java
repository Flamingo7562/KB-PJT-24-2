package com.gighub.common.api;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ApiTimesTest {

    @Test
    void convertsSeoulDatabaseTimeToUtcInstant() {
        LocalDateTime storedAt = LocalDateTime.of(
                2026,
                7,
                31,
                18,
                0
        );

        assertEquals(
                Instant.parse("2026-07-31T09:00:00Z"),
                ApiTimes.toInstant(storedAt)
        );
    }

    @Test
    void keepsNullAsNull() {
        assertNull(ApiTimes.toInstant(null));
    }
}
