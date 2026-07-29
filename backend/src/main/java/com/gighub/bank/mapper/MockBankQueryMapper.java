package com.gighub.bank.mapper;

import com.gighub.bank.dto.BankAccountSummary;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

// Mock 계좌 조회 전용 Mapper, 이체 경로와 분리해 FOR UPDATE 없이 읽기만 수행
@Mapper
public interface MockBankQueryMapper {
    // 마스킹은 SQL이 아닌 서비스에서 수행하도록 원문 계좌번호를 함께 조회
    List<BankAccountSummary> findAccountsByUserId(@Param("userId") Long userId,
                                                  @Param("status") String status);
}
