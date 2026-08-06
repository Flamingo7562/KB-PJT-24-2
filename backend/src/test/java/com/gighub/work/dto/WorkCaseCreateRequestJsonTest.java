package com.gighub.work.dto;

import java.time.LocalDate;
import java.time.LocalTime;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 실제 JSON 문자열로 역직렬화 배선을 확인합니다.
 *
 * <p>생성자를 직접 호출하는 검증 테스트는 {@code @JsonCreator}·{@code @JsonProperty} 배선이
 * 잘못돼도 통과합니다. 배선이 틀리면 모든 필드가 조용히 {@code null}이 되고 예외도 나지
 * 않으므로, JSON을 거치는 확인이 따로 필요합니다.</p>
 *
 * <p>{@link Jackson2ObjectMapperBuilder}는 Spring MVC가 기본 MessageConverter에 쓰는 설정과
 * 같습니다. 테스트만 다른 ObjectMapper를 쓰면 실제 요청에서만 실패하는 차이가 생깁니다.</p>
 */
class WorkCaseCreateRequestJsonTest {

    private final ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json().build();

    @Test
    void bindsApprovedFieldsFromJson() throws Exception {
        String json = "{\"title\":\"주말 홀 서빙\",\"workDate\":\"2026-08-10\","
                + "\"startTime\":\"09:00\",\"endTime\":\"18:00\","
                + "\"breakMinutes\":60,\"breakPaid\":false,\"dailyWage\":120000}";

        WorkCaseCreateRequest request = objectMapper.readValue(json, WorkCaseCreateRequest.class);

        assertEquals("주말 홀 서빙", request.getTitle());
        assertEquals(LocalDate.of(2026, 8, 10), request.getWorkDate());
        assertEquals(LocalTime.of(9, 0), request.getStartTime());
        assertEquals(LocalTime.of(18, 0), request.getEndTime());
        assertEquals(60, request.getBreakMinutes());
        assertEquals(Boolean.FALSE, request.getBreakPaid());
        assertEquals(120_000L, request.getDailyWage());
    }

    @Test
    void trimsTitleDuringDeserialization() throws Exception {
        String json = "{\"title\":\"  주말 홀 서빙  \",\"workDate\":\"2026-08-10\","
                + "\"startTime\":\"09:00\",\"endTime\":\"18:00\","
                + "\"breakMinutes\":60,\"breakPaid\":false,\"dailyWage\":120000}";

        WorkCaseCreateRequest request = objectMapper.readValue(json, WorkCaseCreateRequest.class);

        assertEquals("주말 홀 서빙", request.getTitle());
    }

    @Test
    void rejectsServerOwnedFieldFromJson() {
        String json = "{\"title\":\"주말 홀 서빙\",\"workDate\":\"2026-08-10\","
                + "\"startTime\":\"09:00\",\"endTime\":\"18:00\","
                + "\"breakMinutes\":60,\"breakPaid\":false,\"dailyWage\":120000,"
                + "\"status\":\"ACCEPTED\"}";

        Exception thrown = assertThrows(
                Exception.class,
                () -> objectMapper.readValue(json, WorkCaseCreateRequest.class)
        );

        assertTrue(
                messageChain(thrown).contains("허용되지 않은 근무 조건 필드입니다: status"),
                messageChain(thrown)
        );
    }

    /** Jackson이 우리 예외를 감싸므로 원인 사슬 전체에서 메시지를 찾습니다. */
    private static String messageChain(Throwable thrown) {
        StringBuilder chain = new StringBuilder();
        for (Throwable current = thrown; current != null; current = current.getCause()) {
            chain.append(current.getMessage()).append('\n');
        }
        return chain.toString();
    }
}
