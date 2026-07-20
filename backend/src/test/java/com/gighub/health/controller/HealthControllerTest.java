package com.gighub.health.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@link HealthController}의 HTTP 경로와 JSON 응답 계약을 확인합니다.
 */
class HealthControllerTest {

    private MockMvc mockMvc;

    /**
     * Java Time 직렬화를 포함한 독립형 Spring MVC 테스트 환경을 준비합니다.
     */
    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        MappingJackson2HttpMessageConverter converter =
                new MappingJackson2HttpMessageConverter(objectMapper);

        mockMvc = MockMvcBuilders
                .standaloneSetup(new HealthController())
                .setMessageConverters(converter)
                .build();
    }

    /**
     * Health Endpoint가 HTTP 200과 최소 상태 정보를 반환하는지 검증합니다.
     *
     * @throws Exception MockMvc 요청 실행에 실패한 경우
     */
    @Test
    void returnsUpStatus() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.service").value("gig-hub-backend"))
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.checkedAt").exists());
    }
}

