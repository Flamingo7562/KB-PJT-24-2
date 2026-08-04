package com.gighub.common.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.gighub.common.exception.CommonExceptionHandler;
import com.gighub.common.trace.TraceIdFilter;
import lombok.Builder;
import lombok.Value;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.ResourceHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 보호 명세의 성공·목록 Envelope 계약을 고정합니다.
 *
 * <p>Controller가 응답을 {@code Map}으로 조립하면 필드 이름과 구조가 조용히 달라질 수
 * 있으므로, 실제 직렬화 결과가 승인 계약과 같은지 확인합니다.</p>
 */
class ApiResponseContractTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        // 운영 설정(WebMvcConfig.extendMessageConverters)과 같은 직렬화 규칙을 적용한다.
        // standaloneSetup은 그 설정을 상속하지 않으므로 Instant 규칙을 여기서 맞춘다.
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        mockMvc = MockMvcBuilders
                .standaloneSetup(new EnvelopeController())
                .setControllerAdvice(new CommonExceptionHandler())
                .setMessageConverters(
                        new ResourceHttpMessageConverter(),
                        new MappingJackson2HttpMessageConverter(objectMapper))
                .addFilters(new TraceIdFilter())
                .build();
    }

    @Test
    void singleSuccessWrapsPayloadInData() throws Exception {
        mockMvc.perform(get("/test/single"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.name").value("샘플"));
    }

    @Test
    void listSuccessUsesContentAndPage() throws Exception {
        mockMvc.perform(get("/test/page").param("page", "1").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(2))
                .andExpect(jsonPath("$.data.content[0].id").value(1))
                .andExpect(jsonPath("$.data.page.number").value(1))
                .andExpect(jsonPath("$.data.page.size").value(2))
                .andExpect(jsonPath("$.data.page.totalElements").value(5))
                .andExpect(jsonPath("$.data.page.totalPages").value(3));
    }

    @Test
    void pageMetadataHasOnlyApprovedFields() throws Exception {
        MvcResult result = mockMvc.perform(get("/test/page"))
                .andExpect(status().isOk())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        // 명세에 없는 파생 필드를 구현 편의로 추가하지 않는다.
        assertTrue(body.contains("\"number\""));
        assertTrue(body.contains("\"size\""));
        assertTrue(body.contains("\"totalElements\""));
        assertTrue(body.contains("\"totalPages\""));
        assertEquals(-1, body.indexOf("\"first\""));
        assertEquals(-1, body.indexOf("\"last\""));
        assertEquals(-1, body.indexOf("\"hasNext\""));
        assertEquals(-1, body.indexOf("\"numberOfElements\""));
    }

    @Test
    void emptyPageKeepsStructureAndReportsZeroPages() throws Exception {
        mockMvc.perform(get("/test/page").param("total", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content.length()").value(0))
                .andExpect(jsonPath("$.data.page.totalElements").value(0))
                .andExpect(jsonPath("$.data.page.totalPages").value(0));
    }

    @Test
    void pageBoundsViolationsUseApprovedErrorEnvelope() throws Exception {
        mockMvc.perform(get("/test/page").param("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.traceId").isString());

        mockMvc.perform(get("/test/page").param("size", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        mockMvc.perform(get("/test/page").param("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void approvedPageBoundsAreAccepted() throws Exception {
        mockMvc.perform(get("/test/page").param("page", "0").param("size", "1"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/test/page").param("size", "100"))
                .andExpect(status().isOk());
    }

    @Test
    void defaultPageQueryFollowsApprovedValues() throws Exception {
        mockMvc.perform(get("/test/page"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.page.number").value(PageRequests.DEFAULT_PAGE))
                .andExpect(jsonPath("$.data.page.size").value(PageRequests.DEFAULT_SIZE));
    }

    @Test
    void noContentResponseHasNoBody() throws Exception {
        MvcResult result = mockMvc.perform(get("/test/no-content"))
                .andExpect(status().isNoContent())
                .andReturn();

        assertEquals("", result.getResponse().getContentAsString());
    }

    @Test
    void instantIsSerializedAsIsoStringNotEpochNumber() throws Exception {
        mockMvc.perform(get("/test/single"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.createdAt").value("2026-07-31T09:00:00Z"));
    }

    @Test
    void fileStreamIsNotWrappedInJsonEnvelope() throws Exception {
        MvcResult result = mockMvc.perform(get("/test/file"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", MediaType.APPLICATION_OCTET_STREAM_VALUE))
                .andReturn();

        assertEquals("raw-bytes", result.getResponse().getContentAsString());
    }

    @Value
    @Builder
    static class SamplePayload {
        Long id;
        String name;
        Instant createdAt;
    }

    @RestController
    static class EnvelopeController {

        @GetMapping("/test/single")
        ApiResponse<SamplePayload> single() {
            return ApiResponse.of(SamplePayload.builder()
                    .id(1L)
                    .name("샘플")
                    .createdAt(Instant.parse("2026-07-31T09:00:00Z"))
                    .build());
        }

        @GetMapping("/test/page")
        ApiResponse<PageResponse<SamplePayload>> page(
                @RequestParam(defaultValue = PageRequests.DEFAULT_PAGE_TEXT) int page,
                @RequestParam(defaultValue = PageRequests.DEFAULT_SIZE_TEXT) int size,
                @RequestParam(defaultValue = "5") long total) {
            PageRequests.validate(page, size);

            List<SamplePayload> content = total == 0
                    ? List.of()
                    : List.of(
                            SamplePayload.builder().id(1L).name("첫째").build(),
                            SamplePayload.builder().id(2L).name("둘째").build());

            return ApiResponse.of(PageResponse.of(content, page, size, total));
        }

        @GetMapping("/test/no-content")
        ResponseEntity<Void> noContent() {
            return ResponseEntity.noContent().build();
        }

        @GetMapping(value = "/test/file", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
        ResponseEntity<Resource> file() {
            return ResponseEntity.ok(
                    new ByteArrayResource("raw-bytes".getBytes(StandardCharsets.UTF_8)));
        }
    }
}
