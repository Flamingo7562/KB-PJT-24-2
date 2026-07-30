package com.gighub.document.controller;

import com.gighub.document.dto.Document;
import com.gighub.document.dto.DocumentListItem;
import com.gighub.document.dto.DocumentShare;
import com.gighub.document.dto.DocumentVersion;
import com.gighub.document.mapper.DocumentQueryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpSession;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class DocumentController {
    private static final String LOGIN_USER = "LOGIN_USER";

    private final DocumentQueryMapper documentQueryMapper;

    // DOC-001: 문서 목록
    @GetMapping("/api/documents")
    public ResponseEntity<Map<String, Object>> getDocuments(
            @RequestParam(required = false) String documentType,
            @RequestParam(required = false) String source,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpSession session){
        Long loginUserId = requireLogin(session);

        long offset = (long) page * size;
        List<DocumentListItem> content = documentQueryMapper.findDocuments(
                loginUserId, documentType, offset, size);
        int total = documentQueryMapper.countDocuments(loginUserId, documentType);

        Map<String, Object> pageInfo = new LinkedHashMap<>();
        pageInfo.put("number", page);
        pageInfo.put("size", size);
        pageInfo.put("totalElements", total);
        pageInfo.put("totalPages", size == 0 ? 0 : (int) Math.ceil((double) total / size));

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("content", content);
        data.put("page", pageInfo);
        return ResponseEntity.ok(Map.of("data", data));
    }

    // DOC-003: 문서 메타데이터 + 버전 목록
    @GetMapping("/api/documents/{documentId}")
    public ResponseEntity<Map<String, Object>> getDocument(
            @PathVariable Long documentId,
            HttpSession session){
        requireLogin(session);

        // TODO: 접근 권한 검증(소유자/계약당사자/유효공유), document_access_logs 기록
        Document document = documentQueryMapper.findDocumentById(documentId);
        List<DocumentVersion> versions =
                documentQueryMapper.findVersionsByDocumentId(documentId);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("document", document);
        data.put("versions", versions);
        return ResponseEntity.ok(Map.of("data", data));
    }

    // SHARE-002: 문서 공유 현황
    @GetMapping("/api/documents/{documentId}/shares")
    public ResponseEntity<Map<String, Object>> getDocumentShares(
            @PathVariable Long documentId,
            HttpSession session){
        requireLogin(session);

        // TODO: 문서 소유자 검증 (DOCUMENT_ACCESS_DENIED)
        List<DocumentShare> shares =
                documentQueryMapper.findSharesByDocumentId(documentId);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("items", shares);
        return ResponseEntity.ok(Map.of("data", data));
    }

    private Long requireLogin(HttpSession session){
        Long loginUserId = (Long) session.getAttribute(LOGIN_USER);
//        if(loginUserId == null){
//            throw new AuthRequiredException("로그인이 필요합니다.");
//        }
        return loginUserId;
    }
}
