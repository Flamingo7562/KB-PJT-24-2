package com.gighub.common.validation;

import java.lang.reflect.Field;

import javax.validation.constraints.Pattern;

import com.gighub.auth.dto.SignupRequest;
import com.gighub.member.dto.UserProfileUpdateRequest;
import com.gighub.workplace.dto.WorkplaceCreateRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class PhoneNormalizationContractTest {

    @Test
    void normalizesApprovedSeparatorsAndEmptyInput() {
        assertEquals("01012345678", PhoneNormalizer.normalize("010-1234 5678"));
        assertNull(PhoneNormalizer.normalize(null));
        assertNull(PhoneNormalizer.normalize(" - \t"));
    }

    @Test
    void allPhoneRequestsUseSharedNormalization() {
        String input = "02-1234 5678";
        String expected = PhoneNormalizer.normalize(input);

        SignupRequest signupRequest = new SignupRequest();
        signupRequest.setPhone(input);
        UserProfileUpdateRequest profileRequest = new UserProfileUpdateRequest(input);
        WorkplaceCreateRequest workplaceRequest = WorkplaceCreateRequest.builder()
                .phone(input)
                .build();

        assertAll(
                () -> assertEquals(expected, signupRequest.getPhone()),
                () -> assertEquals(expected, profileRequest.getPhone()),
                () -> assertEquals(expected, workplaceRequest.getPhone()));
    }

    @Test
    void allPhoneRequestsUseSharedValidationPattern() throws NoSuchFieldException {
        assertAll(
                () -> assertEquals(
                        PhoneNormalizer.VALID_PATTERN,
                        phonePattern(SignupRequest.class)),
                () -> assertEquals(
                        PhoneNormalizer.VALID_PATTERN,
                        phonePattern(UserProfileUpdateRequest.class)),
                () -> assertEquals(
                        PhoneNormalizer.VALID_PATTERN,
                        phonePattern(WorkplaceCreateRequest.class)));
    }

    private String phonePattern(Class<?> requestType) throws NoSuchFieldException {
        Field phone = requestType.getDeclaredField("phone");
        return phone.getAnnotation(Pattern.class).regexp();
    }
}
