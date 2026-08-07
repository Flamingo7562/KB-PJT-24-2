package com.gighub.document.controller;

import com.gighub.auth.security.AuthPrincipal;
import com.gighub.common.exception.CommonExceptionHandler;
import com.gighub.document.exception.DocumentAccessDeniedException;
import com.gighub.document.exception.DocumentNotFoundException;
import com.gighub.document.service.DocumentFileAccessService;
import com.gighub.document.service.DocumentFileResult;
import com.gighub.member.domain.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** {@link DocumentFileController}의 인증, 접근 오류 매핑과 응답 Header를 검증합니다. */
@ExtendWith(MockitoExtension.class)
class DocumentFileControllerTest {

    private static final Long USER_ID = 3L;
    private static final Long DOCUMENT_ID = 10L;

    @Mock
    private DocumentFileAccessService documentFileAccessService;

    private MockMvc mockMvc;
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new DocumentFileController(documentFileAccessService))
                .setControllerAdvice(new CommonExceptionHandler())
                .build();

        AuthPrincipal principal = new AuthPrincipal(USER_ID, UserRole.OWNER, "김사장");
        authentication = new UsernamePasswordAuthenticationToken(principal, null, List.of());
    }

    private DocumentFileResult result() {
        return DocumentFileResult.builder()
                .content("PDF-BYTES".getBytes(StandardCharsets.UTF_8))
                .mimeType("application/pdf")
                .fileName("근로계약서_v2.pdf")
                .build();
    }

    @Test
    void returnsInlineDispositionForViewMode() throws Exception {
        when(documentFileAccessService.loadFile(DOCUMENT_ID, USER_ID, "view"))
                .thenReturn(result());

        mockMvc.perform(get("/api/documents/{documentId}/file", DOCUMENT_ID)
                        .principal(authentication))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition",
                        org.hamcrest.Matchers.startsWith("inline;")))
                .andExpect(content().contentType("application/pdf"))
                .andExpect(content().bytes("PDF-BYTES".getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void returnsAttachmentDispositionForDownloadMode() throws Exception {
        when(documentFileAccessService.loadFile(DOCUMENT_ID, USER_ID, "download"))
                .thenReturn(result());

        mockMvc.perform(get("/api/documents/{documentId}/file", DOCUMENT_ID)
                        .param("mode", "download")
                        .principal(authentication))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition",
                        org.hamcrest.Matchers.startsWith("attachment;")));
    }

    @Test
    void rejectsUnknownModeBeforeCallingTheService() throws Exception {
        mockMvc.perform(get("/api/documents/{documentId}/file", DOCUMENT_ID)
                        .param("mode", "edit")
                        .principal(authentication))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        verify(documentFileAccessService, never()).loadFile(any(), any(), any());
    }

    @Test
    void translatesAccessDeniedToForbidden() throws Exception {
        when(documentFileAccessService.loadFile(eq(DOCUMENT_ID), eq(USER_ID), any()))
                .thenThrow(new DocumentAccessDeniedException("문서에 접근할 권한이 없습니다."));

        mockMvc.perform(get("/api/documents/{documentId}/file", DOCUMENT_ID)
                        .principal(authentication))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void translatesMissingDocumentToNotFound() throws Exception {
        when(documentFileAccessService.loadFile(eq(DOCUMENT_ID), eq(USER_ID), any()))
                .thenThrow(new DocumentNotFoundException("문서를 찾을 수 없습니다."));

        mockMvc.perform(get("/api/documents/{documentId}/file", DOCUMENT_ID)
                        .principal(authentication))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    void rejectsRequestWithoutSession() throws Exception {
        mockMvc.perform(get("/api/documents/{documentId}/file", DOCUMENT_ID))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_REQUIRED"));

        verify(documentFileAccessService, never()).loadFile(any(), any(), any());
    }

    @Test
    void doesNotExposeStorageKeyInTheResponse() throws Exception {
        when(documentFileAccessService.loadFile(DOCUMENT_ID, USER_ID, "view"))
                .thenReturn(result());

        String responseHeader = mockMvc.perform(get("/api/documents/{documentId}/file", DOCUMENT_ID)
                        .principal(authentication))
                .andReturn().getResponse().getHeader("Content-Disposition");

        assertEquals(true, responseHeader != null && !responseHeader.contains("contracts/"));
    }
}
