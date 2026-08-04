package com.gighub.common.exception;

import com.jayway.jsonpath.JsonPath;
import org.springframework.test.web.servlet.MvcResult;

import java.io.UnsupportedEncodingException;

/** MockMvc JSON 응답에서 계약 필드를 읽는 테스트 전용 도우미입니다. */
final class JsonTestValues {

    private JsonTestValues() {
    }

    static String readString(MvcResult result, String field) throws UnsupportedEncodingException {
        return JsonPath.read(
                result.getResponse().getContentAsString(),
                "$." + field
        );
    }
}
