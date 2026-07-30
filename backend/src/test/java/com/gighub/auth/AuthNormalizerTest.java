package com.gighub.auth;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class AuthNormalizerTest {

    @Test
    void loginIdIsTrimmedOnly() {
        assertEquals("abc123", AuthNormalizer.loginId("  abc123  "));
    }

    @Test
    void emailIsTrimmedAndLowercased() {
        assertEquals("user@test.com", AuthNormalizer.email("  USER@Test.COM  "));
    }

    @Test
    void phoneStripsHyphensAndSpaces() {
        assertEquals("01012345678", AuthNormalizer.phone("010-1234-5678"));
        assertEquals("01012345678", AuthNormalizer.phone(" 010 1234 5678 "));
    }

    @Test
    void phoneReturnsNullForBlankInput() {
        assertNull(AuthNormalizer.phone(null));
        assertNull(AuthNormalizer.phone("   "));
        assertNull(AuthNormalizer.phone(""));
    }

    @Test
    void nameIsTrimmedOnly() {
        assertEquals("김철수", AuthNormalizer.name("  김철수  "));
    }
}
