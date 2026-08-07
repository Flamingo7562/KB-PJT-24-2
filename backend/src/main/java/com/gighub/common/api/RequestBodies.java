package com.gighub.common.api;

import com.gighub.common.exception.ValidationException;

import javax.servlet.http.HttpServletRequest;

import java.io.IOException;

/**
 * Body를 받지 않기로 한 Operation에서 요청 Body가 실제로 비어 있는지 확인합니다.
 *
 * <p>{@code @RequestBody}를 두지 않으면 Spring은 Body를 그냥 무시합니다. 그러면 client가 보낸
 * 사용자·금액 ID나 서명 이미지가 조용히 버려지고, 보내는 쪽은 서버가 그 값을 반영했다고
 * 오해할 수 있습니다. 승인 계약이 "Body는 0byte"라고 정한 Operation은 무시하지 않고
 * 거절합니다.</p>
 */
public final class RequestBodies {

    private static final String MESSAGE = "이 요청은 본문을 받지 않습니다.";

    private RequestBodies() {
    }

    /**
     * @param request 현재 요청
     * @throws ValidationException Body가 1byte라도 있으면
     */
    public static void requireEmpty(HttpServletRequest request) {
        if (request.getContentLengthLong() > 0L) {
            throw new ValidationException(MESSAGE);
        }
        try {
            // Chunked 전송은 Content-Length가 -1이라 길이만으로는 알 수 없습니다.
            // 한 byte만 읽어 실제로 비어 있는지 확인합니다.
            if (request.getInputStream().read() != -1) {
                throw new ValidationException(MESSAGE);
            }
        } catch (IOException exception) {
            throw new ValidationException(MESSAGE);
        }
    }
}
