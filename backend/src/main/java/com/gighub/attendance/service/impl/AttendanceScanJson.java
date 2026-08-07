package com.gighub.attendance.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gighub.attendance.dto.AttendanceConfirmationRequiredResponse;
import com.gighub.attendance.dto.AttendanceRecordedResponse;
import com.gighub.attendance.dto.AttendanceScanResponse;
import com.gighub.common.api.ApiResponse;
import com.gighub.config.ApiJsonMapper;
import org.springframework.stereotype.Component;

import java.time.Instant;

/** 멱등 Claim에 저장할 스캔 응답을 운영 API와 같은 JSON 규칙으로 읽고 씁니다. */
@Component
public class AttendanceScanJson {

    private final ObjectMapper objectMapper = ApiJsonMapper.create();

    public String writeResponseBody(AttendanceScanResponse response) {
        try {
            return objectMapper.writeValueAsString(ApiResponse.of(response));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("출퇴근 응답을 직렬화하지 못했습니다.", exception);
        }
    }

    public AttendanceScanResponse readResponseBody(String storedBody) {
        try {
            JsonNode data = objectMapper.readTree(storedBody).get("data");
            if (data == null) {
                throw new IllegalStateException("저장된 출퇴근 응답에 data가 없습니다.");
            }
            if ("CONFIRMATION_REQUIRED".equals(data.get("result").asText())) {
                return new AttendanceConfirmationRequiredResponse(
                        data.get("workCaseId").asLong(),
                        Instant.parse(data.get("scheduledEndAt").asText()));
            }
            return new AttendanceRecordedResponse(
                    data.get("workCaseId").asLong(),
                    data.get("scanType").asText(),
                    Instant.parse(data.get("recordedAt").asText()),
                    data.get("isLate").asBoolean(),
                    data.get("lateMinutes").asInt(),
                    instantOrNull(data.get("earlyCheckoutConfirmedAt")),
                    instantOrNull(data.get("settlementDueAt")));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("저장된 출퇴근 응답을 읽지 못했습니다.", exception);
        }
    }

    private static Instant instantOrNull(JsonNode node) {
        return node == null || node.isNull() ? null : Instant.parse(node.asText());
    }
}
