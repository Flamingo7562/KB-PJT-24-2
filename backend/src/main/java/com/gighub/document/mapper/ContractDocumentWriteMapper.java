package com.gighub.document.mapper;

import com.gighub.document.mapper.param.DocumentInsertParam;
import com.gighub.document.mapper.param.DocumentShareInsertParam;
import com.gighub.document.mapper.param.DocumentSignatureInsertParam;
import com.gighub.document.mapper.param.DocumentVersionInsertParam;
import com.gighub.document.mapper.result.ContractVersionPromotionRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 근로계약서 자동 생성이 쓰는 {@code documents} 계열 행 생성·조회 진입점입니다.
 *
 * <p>{@link com.gighub.contract.ContractArtifactPort} 구현 전용입니다. 조회용 API가 쓰는
 * {@link DocumentQueryMapper}, {@link DocumentAccessMapper}와는 관심사가 달라 나눕니다.</p>
 */
@Mapper
public interface ContractDocumentWriteMapper {

    /** {@code documents} 한 행을 저장하고 생성된 식별자를 {@code param.id}에 채운다. */
    int insertDocument(DocumentInsertParam param);

    /** {@code document_versions} 한 행을 저장하고 생성된 식별자를 {@code param.id}에 채운다. */
    int insertVersion(DocumentVersionInsertParam param);

    int insertSignature(DocumentSignatureInsertParam param);

    int insertShare(DocumentShareInsertParam param);

    /** 문서 상태를 예상 상태에서만 전이한다. */
    int updateDocumentStatus(
            @Param("documentId") long documentId,
            @Param("expectedStatus") String expectedStatus,
            @Param("status") String status);

    /**
     * 특정 근무의 EMPLOYMENT_CONTRACT 문서에 딸린 Version들의 승격 정보를 읽는다.
     *
     * <p>Commit 뒤 승격은 {@link com.gighub.contract.ContractArtifactHandle}이 저장 Key를
     * 들고 있지 않으므로 근무 식별자로 다시 조회한다.</p>
     */
    List<ContractVersionPromotionRow> findPromotionRowsByWorkCaseId(
            @Param("workCaseId") long workCaseId);
}
