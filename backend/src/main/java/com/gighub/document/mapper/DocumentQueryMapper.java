package com.gighub.document.mapper;

import com.gighub.document.dto.Document;
import com.gighub.document.dto.DocumentListItem;
import com.gighub.document.dto.DocumentShare;
import com.gighub.document.dto.DocumentVersion;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DocumentQueryMapper {
    Document findDocumentById(@Param("documentId") Long documentId);

    List<DocumentVersion> findVersionsByDocumentId(@Param("documentId") Long documentId);

    List<DocumentListItem> findDocuments(
            @Param("userId") Long userId,
            @Param("documentType") String documentType,
            @Param("offset") long offset,
            @Param("size") int size);

    int countDocuments(
            @Param("userId") Long userId,
            @Param("documentType") String documentType);

    List<DocumentShare> findSharesByDocumentId(
            @Param("documentId") Long documentId);
}
