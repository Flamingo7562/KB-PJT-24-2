package com.gighub.document.controller;

import com.gighub.auth.security.AuthPrincipals;
import com.gighub.common.exception.ValidationException;
import com.gighub.document.service.DocumentFileAccessService;
import com.gighub.document.service.DocumentFileResult;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.Set;

/**
 * 계약 당사자의 문서 파일 view·download 경계입니다(DOC-011).
 *
 * <p>M7 #132의 일반 문서 목록·Metadata API와 분리된, M4 계약 전용 파일 접근 Endpoint다.
 * Storage Key와 실제 저장 경로는 응답에 포함하지 않는다.</p>
 */
@RestController
@RequiredArgsConstructor
public class DocumentFileController {

    private static final Set<String> ALLOWED_MODES = Set.of("view", "download");

    private final DocumentFileAccessService documentFileAccessService;

    @GetMapping("/api/documents/{documentId}/file")
    public ResponseEntity<byte[]> getDocumentFile(
            @PathVariable Long documentId,
            @RequestParam(defaultValue = "view") String mode,
            Authentication authentication) {
        if (!ALLOWED_MODES.contains(mode)) {
            throw new ValidationException("mode는 view 또는 download여야 합니다.");
        }
        Long loginUserId = AuthPrincipals.resolve(authentication).getUserId();

        DocumentFileResult result =
                documentFileAccessService.loadFile(documentId, loginUserId, mode);

        ContentDisposition disposition = ("download".equals(mode)
                ? ContentDisposition.attachment() : ContentDisposition.inline())
                .filename(result.getFileName(), StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(MediaType.parseMediaType(result.getMimeType()))
                .body(result.getContent());
    }

}
