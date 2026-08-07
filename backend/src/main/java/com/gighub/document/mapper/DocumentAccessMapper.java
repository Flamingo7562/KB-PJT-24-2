package com.gighub.document.mapper;

import com.gighub.document.dto.DocumentFileVersion;
import com.gighub.document.mapper.param.DocumentAccessLogParam;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** 당사자 파일 접근 권한 검사와 접근 감사 기록을 담당합니다(DOC-011). */
@Mapper
public interface DocumentAccessMapper {

    /** 일반 사용자에게 반환할 SIGNED Version 2를 조회한다(없으면 null). */
    DocumentFileVersion findSignedVersionForAccess(@Param("documentId") Long documentId);

    /** 문서가 참조하는 불변 계약 Snapshot의 OWNER 또는 WORKER인지 확인한다. */
    boolean isContractParty(
            @Param("documentId") Long documentId,
            @Param("userId") Long userId);

    int insertAccessLog(DocumentAccessLogParam param);
}
