package com.gighub.document.controller;

import com.gighub.common.api.ApiResponse;
import com.gighub.common.api.PageRequests;
import com.gighub.common.api.PageResponse;
import com.gighub.common.exception.AuthRequiredException;
import com.gighub.document.dto.Document;
import com.gighub.document.dto.DocumentDetailResponse;
import com.gighub.document.dto.DocumentListItem;
import com.gighub.document.dto.DocumentShare;
import com.gighub.document.dto.DocumentShareListResponse;
import com.gighub.document.dto.DocumentVersion;
import com.gighub.document.exception.DocumentNotFoundException;
import com.gighub.document.mapper.DocumentQueryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpSession;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class DocumentController {
    private static final String LOGIN_USER = "LOGIN_USER";

    private final DocumentQueryMapper documentQueryMapper;

    // DOC-001: 문서 목록
    @GetMapping("/api/documents")
    public ResponseEntity<ApiResponse<PageResponse<DocumentListItem>>> getDocuments(
            @RequestParam(required = false) String documentType,
            @RequestParam(required = false) String source,
            @RequestParam(defaultValue = PageRequests.DEFAULT_PAGE_TEXT) int page,
            @RequestParam(defaultValue = PageRequests.DEFAULT_SIZE_TEXT) int size,
            HttpSession session){
        Long loginUserId = requireLogin(session);
        PageRequests.validate(page, size);

        List<DocumentListItem> content = documentQueryMapper.findDocuments(
                loginUserId, documentType, PageRequests.offset(page, size), size);
        int total = documentQueryMapper.countDocuments(loginUserId, documentType);

        return ResponseEntity.ok(
                ApiResponse.of(PageResponse.of(content, page, size, total)));
    }

    // DOC-003: 문서 메타데이터 + 버전 목록
    @GetMapping("/api/documents/{documentId}")
    public ResponseEntity<ApiResponse<DocumentDetailResponse>> getDocument(
            @PathVariable Long documentId,
            HttpSession session) {
        requireLogin(session);

        Document document = documentQueryMapper.findDocumentById(documentId);
        if (document == null) {
            throw new DocumentNotFoundException("문서를 찾을 수 없습니다.");
        }

        // TODO: 접근 권한 검증(소유자/계약당사자/유효공유), document_access_logs 기록
        List<DocumentVersion> versions =
                documentQueryMapper.findVersionsByDocumentId(documentId);

        return ResponseEntity.ok(
                ApiResponse.of(DocumentDetailResponse.of(document, versions)));
    }

    // SHARE-002: 문서 공유 현황
    @GetMapping("/api/documents/{documentId}/shares")
    public ResponseEntity<ApiResponse<DocumentShareListResponse>> getDocumentShares(
            @PathVariable Long documentId,
            HttpSession session){
        requireLogin(session);

        // TODO: 문서 소유자 검증 (DOCUMENT_ACCESS_DENIED)
        List<DocumentShare> shares =
                documentQueryMapper.findSharesByDocumentId(documentId);

        return ResponseEntity.ok(ApiResponse.of(DocumentShareListResponse.of(shares)));
    }

    private Long requireLogin(HttpSession session){
        Long loginUserId = (Long) session.getAttribute(LOGIN_USER);
        if(loginUserId == null){
            throw new AuthRequiredException("로그인이 필요합니다.");
        }
        return loginUserId;
    }
}
