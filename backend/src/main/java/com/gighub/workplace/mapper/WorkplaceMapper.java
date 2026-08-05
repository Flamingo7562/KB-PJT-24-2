package com.gighub.workplace.mapper;

import com.gighub.workplace.mapper.param.WorkplaceInsertParam;
import org.apache.ibatis.annotations.Mapper;

/** 사업장 등록과 소유 사업장 조회 SQL 진입점입니다. */
@Mapper
public interface WorkplaceMapper {

    /**
     * 사업장 한 건을 저장하고 생성된 식별자를 {@code param.id}에 채웁니다.
     *
     * <p>사업자등록번호 중복은 여기서 미리 조회하지 않고 Unique 제약이 던지는 예외를
     * Service가 승인된 충돌 응답으로 바꿉니다. 조회 후 저장 사이에 다른 요청이 끼어들 수
     * 있어 사전 조회로는 동시 등록을 막지 못합니다.</p>
     */
    int insert(WorkplaceInsertParam param);
}
