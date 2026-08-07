package com.gighub.idempotency;

import com.gighub.common.exception.ValidationException;

import java.util.regex.Pattern;

/**
 * 모든 멱등 Operation이 공유하는 {@code Idempotency-Key} 형식 규칙입니다.
 *
 * <p>승인 계약은 "공백 없는 출력 가능한 ASCII 1~100자"만 허용합니다. Operation마다 형식을
 * 따로 두면 같은 Header가 Endpoint별로 다르게 거절됩니다.</p>
 */
public final class IdempotencyKeys {

    private static final int MAX_LENGTH = 100;

    /** 공백과 제어 문자를 제외한 출력 가능한 ASCII입니다. */
    private static final Pattern VISIBLE_ASCII =
            Pattern.compile("\\A[\\x21-\\x7E]{1," + MAX_LENGTH + "}\\z");

    private IdempotencyKeys() {
    }

    /**
     * @param rawKey 요청 Header가 전달한 Key 원문
     * @return 형식 검증을 통과한 같은 Key
     * @throws ValidationException 형식이 계약과 다를 때
     */
    public static String validate(String rawKey) {
        if (rawKey == null || !VISIBLE_ASCII.matcher(rawKey).matches()) {
            // 오류 메시지에 받은 Key를 되돌려 담지 않습니다. Key는 로그·오류에 남기지
            // 않기로 한 값이라 형식 규칙만 알립니다.
            throw new ValidationException(
                    "멱등 키는 1~100자의 공백 없는 출력 가능한 ASCII 문자열이어야 합니다."
            );
        }
        return rawKey;
    }
}
