package com.gighub.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.cfg.CoercionAction;
import com.fasterxml.jackson.databind.cfg.CoercionInputShape;
import com.fasterxml.jackson.databind.type.LogicalType;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

/** 요청·응답 JSON 경계 규칙을 한 곳에서 고정합니다. */
public final class ApiJsonMapper {

    private ApiJsonMapper() {
    }

    /**
     * 운영 MessageConverter와 같은 규칙을 적용한 ObjectMapper를 만듭니다.
     *
     * <p>Spring MVC가 등록하는 Converter와 같은 기본값에서 시작해야 하므로
     * {@link Jackson2ObjectMapperBuilder}로 만든 뒤 경계 규칙을 덧붙입니다.</p>
     */
    public static ObjectMapper create() {
        ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json().build();
        configure(objectMapper);
        return objectMapper;
    }

    public static void configure(ObjectMapper objectMapper) {
        // Instant가 epoch 숫자가 아닌 사람이 읽을 수 있는 UTC ISO-8601 문자열로 노출되도록 고정합니다.
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // 금액은 KRW 원 단위 정수만 허용합니다. Jackson 기본 coercion은 100000.9를 100000으로
        // 절단하므로, 요청과 다른 금액을 묵시 처리하지 않도록 소수 입력을 거부합니다.
        objectMapper.coercionConfigFor(LogicalType.Integer)
                .setCoercion(CoercionInputShape.Float, CoercionAction.Fail);
    }
}
